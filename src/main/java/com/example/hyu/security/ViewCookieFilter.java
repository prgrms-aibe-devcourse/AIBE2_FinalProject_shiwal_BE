package com.example.hyu.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Component
public class ViewCookieFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "VCID"; // Visitor Cookie ID
    private static final Duration COOKIE_AGE = Duration.ofDays(365); // 1년 유지

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 이미 VCID 쿠키가 있는지 확인
        String existingCookie = null;
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    existingCookie = cookie.getValue();
                    break;
                }
            }
        }

        // 2. 없으면 새로 발급
        if (existingCookie == null) {
            String newId = UUID.randomUUID().toString();

            // ResponseCookie 사용 → SameSite, Secure, HttpOnly 등 세밀한 설정 가능
            ResponseCookie vcidCookie = ResponseCookie.from(COOKIE_NAME, newId)
                    .httpOnly(true)                  // JS에서 접근 불가 (보안 강화)
                    .secure(false)                   // 개발 단계에서는 false, 운영에서는 true (HTTPS)
                    .path("/")                       // 전체 경로에서 쿠키 사용 가능
                    .maxAge(COOKIE_AGE)              // 1년 유지
                    .sameSite("Lax")                 // 기본 안전 모드 (CSRF 방지)
                    .build();

            // 실제 응답 헤더에 쿠키 추가
            response.addHeader(HttpHeaders.SET_COOKIE, vcidCookie.toString());
        }

        // 3. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}

