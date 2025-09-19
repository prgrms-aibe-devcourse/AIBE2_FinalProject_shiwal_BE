package com.example.hyu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private final WebClient be2WebClient;

    // /v1/chat 포워딩 (대화 스모크)
    public Mono<Map<String, Object>> chatSmoke(ChatIn in) {
        Map<String, Object> body = Map.of(
                "message", in.message(),
                "session_id", in.sessionId(),
                "user_id", in.userId()
        );
        return be2WebClient.post()
                .uri("/v1/chat")
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, r ->
                        r.bodyToMono(String.class)
                         .map(m -> new ResponseStatusException(r.statusCode(), m)))
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    // /v1/analyze 포워딩 (분석 스모크)
    public Mono<Map<String, Object>> analyzeSmoke(AnalyzeIn in) {
        Map<String, Object> body = Map.of(
                "text", in.text(),
                "session_id", in.sessionId(),
                "user_id", in.userId()
        );
        return be2WebClient.post()
                .uri("/v1/analyze")
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, r ->
                        r.bodyToMono(String.class)
                         .map(m -> new ResponseStatusException(r.statusCode(), m)))
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    // 간단 DTO (별도 파일 없이 record로)
    public record ChatIn(String message, String sessionId, Long userId) {}
    public record AnalyzeIn(String text, String sessionId, Long userId) {}
}