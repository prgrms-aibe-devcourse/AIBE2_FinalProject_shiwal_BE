package com.example.hyu.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hue.ai")
public record AiProperties(String baseUrl, String apiKey) {
}