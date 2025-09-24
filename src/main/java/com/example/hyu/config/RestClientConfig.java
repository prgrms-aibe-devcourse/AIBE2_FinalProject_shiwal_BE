package com.example.hyu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    // application-*.yml에서 오버라이드 가능
    @Value("${hue.ai.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${hue.ai.read-timeout-ms:20000}")
    private int readTimeoutMs;

    @Bean(name = "aiRestTemplate")
    public RestTemplate aiRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }
}