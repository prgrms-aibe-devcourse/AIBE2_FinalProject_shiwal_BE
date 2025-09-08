package com.example.hyu.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        Long validityMillis,
        Long refreshValidityMillis,
        String refreshCookieName,
        String refreshCookieDomain,
        Boolean refreshCookieSecure
) {}