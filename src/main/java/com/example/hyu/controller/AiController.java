package com.example.hyu.controller;

import com.example.hyu.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/ai", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // POST /api/ai/smoke
    @PostMapping(value = "/smoke", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> chatSmoke(@RequestBody AiService.ChatIn in) {
        return aiService.chatSmoke(in);
    }

    // POST /api/ai/analyze-smoke
    @PostMapping(value = "/analyze-smoke", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> analyzeSmoke(@RequestBody AiService.AnalyzeIn in) {
        return aiService.analyzeSmoke(in);
    }
}