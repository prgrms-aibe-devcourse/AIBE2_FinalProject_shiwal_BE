package com.example.hyu.security;

import com.example.hyu.service.TokenStoreService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenStoreService tokenStoreService; // ★ 서버측 블랙리스트/리프레시 저장소

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (!jwtTokenProvider.isValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        // ★ Access 토큰 블랙리스트(JTI) 확인 (서버측 로그아웃/무효화)
        String jti = jwtTokenProvider.getJti(token).orElse(null);
        if (jti != null && tokenStoreService.isBlacklisted(jti)) {
            chain.doFilter(request, response);
            return;
        }

        var userIdOpt = jwtTokenProvider.getUserId(token);
        var roleOpt   = jwtTokenProvider.getRole(token);
        var emailOpt  = jwtTokenProvider.getEmail(token);

        if (userIdOpt.isPresent()) {
            String role = normalizeRole(roleOpt.orElse("ROLE_USER")); // 안전하게 ROLE_ 표준화
            List<SimpleGrantedAuthority> auths = List.of(new SimpleGrantedAuthority(role));

            // ★ 우리 프로젝트의 주체 타입(컨트롤러에서 @AuthenticationPrincipal로 받음)
            AuthPrincipal principal = new AuthPrincipal(userIdOpt.get(), emailOpt.orElse(null), role);

            AbstractAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, auths);

            // (선택) 요청정보를 details에 넣고 싶으면:
            authentication.setDetails(request);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(request, response);
    }

    private String normalizeRole(String r) {
        if (!StringUtils.hasText(r)) return "ROLE_USER";
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }
}
