package com.example.hyu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiSmokeController {

    @Value("${AI_BASE_URL:http://localhost:8001}")
    private String aiBaseUrl;

    @Value("${AI_API_KEY:dev-key}")
    private String aiApiKey;

    private final RestTemplate restTemplate;

    @PostMapping(path = "/smoke", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> chatSmoke(@RequestBody Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", aiApiKey);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

        ResponseEntity<String> res =
                restTemplate.postForEntity(aiBaseUrl + "/v1/chat", req, String.class);

        MediaType jsonUtf8 = MediaType.parseMediaType("application/json;charset=UTF-8");
        return ResponseEntity.status(res.getStatusCode())
                .contentType(jsonUtf8)
                .body(res.getBody());
    }

    @PostMapping(path = "/analyze-smoke", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> analyzeSmoke(@RequestBody Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", aiApiKey);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

        ResponseEntity<String> res =
                restTemplate.postForEntity(aiBaseUrl + "/v1/analyze", req, String.class);

        MediaType jsonUtf8 = MediaType.parseMediaType("application/json;charset=UTF-8");
        return ResponseEntity.status(res.getStatusCode())
                .contentType(jsonUtf8)
                .body(res.getBody());
    }
}