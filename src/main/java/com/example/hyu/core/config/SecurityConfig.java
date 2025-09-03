package com.example.hyu.core.config;

import com.example.hyu.core.security.CustomUserDetailsService;
import com.example.hyu.core.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setPasswordEncoder(passwordEncoder);
        p.setUserDetailsService(userDetailsService);
        return new ProviderManager(p);
    }

    @Bean
    AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"" +
                    (authException.getMessage() == null ? "Unauthorized" : authException.getMessage()) + "\"}");
        };
    }

    @Bean
    AccessDeniedHandler restAccessDeniedHandler() {
        return (request, response, ex) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"FORBIDDEN\",\"message\":\"Access Denied\"}");
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration c = new CorsConfiguration();
            // 필요 시 특정 도메인으로 좁히세요: https://app.example.com 등
            c.setAllowedOriginPatterns(List.of("*"));
            c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
            c.setAllowedHeaders(List.of("*"));
            c.setAllowCredentials(true); // 쿠키/Authorization 헤더 허용
            c.setMaxAge(3600L);
            return c;
        };
    }

    @Bean
    @Order(0)
    public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http
                .securityMatchers(m -> m.requestMatchers("/auth/**", "/api/**", "/secure/**"))
                .csrf(cs -> cs.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(fl -> fl.disable())
                .httpBasic(b -> b.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(
                                "/auth/signup",
                                "/auth/login",
                                "/auth/refresh",
                                "/actuator/health"
                        ).permitAll()

                        .requestMatchers("/secure/**").authenticated()
                        .requestMatchers("/api/**").authenticated()

                        .requestMatchers("/auth/**").authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(restAuthenticationEntryPoint())
                        .accessDeniedHandler(restAccessDeniedHandler())
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain pagesChain(HttpSecurity http) throws Exception {
        http
                .securityMatchers(m -> m.requestMatchers("/**"))
                .csrf(cs -> cs.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

                        .requestMatchers(
                                "/", "/index", "/index.html", "/test.html",
                                "/error/**", "/favicon.ico"
                        ).permitAll()

                        .anyRequest().permitAll()
                );

        return http.build();
    }
}