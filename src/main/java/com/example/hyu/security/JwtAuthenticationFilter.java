package com.example.hyu.security;

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
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // 1) Authorization 헤더에서 Bearer 토큰 추출
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.toLowerCase().startsWith("bearer ")) { // ★ 변경: 견고한 파싱
            String token = header.substring(7).trim(); // ★ 변경: 공백 제거

            // 2) 토큰 유효성 검사
            if (jwtTokenProvider.isValid(token)) {
                var userIdOpt = jwtTokenProvider.getUserId(token);  // Optional<Long>
                var roleOpt   = jwtTokenProvider.getRole(token);    // Optional<String>("ADMIN"/"USER")
                var emailOpt  = jwtTokenProvider.getEmail(token);   // ★ 변경: Optional<String>, 없으면 empty 반환

                // 3) 인증 객체 생성 (principal은 AuthPrincipal로!)
                if (userIdOpt.isPresent() && roleOpt.isPresent()) {  // ★ 변경: role까지 확인
                    String role = roleOpt.get(); // "ADMIN" | "USER" ...
                    var principal = new AuthPrincipal(                 // ★ 변경: Long → AuthPrincipal
                            userIdOpt.get(),
                            emailOpt.orElse(null),                      // email 클레임 없으면 null
                            role
                    );

                    // Spring Security는 "ROLE_" 접두사가 붙은 권한을 기대
                    List<SimpleGrantedAuthority> authorities =
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)); // ★ 변경

                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal,                                     // ★ 변경: principal 교체
                            null,
                            authorities
                    );

                    // 표준 details 세팅 (IP/세션 등)
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); // ★ 변경
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

        }

        chain.doFilter(request, response);
    }
}
