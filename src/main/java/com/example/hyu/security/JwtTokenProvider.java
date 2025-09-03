package com.example.hyu.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * JWT 발급/검증 유틸 (JJWT 0.12.x)
 * - test/CI에서 jwt.secret이 비어도 임시키로 동작 (재시작 시 토큰 무효 주의)
 * - 운영/스테이징에선 반드시 강한 secret을 프로퍼티/환경변수로 주입
 */
public class JwtTokenProvider {

    private final SecretKey key;       // HS256용
    private final long validityMillis; // 토큰 유효기간(ms)

    public JwtTokenProvider(JwtProperties props) {
        String secret = (props != null) ? props.secret() : null;
        long validity = (props != null && props.validityMillis() > 0) ? props.validityMillis() : 3600000L;

        if (secret == null || secret.isBlank()) {
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256); // 임시키
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // ≥32바이트 권장
        }
        this.validityMillis = validity;
    }

    /** 권장: email 클레임 포함 발급 */
    public String createToken(Long userId, String role, String email) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(validityMillis)));

        if (email != null && !email.isBlank()) {
            builder.claim("email", email);
        }
        return builder.signWith(key).compact();
    }

    /** 호환용(이메일 없이 발급) — 가능하면 위 오버로드 사용 */
    @Deprecated
    public String createToken(Long userId, String role) {
        return createToken(userId, role, null);
    }

    /** 토큰 파싱(서명/만료 검증). 실패 시 JwtException */
    public Jws<Claims> parse(String token) throws JwtException {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }

    /** 간이 유효성 검사 */
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

    /** 외부에서 만료시간(s) 응답 등에 쓰도록 공개 */
    public long getValidityMillis() {
        return validityMillis;
    }
}
