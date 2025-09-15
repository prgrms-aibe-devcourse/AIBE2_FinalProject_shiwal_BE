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

    /**
     * Creates a JwtAuthenticationFilter bean that validates incoming JWTs and enforces token blacklist (JTI) checks.
     *
     * <p>The filter is constructed with a JWT token provider and a token store service used to verify whether a token
     * has been revoked/blacklisted.</p>
     *
     * @return a configured JwtAuthenticationFilter instance
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider provider,
                                                           TokenStoreService tokenStoreService) {
        // ★ 블랙리스트(JTI) 확인을 위해 TokenStoreService 주입한 필터 사용
        return new JwtAuthenticationFilter(provider, tokenStoreService);
    }

    /**
     * Registers a JwtAuthenticationEntryPoint bean used to commence authentication for unauthorized requests.
     *
     * <p>This entry point produces the 401 Unauthorized response for requests that fail JWT authentication.
     *
     * @return a JwtAuthenticationEntryPoint instance
     */
    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() { return new JwtAuthenticationEntryPoint(); }

    /**
     * Creates and exposes a CustomAccessDeniedHandler bean used to handle access-denied (HTTP 403) responses.
     *
     * <p>Registered in the Spring context so the application's exception handling can delegate 403 responses
     * to this handler (e.g., configured on the security filter chain).</p>
     */
    @Bean
    public CustomAccessDeniedHandler customAccessDeniedHandler() { return new CustomAccessDeniedHandler(); }

    /**
     * Builds and returns the SecurityFilterChain used by the application.
     *
     * Configures HTTP security for a stateless JWT-based setup:
     * - Disables CSRF, HTTP Basic, and form login.
     * - Sets session management to stateless.
     * - Permits unauthenticated access to public endpoints (root, health, auth paths, static/docs, etc.).
     * - Requires ROLE_ADMIN for "/api/admin/**".
     * - When {@code openMode} is true, all other requests are permitted; when false, all other requests require authentication.
     * - Uses the provided {@code entryPoint} for 401 (unauthorized) handling and {@code denied} for 403 (access denied).
     * - Registers the provided {@code jwtFilter} before {@link org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter}.
     *
     * @return a configured {@link SecurityFilterChain}
     * @throws Exception if building the filter chain fails
     */
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