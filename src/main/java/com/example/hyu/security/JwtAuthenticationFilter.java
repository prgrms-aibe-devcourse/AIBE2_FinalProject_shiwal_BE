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

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtTokenProvider.isValid(token)) {
                var userIdOpt = jwtTokenProvider.getUserId(token);
                var roleOpt   = jwtTokenProvider.getRole(token);

                if (userIdOpt.isPresent()) {
                    String role = roleOpt.orElse("USER");
                    List<SimpleGrantedAuthority> auths =
                            List.of(new SimpleGrantedAuthority("ROLE_" + role));

                    AbstractAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    // principal(원하면 CustomPrincipal 객체로 대체)
                                    userIdOpt.get(),
                                    null,
                                    auths
                            );

                    // 요청 정보 바인딩(optional)
                    authentication.setDetails(request);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

        }

        chain.doFilter(request, response);
    }
}
