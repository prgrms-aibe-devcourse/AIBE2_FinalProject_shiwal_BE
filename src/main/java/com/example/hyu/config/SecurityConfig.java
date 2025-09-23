package com.example.hyu.config;

import com.example.hyu.repository.UserRepository;
import com.example.hyu.security.CustomAccessDeniedHandler;
import com.example.hyu.security.JwtAuthenticationEntryPoint;
import com.example.hyu.security.JwtAuthenticationFilter;
import com.example.hyu.security.JwtProperties;
import com.example.hyu.security.JwtTokenProvider;
import com.example.hyu.security.SuspensionGuardFilter;
import com.example.hyu.security.ViewCookieFilter;
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

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(JwtProperties.class)
class SecurityConfig {

    /**
     * 개발/운영 모드 스위치
     * application(-dev).yml 에서 security.open-mode=true/false 로 제어
     * (주의: spring.security.open-mode 가 아니라 security.open-mode)
     */
    @Value("${security.open-mode:true}")
    private boolean openMode;

    // JWT 필터(블랙리스트 JTI 검사 포함)
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenProvider provider,
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
    public SuspensionGuardFilter suspensionGuardFilter(UserRepository userRepository) {
        return new SuspensionGuardFilter(userRepository);
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
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

                // 🔹 CorsConfig의 CorsConfigurationSource 를 Security에 연결
                .cors(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> {
                    // CORS 프리플라이트 허용
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // 완전 공개 라우트
                    auth.requestMatchers(
                            "/", "/index.html",
                            "/health",
                            "/auth/**",
                            "/api/contents/**",
                            "/auth-test.html", "/jwt-check.html",
                            "/static/**", "/favicon.ico",
                            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                            "/sessions/**", "/checkins/**",
                            "/api/public/password-reset/**").permitAll();

                    // 공개 GET
                    auth.requestMatchers(HttpMethod.GET,
                            "/api/assessments",
                            "/api/assessments/*/questions",
                            "/api/assessments/by-code/**",
                            "/api/community-posts/**",
                            "/api/community-posts/*/comments/**",
                            "/api/community-posts/*/likes/count",
                            "/api/community-posts/*/comments/*/likes/count",
                            "/api/community-posts/*/polls",
                            "/api/polls/*",
                            "/api/polls/*/results").permitAll();

                    // 공개 PATCH/POST (선택적)
                    auth.requestMatchers(HttpMethod.PATCH, "/api/assessments/*/answers").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/assessments/*/submit").permitAll();

                    // 🔹 DevPlayground 스모크용: /api/ai/** 경로
                    if (openMode) {
                        auth.requestMatchers("/api/ai/**").permitAll(); // dev: 열어둠
                    } else {
                        auth.requestMatchers("/api/ai/**").authenticated(); // prod: 보호
                    }

                    // 보호: 인증 필요
                    auth.requestMatchers(HttpMethod.GET, "/api/assessments/*/results/**").authenticated();
                    auth.requestMatchers(
                            "/api/community-posts/**",
                            "/api/community-posts/*/comments/**",
                            "/api/community-posts/*/likes/**",
                            "/api/community-posts/*/comments/*/likes/**",
                            "/api/community-posts/*/polls",
                            "/api/polls/*/vote",
                            "/api/polls/*",
                            "/api/reports/**").authenticated();

                    // --- 관리자/액추에이터 ---
                    if (openMode) {
                        auth.requestMatchers("/api/admin/**").permitAll();
                        auth.requestMatchers("/actuator/**").permitAll();
                    } else {
                        auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
                        auth.requestMatchers("/actuator/**").hasRole("ADMIN");
                    }

                    // 나머지
                    if (openMode) {
                        auth.anyRequest().permitAll(); // dev: 광범위 공개
                    } else {
                        auth.anyRequest().authenticated(); // prod: 기본 보호
                    }
                })

                // 401/403 핸들러
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(denied))

                // 필터 순서: JWT 인증 → 정지/탈퇴 가드 → ViewCookie
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(suspensionGuardFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(viewCookieFilter, SuspensionGuardFilter.class);

        return http.build();
    }
}