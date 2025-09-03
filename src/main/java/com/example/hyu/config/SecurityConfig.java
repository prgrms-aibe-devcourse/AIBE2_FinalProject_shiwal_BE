package com.example.hyu.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import com.example.hyu.security.JwtProperties;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /**
     * Configures and returns the application's SecurityFilterChain.
     *
     * Disables CSRF and currently permits unauthenticated access to /health, /auth/**,
     * /api/admin/**, /static/** and, via the fallback rule, all other requests (no
     * authentication enforced). The method returns the built SecurityFilterChain.
     *
     * @param http the HttpSecurity builder to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if an error occurs while configuring or building the filter chain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Postman/프론트 초기 통신 편의를 위해 CSRF 일단 비활성화
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/health",
                                "/auth/**",
                                "/api/admin/**",
                                "/static/**"
                        ).permitAll()
                        .anyRequest().permitAll()
                );

        // TODO: 보호 API 생기면 아래처럼 전환
        // .authorizeHttpRequests(auth -> auth
        //     .requestMatchers("/health", "/auth/**", "/static/**").permitAll()
        //     .requestMatchers("/api/admin/**").hasRole("ADMIN")
        //     .anyRequest().authenticated()
        // )
        // .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        // .exceptionHandling(e -> e
        //     .authenticationEntryPoint(jwtAuthenticationEntryPoint)
        //     .accessDeniedHandler(customAccessDeniedHandler)
        // );

        return http.build();
    }
}