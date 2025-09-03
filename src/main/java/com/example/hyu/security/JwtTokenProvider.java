package com.example.hyu.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

public class JwtTokenProvider {
    private final Key key;
    private final long validityMillis;

    /**
     * Constructs a JwtTokenProvider by deriving an HMAC-SHA signing key and token validity.
     *
     * The constructor derives a signing Key from the provided JwtProperties secret and
     * initializes the token validity duration (milliseconds) from the properties.
     *
     * @param props configuration containing the signing secret and token validity duration
     */
    public JwtTokenProvider(JwtProperties props) {
        String secret = (props != null) ? props.secret() : null;
        long validity = (props != null && props.validityMillis() > 0) ? props.validityMillis() : 3600000L;

        if (secret == null || secret.isBlank()) {
            // 프로퍼티 없으면 테스트/CI용 임시 키 생성(HS256)
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        } else {
            byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
            // hmacShaKeyFor는 최소 32바이트 필요 → 안되면 예외 발생
            this.key = Keys.hmacShaKeyFor(bytes);
        }
        this.validityMillis = validity;
    }

    /**
     * Creates a signed JWT for the given user containing a "role" claim.
     *
     * The token's subject is the string form of {@code userId}, issued at the current time,
     * and expires after the provider's configured validity period. The token is signed
     * using this provider's signing key.
     *
     * @param userId the user identifier to set as the token subject
     * @param role the role value to include in the token's "role" claim
     * @return a compact JWT string
     */
    public String createToken(Long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(validityMillis)))
                .signWith(key)
                .compact();
    }
}