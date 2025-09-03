package com.example.hyu.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityBeans {

    /**
     * Exposes a Spring PasswordEncoder bean that uses BCrypt for hashing passwords.
     *
     * @return a BCryptPasswordEncoder instance for encoding and verifying passwords
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Creates a JwtTokenProvider bean configured with the supplied JWT properties.
     *
     * @param props JWT configuration properties used to initialize the provider
     * @return a JwtTokenProvider configured according to the given properties
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider(JwtProperties props) {
        return new JwtTokenProvider(props);
    }
}
