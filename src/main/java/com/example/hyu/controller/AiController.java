package com.example.hyu.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/ai", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
public class AiController {

    // 우선순위: application-*.yml(hue.ai.*) → 환경변수(HUE_AI_*) → 기본값
    @Value("${hue.ai.base-url:${HUE_AI_BASE_URL:http://localhost:8001}}")
    private String aiBaseUrl;

    @Value("${hue.ai.api-key:${HUE_API_KEY:dev-key}}")
    private String aiApiKey;

    private final @Qualifier("aiRestTemplate") RestTemplate restTemplate;

    @PostMapping(path = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> chat(@RequestBody ChatReq req) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", aiApiKey);

        Map<String, Object> body = new HashMap<>();
        // FE(camelCase) → AI 서버가 기대하는 snake_case로 변환
        body.put("session_id", req.getSessionId());
        body.put("message", req.getMessage());
        body.put("user_id", req.getUserId());

        ResponseEntity<String> res = restTemplate.postForEntity(
                aiBaseUrl + "/v1/chat",
                new HttpEntity<>(body, headers),
                String.class);
        return ResponseEntity.status(res.getStatusCode())
                .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
                .body(res.getBody());
    }

    @PostMapping(path = "/chatx", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> chatx(@RequestBody ChatReq req) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", aiApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("session_id", req.getSessionId());
        body.put("message", req.getMessage());
        body.put("user_id", req.getUserId());

        ResponseEntity<String> res = restTemplate.postForEntity(
                aiBaseUrl + "/v1/chatx",
                new HttpEntity<>(body, headers),
                String.class);
        return ResponseEntity.status(res.getStatusCode())
                .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
                .body(res.getBody());
    }

    @Data
    public static class ChatReq {
        private String sessionId;
        private String message;
        private Integer userId;
    }
}