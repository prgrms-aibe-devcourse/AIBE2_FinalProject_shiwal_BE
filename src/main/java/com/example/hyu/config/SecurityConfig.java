package com.example.hyu.config;

import com.example.hyu.security.CustomAccessDeniedHandler;
import com.example.hyu.security.JwtAuthenticationEntryPoint;
import com.example.hyu.security.JwtAuthenticationFilter;
import com.example.hyu.security.JwtProperties;
import com.example.hyu.security.JwtTokenProvider;
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
@EnableConfigurationProperties(JwtProperties.class)
@EnableMethodSecurity // 추가 : 메서드 단위 보안 활성화 (@PreAuthorize 등)
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider provider) {
        return new JwtAuthenticationFilter(provider); // 토큰 유효하면 컨텍스트 세팅, 아니면 무시
    }

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() { return new JwtAuthenticationEntryPoint(); }

    @Bean
    public CustomAccessDeniedHandler customAccessDeniedHandler() { return new CustomAccessDeniedHandler(); }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            JwtAuthenticationEntryPoint entryPoint,
            CustomAccessDeniedHandler denied
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                                // ✅ 완전 공개 경로
                                .requestMatchers(
                                        "/",
                                        "/index.html",
                                        "/health",
                                        "/auth/**",
                                        "/api/contents/**",     // 콘텐츠 열람은 전부 공개
                                        "/auth-test.html",
                                        "/jwt-check.html",
                                        "/static/**",
                                        "/favicon.ico",
                                        "/api/public/password-reset/**"
                                ).permitAll()

                                // 자가진단평가: 공개 엔드포인트만 선별 허용
                                .requestMatchers(HttpMethod.GET,  "/api/assessments").permitAll()                     // 목록
                                .requestMatchers(HttpMethod.GET,  "/api/assessments/by-code/**").permitAll()          // 코드 단건
                                .requestMatchers(HttpMethod.GET,  "/api/assessments/*/questions").permitAll()         // 문항 조회
                                .requestMatchers(HttpMethod.PATCH,"/api/assessments/*/answers").permitAll()           // 답변 업서트(비로그인 허용)
                                .requestMatchers(HttpMethod.POST, "/api/assessments/*/submit").permitAll()            // 제출 확정(비로그인 허용)

                                /* ---- 인증 필요 ---- */
                                // 히스토리/최신 결과 는 로그인 필요
                                .requestMatchers(HttpMethod.GET, "/api/assessments/*/results/**").authenticated()

                                // ✅ 관리자 전용
                                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                                .anyRequest().permitAll()

                        // ⬇︎ 만약 일부 쓰기만 보호하고 싶다면 위 줄 대신 아래 두 줄 사용:
                        // .requestMatchers(HttpMethod.GET, "/**").permitAll()
                        // .anyRequest().authenticated()
                )

                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint) // 401
                        .accessDeniedHandler(denied)          // 403
                )

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
