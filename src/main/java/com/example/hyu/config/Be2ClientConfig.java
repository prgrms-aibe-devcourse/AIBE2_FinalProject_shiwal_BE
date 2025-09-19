package com.example.hyu.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class Be2ClientConfig {

    @Bean
    public WebClient be2WebClient(WebClient.Builder builder) {
        // BE2는 snake_case 바디를 받음
        ObjectMapper snake = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs()
                        .jackson2JsonEncoder(new Jackson2JsonEncoder(snake, MediaType.APPLICATION_JSON)))
                .codecs(c -> c.defaultCodecs()
                        .jackson2JsonDecoder(new Jackson2JsonDecoder(snake, MediaType.APPLICATION_JSON)))
                .build();

        String baseUrl = System.getenv().getOrDefault("AI_BASE_URL", "http://localhost:8001");
        String apiKey  = System.getenv().getOrDefault("AI_API_KEY" , "");

        WebClient.Builder b = builder
                .baseUrl(baseUrl)
                .exchangeStrategies(strategies);

        if (!apiKey.isBlank()) {
            b = b.defaultHeader("X-API-Key", apiKey);
        }
        return b.build();
    }
}