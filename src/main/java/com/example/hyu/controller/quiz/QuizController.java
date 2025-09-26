package com.example.hyu.controller.quiz;

import com.example.hyu.dto.quiz.QuizCatalogResponse;
import com.example.hyu.dto.quiz.QuizMetaResponse;
import com.example.hyu.service.quiz.QuizContentService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizContentService svc;

    /** 목록(3개 고정) */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QuizCatalogResponse> list() {
        return ResponseEntity.ok(svc.getCatalog());
    }

    /** 메타(표시 정보) */
    @GetMapping(value = "/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QuizMetaResponse> meta(@PathVariable String code) {
        return ResponseEntity.ok(svc.getMeta(code));
    }

    /** 문항+채점 정의(JSON 그대로) – 프론트 채점 용 */
    @GetMapping(value = "/{code}/questions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> questions(@PathVariable String code) {
        return ResponseEntity.ok(svc.getQuestionsPayload(code));
    }
}
