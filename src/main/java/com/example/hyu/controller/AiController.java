package com.example.hyu.controller;

import com.example.hyu.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/ai",
        // 클래스 레벨에서 응답을 항상 UTF-8 JSON으로 고정
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // POST /api/ai/smoke
    @PostMapping(value = "/smoke", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
            + ";charset=UTF-8")
    public Mono<Map<String, Object>> chatSmoke(@RequestBody AiService.ChatIn in) {
        return aiService.chatSmoke(in);
    }

    // POST /api/ai/analyze-smoke
    @PostMapping(value = "/analyze-smoke", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
            + ";charset=UTF-8")
    public Mono<Map<String, Object>> analyzeSmoke(@RequestBody AiService.AnalyzeIn in) {
        return aiService.analyzeSmoke(in);
    }
}