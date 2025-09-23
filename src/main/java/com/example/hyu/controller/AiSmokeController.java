package com.example.hyu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
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

        // 이름 바뀐 빈을 명시적으로 주입
        private final @Qualifier("aiRestTemplate") RestTemplate restTemplate;

        @PostMapping(path = "/smoke", produces = "application/json;charset=UTF-8")
        public ResponseEntity<String> chatSmoke(@RequestBody Map<String, Object> body) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-API-Key", aiApiKey);

                ResponseEntity<String> res = restTemplate.postForEntity(aiBaseUrl + "/v1/chat",
                                new HttpEntity<>(body, headers), String.class);

                return ResponseEntity.status(res.getStatusCode())
                                .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
                                .body(res.getBody());
        }

        @PostMapping(path = "/analyze-smoke", produces = "application/json;charset=UTF-8")
        public ResponseEntity<String> analyzeSmoke(@RequestBody Map<String, Object> body) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-API-Key", aiApiKey);

                ResponseEntity<String> res = restTemplate.postForEntity(aiBaseUrl + "/v1/analyze",
                                new HttpEntity<>(body, headers), String.class);

                return ResponseEntity.status(res.getStatusCode())
                                .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
                                .body(res.getBody());
        }
}