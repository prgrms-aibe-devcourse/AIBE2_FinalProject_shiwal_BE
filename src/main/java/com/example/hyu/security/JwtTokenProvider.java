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
        long validity = (props != null && props.validityMillis() > 0) ? props.validityMillis() : 3600000L;

        if (secret == null || secret.isBlank()) {
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256); // 테스트/CI 임시키
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // >=32 bytes 권장
        }
        this.validityMillis = validity;
    }

    /** 권장: email 포함 + JTI 포함 발급 (기존 시그니처 유지) */
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

    /** 호환용(이메일 없이) */
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
    /** 새 코드 편의용 별칭 */
    public long getAccessValidityMillis() { return validityMillis; }

    // util
    private String normalizeRole(String r){
        if (r == null || r.isBlank()) return "ROLE_USER";
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }
    private String randomId() {
        byte[] b = new byte[16]; rnd.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}