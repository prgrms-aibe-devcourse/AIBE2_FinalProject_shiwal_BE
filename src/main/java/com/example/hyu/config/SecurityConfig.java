package com.example.hyu.config;

import com.example.hyu.security.CustomAccessDeniedHandler;
import com.example.hyu.security.JwtAuthenticationEntryPoint;
import com.example.hyu.security.JwtAuthenticationFilter;
import com.example.hyu.security.JwtProperties;
import com.example.hyu.security.JwtTokenProvider;
import com.example.hyu.service.TokenStoreService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;


@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /**
     * 개발 중엔 true(전체 공개), 운영 전환 시 false(보호 모드)
     * application.yml에 security.open-mode: true/false 로 제어
     */
    @Value("${security.open-mode:true}")
    private boolean openMode;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider provider,
                                                           TokenStoreService tokenStoreService) {
        // ★ 블랙리스트(JTI) 확인을 위해 TokenStoreService 주입한 필터 사용
        return new JwtAuthenticationFilter(provider, tokenStoreService);
    }

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() { return new JwtAuthenticationEntryPoint(); }

    @Bean
    public CustomAccessDeniedHandler customAccessDeniedHandler() { return new CustomAccessDeniedHandler(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter,
                                           JwtAuthenticationEntryPoint entryPoint,
                                           CustomAccessDeniedHandler denied) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> {
                    auth
                            // 공개 리소스
                            .requestMatchers(
                                    "/", "/index.html",
                                    "/health",
                                    "/auth/**",
                                    "/api/contents/**",
                                    "/auth-test.html", "/jwt-check.html",
                                    "/static/**", "/favicon.ico",
                                    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
                            ).permitAll()
                            // 관리자 전용
                            .requestMatchers("/api/admin/**").hasRole("ADMIN");

                    if (openMode) {
                        // ★ 개발 모드: 나머지도 전부 공개
                        auth.anyRequest().permitAll();
                    } else {
                        // ★ 보호 모드: 그 외는 인증 필요
                        auth.anyRequest().authenticated();
                    }
                })

                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint) // 401
                        .accessDeniedHandler(denied)          // 403
                )

                // JWT 검증 필터 연결 (UsernamePasswordAuthenticationFilter 앞)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}