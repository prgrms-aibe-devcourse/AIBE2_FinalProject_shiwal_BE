package com.example.hyu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()); // Postman 테스트 위해 CSRF 끄기
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").permitAll() // 관리자 API 임시 열기
                .anyRequest().permitAll()                     // 그 외도 다 열기
        );
        return http.build();
    }
}