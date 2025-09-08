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
    // Access 블랙리스트:      blk:{jti} -> "1" (TTL = access 남은시간)

    private String rtKey(String hash){ return "rt:" + hash; }
    private String rtuKey(Long userId){ return "rtu:" + userId; }
    private String blkKey(String jti){ return "blk:" + jti; }

    private long refreshTtlMillis() {
        Long v = props.refreshValidityMillis();
        return (v != null && v > 0) ? v : 14L * 24 * 3600 * 1000; // 기본 14일
    }

    /** 새 리프레시 토큰 발급(클라이언트에 원문 전달, 서버에는 해시만 저장) */
    public String issueRefresh(Long userId, String role, String email) {
        String raw = "rt." + UUID.randomUUID() + "." + System.nanoTime();
        String hash = sha256(raw);
        String val = userId + "|" + role + "|" + (email == null ? "" : email);

        redis.opsForValue().set(rtKey(hash), val, Duration.ofMillis(refreshTtlMillis()));
        redis.opsForSet().add(rtuKey(userId), hash);
        return raw;
    }

    /** 리프레시 검증 → 소유자 정보 반환 */
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

    /** 리프레시 회전(rotate): 기존 것 폐기 → 새 토큰 발급 */
    public String rotateRefresh(UserTokenOwner owner) {
        redis.delete(rtKey(owner.hash()));
        redis.opsForSet().remove(rtuKey(owner.userId()), owner.hash());
        return issueRefresh(owner.userId(), owner.role(), owner.email());
    }

    /** 로그아웃: 해당 리프레시만 또는 사용자 전체 리프레시 폐기 */
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

    /** Access 블랙리스트 등록(JTI 기준) */
    public void blacklistAccess(String jti, long ttlMillis) {
        if (jti == null) return;
        redis.opsForValue().set(blkKey(jti), "1", Duration.ofMillis(Math.max(1000, ttlMillis)));
    }

    /** 블랙리스트 확인 */
    public boolean isBlacklisted(String jti) {
        return jti != null && Boolean.TRUE.equals(redis.hasKey(blkKey(jti)));
    }

    // === util ===
    public record UserTokenOwner(Long userId, String role, String email, String hash) {}
    private static String sha256(String s){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
        }catch(Exception e){ throw new IllegalStateException(e); }
    }
}