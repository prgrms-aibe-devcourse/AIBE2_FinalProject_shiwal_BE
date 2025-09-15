package com.example.hyu.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

public class JwtTokenProvider {

    private final SecretKey key;             // HS256
    private final long validityMillis;       // Access 토큰 유효기간(ms)
    private final SecureRandom rnd = new SecureRandom();

    /**
     * Creates a JwtTokenProvider configured from the given properties.
     *
     * If {@code props} is null or {@code props.secret()} is null/blank, a new HS256 SecretKey is generated
     * (intended for tests/CI). Otherwise a SecretKey is derived from {@code props.secret()} using
     * UTF-8 bytes (recommended to be at least 32 bytes for HS256). The token validity is taken from
     * {@code props.validityMillis()} when > 0; otherwise a default of 3,600,000 ms (1 hour) is used.
     *
     * @param props configuration source; may be null
     */
    public JwtTokenProvider(JwtProperties props) {
        String secret = (props != null) ? props.secret() : null;
        long validity = (props != null && props.validityMillis() > 0) ? props.validityMillis() : 3600000L;

        if (secret == null || secret.isBlank()) {
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256); // 테스트/CI 임시키
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // >=32 bytes 권장
        }
        this.validityMillis = validity;
    }

    /**
     * Creates a signed JWT containing subject (userId), role, optional email and a JTI.
     *
     * The token includes:
     * - `jti` (ID): a URL-safe random identifier for blacklist/logout handling,
     * - `sub` (subject): the user's id,
     * - `role`: normalized to start with `ROLE_` if not already,
     * - `email`: included only when a non-blank value is provided,
     * - `iat` and `exp`: issued-at and expiration based on the provider's validityMillis.
     *
     * @param userId the user identifier stored as the token subject
     * @param role the user's role; will be normalized to start with `ROLE_` if necessary
     * @param email optional email to include as the `email` claim; null or blank values are not included
     * @return the compact serialized JWT string
     */
    public String createToken(Long userId, String role, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(randomId()) // ★ JTI 추가: 블랙리스트/로그아웃용
                .subject(String.valueOf(userId))
                .claim("role", normalizeRole(role))
                .claim("email", (email == null || email.isBlank()) ? null : email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(validityMillis)))
                .signWith(key)
                .compact();
    }

    /**
     * Deprecated compatibility overload that creates a signed JWT for the given user without an email claim.
     *
     * <p>Delegates to {@link #createToken(Long, String, String)} passing a null email.</p>
     *
     * @param userId the numeric user id to set as the token subject
     * @param role the user's role (will be normalized, e.g. "ROLE_USER" if missing the prefix)
     * @return a compact signed JWT string
     * @deprecated use {@link #createToken(Long, String, String)} and provide an explicit email when available
     */
    @Deprecated
    public String createToken(Long userId, String role) {
        return createToken(userId, role, null);
    }

    /**
     * Parses and verifies a signed JWT (JWS) and returns its claims.
     *
     * The provided token must be a compact serialized signed JWT. The method verifies
     * the signature using the provider's signing key and returns the parsed
     * Jws<Claims> on success.
     *
     * @param token compact serialized signed JWT (JWS)
     * @return the parsed JWS with its claims
     * @throws JwtException if the token is invalid, malformed, expired, or signature verification fails
     */
    public Jws<Claims> parse(String token) throws JwtException {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }

    /**
     * Checks whether the provided JWT string is valid and was signed with this provider's key.
     *
     * @param token the compact JWT string to validate
     * @return true if the token can be parsed and verified; false if parsing fails (expired, malformed, invalid signature, or null/invalid input)
     */
    public boolean isValid(String token) {
        try { parse(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }

    /**
     * Extracts the user id from the JWT `sub` (subject) claim.
     *
     * Parses the provided JWT and returns the subject converted to a Long.
     * If the token is invalid, the subject is missing/blank, or cannot be parsed as a Long,
     * an empty Optional is returned.
     *
     * @param token the compact JWT string to inspect
     * @return an Optional containing the user id from the subject claim, or Optional.empty() if unavailable or invalid
     */
    public Optional<Long> getUserId(String token) {
        try {
            String sub = parse(token).getPayload().getSubject();
            return (sub == null || sub.isBlank()) ? Optional.empty() : Optional.of(Long.valueOf(sub));
        } catch (Exception e) { return Optional.empty(); }
    }

    /**
     * Extracts the "role" claim from the given JWT.
     *
     * @param token the compact JWT string to parse
     * @return an Optional containing the role claim if present and token parsing succeeds; otherwise Optional.empty()
     */
    public Optional<String> getRole(String token) {
        try { return Optional.ofNullable(parse(token).getPayload().get("role", String.class)); }
        catch (Exception e) { return Optional.empty(); }
    }

    /**
     * Extracts the `email` claim from the given JWT.
     *
     * Returns an Optional containing the email claim if the token is valid and the claim is present;
     * otherwise returns Optional.empty() (for invalid tokens, parse failures, or when the email claim is missing).
     *
     * @param token the compact JWT string to read
     * @return an Optional with the email value if available
     */
    public Optional<String> getEmail(String token) {
        try { return Optional.ofNullable(parse(token).getPayload().get("email", String.class)); }
        catch (Exception e) { return Optional.empty(); }
    }

    /**
     * Extracts the expiration time (exp) from the given JWT.
     *
     * @param token the compact JWT string to inspect
     * @return an Optional containing the token's expiration Date if present and the token can be parsed; otherwise Optional.empty()
     */
    public Optional<Date> getExpiration(String token) {
        try { return Optional.ofNullable(parse(token).getPayload().getExpiration()); }
        catch (Exception e) { return Optional.empty(); }
    }

    /**
     * Extracts the JWT ID (jti) from the given token.
     *
     * Parses the token and returns the `jti` (ID) claim if present. If the token is invalid,
     * cannot be parsed, or contains no ID, an empty Optional is returned.
     *
     * @param token the compact JWT string
     * @return an Optional containing the token's `jti` claim, or empty if absent or on parse error
     */
    public Optional<String> getJti(String token) {
        try { return Optional.ofNullable(parse(token).getPayload().getId()); }
        catch (Exception e) { return Optional.empty(); }
    }

    /**
 * Returns the token validity duration in milliseconds.
 *
 * This value is the lifespan applied when issuing tokens and is used as the backing
 * value for {@link #getAccessValidityMillis()}.
 *
 * @return token validity in milliseconds
 */
public long getValidityMillis() { return validityMillis; }
    /**
 * Returns the token validity period in milliseconds.
 *
 * This is an alias for {@link #getValidityMillis()} retained for API compatibility.
 *
 * @return the configured token validity duration, in milliseconds
 */
    public long getAccessValidityMillis() { return validityMillis; }

    /**
     * Normalize a role name to the application's expected format.
     *
     * Returns "ROLE_USER" when the input is null or blank. If the input already
     * starts with "ROLE_" it is returned unchanged; otherwise "ROLE_" is
     * prefixed to the input.
     *
     * @param r the role name to normalize; may be null or blank
     * @return the normalized role string (never null)
     */
    private String normalizeRole(String r){
        if (r == null || r.isBlank()) return "ROLE_USER";
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }
    /**
     * Generates a cryptographically secure random identifier suitable for use as a JWT ID (jti).
     *
     * Produces 16 random bytes from the instance SecureRandom and encodes them using URL-safe
     * Base64 without padding.
     *
     * @return a URL-safe, no-padding Base64 string representing 16 random bytes
     */
    private String randomId() {
        byte[] b = new byte[16]; rnd.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}