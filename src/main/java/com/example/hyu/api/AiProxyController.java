package com.example.hyu.api;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * FE(5173) -> BE1(8080, 본 컨트롤러) -> BE2(8001, FastAPI) 프록시
 * DevPlayground 버튼 경로에 맞춰 /api/ai/smoke, /api/ai/analyze-smoke 제공
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiProxyController {

    private final WebClient webClient;

    @Value("${hue.ai.base-url}")
    private String aiBase;

    @Value("${hue.ai.api-key}")
    private String aiKey;

    @PostMapping("/smoke")
    public Mono<ResponseEntity<String>> smoke(@RequestBody String body) {
        // TODO: FastAPI의 실제 경로로 수정 (예: /v1/chat)
        return webClient.post()
                .uri(aiBase + "/v1/chat")
                .header("X-API-Key", aiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toEntity(String.class);
    }

    @PostMapping("/analyze-smoke")
    public Mono<ResponseEntity<String>> analyzeSmoke(@RequestBody String body) {
        // TODO: FastAPI의 실제 경로로 수정 (예: /v1/analyze)
        return webClient.post()
                .uri(aiBase + "/v1/analyze")
                .header("X-API-Key", aiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toEntity(String.class);
    }
}