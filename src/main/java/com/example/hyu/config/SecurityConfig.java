package com.example.hyu.config;

import com.example.hyu.repository.UserRepository;
import com.example.hyu.security.*;
import com.example.hyu.service.TokenStoreService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(JwtProperties.class)
class SecurityConfig {

    // 개발 중 전체 공개/보호모드 스위치 (application.yml: security.open-mode)
    @Value("${security.open-mode:true}")
    private boolean openMode;

    // JWT 필터(블랙리스트 JTI 검사 포함)
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider provider,
                                                           TokenStoreService tokenStoreService) {
        return new JwtAuthenticationFilter(provider, tokenStoreService);
    }

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    @Bean
    public CustomAccessDeniedHandler customAccessDeniedHandler() {
        return new CustomAccessDeniedHandler();
    }

    // 정지/탈퇴 전역 차단 필터
    @Bean
    public SuspensionGuardFilter suspensionGuardFilter(UserRepository userRepository){
        return new SuspensionGuardFilter(userRepository);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("http://localhost:5173")); // 프론트 dev 서버
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter,
                                           JwtAuthenticationEntryPoint entryPoint,
                                           CustomAccessDeniedHandler denied,
                                           SuspensionGuardFilter suspensionGuardFilter,
                                           ViewCookieFilter viewCookieFilter) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())

                // 인가 규칙 병합본
                .authorizeHttpRequests(auth -> {

                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    auth.requestMatchers(
                            "/", "/index.html",
                            "/health",
                            "/auth/**",
                            "/api/contents/**",
                            "/auth-test.html", "/jwt-check.html",
                            "/static/**", "/favicon.ico",
                            "/v3/api-docs/**", "/swagger-ui/**", "/sessions/**","/swagger-ui.html", "/checkins/**",
                            "/api/public/password-reset/**"
                    ).permitAll();

                    // 공개 엔드포인트
                    auth.requestMatchers(HttpMethod.GET,  "/api/assessments",
                                                        "/api/assessments/*/questions",
                                                        "/api/assessments/by-code/**").permitAll();
                    auth.requestMatchers(HttpMethod.PATCH,"/api/assessments/*/answers").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/assessments/*/submit").permitAll();
                    auth.requestMatchers(HttpMethod.GET,  "/api/community-posts/**",
                                                        "/api/community-posts/*/comments/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET,
                            "/api/community-posts/*/likes/count",
                            "/api/community-posts/*/comments/*/likes/count"
                    ).permitAll();
                    auth.requestMatchers(HttpMethod.GET,
                            "/api/community-posts/*/polls",
                            "/api/polls/*",
                            "/api/polls/*/results"
                    ).permitAll();

                    // 인증 필요
                    auth.requestMatchers(HttpMethod.GET, "/api/assessments/*/results/**").authenticated();
                    auth.requestMatchers(
                            "/api/community-posts/**",
                            "/api/community-posts/*/comments/**",
                            "/api/community-posts/*/likes/**",
                            "/api/community-posts/*/comments/*/likes/**"
                    ).authenticated();
                    auth.requestMatchers(
                            "/api/community-posts/*/polls",
                            "/api/polls/*/vote",
                            "/api/polls/*"
                    ).authenticated();
                    auth.requestMatchers(HttpMethod.POST, "/api/reports/**").authenticated();
                    auth.requestMatchers(
                            HttpMethod.POST,
                            "/api/community/posts/*/report",
                            "/api/community/comments/*/report"
                    ).authenticated();


                    // 관리자 전용
                    auth.requestMatchers("/api/admin/**").hasRole("ADMIN");

                    // openMode 스위치 (develop)
                    if (openMode) {
                        auth.anyRequest().permitAll();                            // 개발모드: 전부 공개
                    } else {
                        auth.anyRequest().authenticated();                        // 보호모드: 인증 필요
                    }
                })

                // 401/403 핸들러
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(denied)
                )

                // 필터 순서: JWT 인증 → 정지/탈퇴 가드 -> ViewCookie
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(suspensionGuardFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(viewCookieFilter,SuspensionGuardFilter.class);

        return http.build();
    }
}