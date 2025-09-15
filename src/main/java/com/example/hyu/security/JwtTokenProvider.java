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

    public JwtTokenProvider(JwtProperties props) {
        String secret = (props != null) ? props.secret() : null;

        // null-safe Long -> long
        Long v = (props != null) ? props.validityMillis() : null;
        long validity = (v != null && v > 0) ? v : 3600000L;

        if (secret == null || secret.isBlank()) {
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256); // 테스트/CI 임시키
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // >=32 bytes 권장
        }
        this.validityMillis = validity;
    }

    public String createToken(Long userId, String role, String email) {
        Instant now = Instant.now();

        JwtBuilder builder = Jwts.builder()
                .id(randomId()) // JTI
                .subject(String.valueOf(userId))
                .claim("role", normalizeRole(role))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(validityMillis)))
                .signWith(key);

        if (email != null && !email.isBlank()) {
            builder.claim("email", email);
        }
        return builder.compact();
    }

    @Deprecated
    public String createToken(Long userId, String role) {
        return createToken(userId, role, null);
    }

    public Jws<Claims> parse(String token) throws JwtException {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }

    public boolean isValid(String token) {
        try { parse(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }

    public Optional<Long> getUserId(String token) {
        try {
            String sub = parse(token).getPayload().getSubject();
            return (sub == null || sub.isBlank()) ? Optional.empty() : Optional.of(Long.valueOf(sub));
        } catch (Exception e) { return Optional.empty(); }
    }

    public Optional<String> getRole(String token) {
        try { return Optional.ofNullable(parse(token).getPayload().get("role", String.class)); }
        catch (Exception e) { return Optional.empty(); }
    }

    public Optional<String> getEmail(String token) {
        try { return Optional.ofNullable(parse(token).getPayload().get("email", String.class)); }
        catch (Exception e) { return Optional.empty(); }
    }

    public Optional<Date> getExpiration(String token) {
        try { return Optional.ofNullable(parse(token).getPayload().getExpiration()); }
        catch (Exception e) { return Optional.empty(); }
    }

    public Optional<String> getJti(String token) {
        try { return Optional.ofNullable(parse(token).getPayload().getId()); }
        catch (Exception e) { return Optional.empty(); }
    }

    public long getValidityMillis() { return validityMillis; }
    public long getAccessValidityMillis() { return validityMillis; }

    private String normalizeRole(String r){
        if (r == null || r.isBlank()) return "ROLE_USER";
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }

    private String randomId() {
        byte[] b = new byte[16]; rnd.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
