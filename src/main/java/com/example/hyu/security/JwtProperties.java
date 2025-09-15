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
) {
    public String secret() {
        return (secret == null || secret.isBlank())
                ? ""  // TokenProvider에서 임시 키 생성 유도
                : secret;
    }

    public Long validityMillis() {
        return (validityMillis != null && validityMillis > 0)
                ? validityMillis
                : 3600000L; // 1시간
    }

    public Long refreshValidityMillis() {
        return (refreshValidityMillis != null && refreshValidityMillis > 0)
                ? refreshValidityMillis
                : 1209600000L; // 14일
    }

    public String refreshCookieName() {
        return (refreshCookieName != null && !refreshCookieName.isBlank())
                ? refreshCookieName
                : "RT";
    }

    public String refreshCookieDomain() {
        return (refreshCookieDomain != null && !refreshCookieDomain.isBlank())
                ? refreshCookieDomain
                : null;
    }

    public Boolean refreshCookieSecure() {
        return (refreshCookieSecure != null)
                ? refreshCookieSecure
                : Boolean.FALSE;
    }
}
