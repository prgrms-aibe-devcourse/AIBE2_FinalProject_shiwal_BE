package com.example.hyu.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes());
        this.validityMillis = props.validityMillis();
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
