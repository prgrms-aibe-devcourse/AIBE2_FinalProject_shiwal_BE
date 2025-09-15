package com.example.hyu.service;

import com.example.hyu.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenStoreService {

    private final StringRedisTemplate redis;
    private final JwtProperties props;

    // ==== Redis Keys ====
    // Refresh 토큰(해시 저장):  rt:{hash} -> value: userId|role|email   (TTL = refresh-validity)
    // 사용자별 Refresh 세트:   rtu:{userId} (members: {hash})
    /**
 * Build the Redis key used to store a refresh token entry for the given token hash.
 *
 * @param hash SHA-256, URL-safe Base64 (no padding) hash of the raw refresh token
 * @return Redis key in the format "rt:{hash}"
 */

    private String rtKey(String hash){ return "rt:" + hash; }
    /**
 * Builds the Redis key for a user's refresh-token set.
 *
 * The key has the form `rtu:{userId}` where `{userId}` is the user's numeric id.
 *
 * @param userId the user id to embed in the key
 * @return the Redis key for the user's refresh-token set
 */
private String rtuKey(Long userId){ return "rtu:" + userId; }
    /**
 * Builds the Redis key used to store an access-token blacklist entry for the given JWT ID.
 *
 * @param jti the JWT ID to include in the key
 * @return the Redis key in the form {@code "blk:{jti}"}
 */
private String blkKey(String jti){ return "blk:" + jti; }

    /**
     * Returns the refresh-token TTL in milliseconds.
     *
     * If JwtProperties.refreshValidityMillis() provides a positive value, that value is returned;
     * otherwise a default of 14 days (14 * 24 * 3600 * 1000 ms) is used.
     *
     * @return refresh token time-to-live in milliseconds
     */
    private long refreshTtlMillis() {
        Long v = props.refreshValidityMillis();
        return (v != null && v > 0) ? v : 14L * 24 * 3600 * 1000; // 기본 14일
    }

    /**
     * Creates and stores a new refresh token (only a SHA-256 hash is stored in Redis) and returns the raw token to give to the client.
     *
     * The stored Redis value is "userId|role|email" (email becomes an empty string if null). The raw token is of the form
     * "rt.{uuid}.{nanotime}" and its SHA-256 (URL-safe Base64, no padding) is used as the Redis key; the entry is set with the service's refresh TTL.
     *
     * @param userId the owner user's id
     * @param role the user's role to record with the refresh token
     * @param email the user's email (may be null; stored as an empty string if null)
     * @return the raw refresh token string to be returned to the client
     */
    public String issueRefresh(Long userId, String role, String email) {
        String raw = "rt." + UUID.randomUUID() + "." + System.nanoTime();
        String hash = sha256(raw);
        String val = userId + "|" + role + "|" + (email == null ? "" : email);

        redis.opsForValue().set(rtKey(hash), val, Duration.ofMillis(refreshTtlMillis()));
        redis.opsForSet().add(rtuKey(userId), hash);
        return raw;
    }

    /**
     * Validates a raw refresh token and returns its owner information if present.
     *
     * The method computes the storage hash of the provided raw token, looks up the
     * corresponding Redis entry (key `rt:{hash}`) and, if found, parses the stored
     * value `userId|role|email` into a UserTokenOwner (email may be null).
     *
     * @param raw the raw refresh token string previously issued to a client
     * @return an Optional containing the token owner (userId, role, email, hash) if the token exists; otherwise Optional.empty()
     */
    public Optional<UserTokenOwner> checkRefresh(String raw) {
        String hash = sha256(raw);
        String v = redis.opsForValue().get(rtKey(hash));
        if (v == null) return Optional.empty();
        String[] parts = v.split("\\|", -1);
        Long uid = Long.valueOf(parts[0]);
        String role = parts.length > 1 ? parts[1] : "ROLE_USER";
        String email = parts.length > 2 ? parts[2] : null;
        return Optional.of(new UserTokenOwner(uid, role, email, hash));
    }

    /**
     * Rotate a refresh token: revoke the provided token and issue a new one for the same owner.
     *
     * Deletes the stored refresh entry identified by the owner's hash and removes that hash from
     * the owner's Redis set, then creates and returns a new raw refresh token for the same user,
     * role, and email.
     *
     * @param owner the current token owner containing userId, role, email and the hash of the token to revoke
     * @return a newly issued raw refresh token string to be returned to the client
     */
    public String rotateRefresh(UserTokenOwner owner) {
        redis.delete(rtKey(owner.hash()));
        redis.opsForSet().remove(rtuKey(owner.userId()), owner.hash());
        return issueRefresh(owner.userId(), owner.role(), owner.email());
    }

    /**
     * Revoke refresh token(s) for a user.
     *
     * <p>When {@code revokeAllForUser} is true, deletes all refresh-token entries associated
     * with the owner's userId and removes the user's refresh-token set. When false, deletes
     * only the specific refresh-token entry identified by the owner's hash and removes that
     * hash from the user's set.
     *
     * @param owner the token owner containing userId and the specific refresh-token hash
     * @param revokeAllForUser if true, revoke every refresh token for the user; if false, revoke only the specific token
     */
    public void revokeRefresh(UserTokenOwner owner, boolean revokeAllForUser) {
        if (revokeAllForUser) {
            var members = redis.opsForSet().members(rtuKey(owner.userId()));
            if (members != null) for (String h : members) redis.delete(rtKey(h));
            redis.delete(rtuKey(owner.userId()));
        } else {
            redis.delete(rtKey(owner.hash()));
            redis.opsForSet().remove(rtuKey(owner.userId()), owner.hash());
        }
    }

    /**
     * Adds an access-token identifier (JTI) to the Redis blacklist so the token can be treated as revoked.
     *
     * If `jti` is null this method does nothing. The blacklist entry is stored under the `blk:{jti}` key
     * with a TTL equal to the provided `ttlMillis` but never less than 1000 milliseconds.
     *
     * @param jti      the token's JTI to blacklist; when null the operation is a no-op
     * @param ttlMillis desired time-to-live for the blacklist entry in milliseconds (minimum 1000 ms)
     */
    public void blacklistAccess(String jti, long ttlMillis) {
        if (jti == null) return;
        redis.opsForValue().set(blkKey(jti), "1", Duration.ofMillis(Math.max(1000, ttlMillis)));
    }

    /**
     * Returns whether an access token with the given JWT ID is blacklisted.
     *
     * Checks Redis for existence of the blacklist key `blk:{jti}`. A null `jti` always returns false.
     *
     * @param jti the JWT ID (jti) of the access token to check
     * @return true if the token is blacklisted, false otherwise
     */
    public boolean isBlacklisted(String jti) {
        return jti != null && Boolean.TRUE.equals(redis.hasKey(blkKey(jti)));
    }

    // === util ===
    public record UserTokenOwner(Long userId, String role, String email, String hash) {}
    /**
     * Computes the SHA-256 digest of the input string and returns it as a
     * URL-safe Base64 string with padding removed.
     *
     * @param s the input string to hash (encoded as UTF-8)
     * @return a URL-safe, no-padding Base64 representation of the SHA-256 hash
     * @throws IllegalStateException if the SHA-256 MessageDigest cannot be created or another error occurs
     */
    private static String sha256(String s){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
        }catch(Exception e){ throw new IllegalStateException(e); }
    }
}