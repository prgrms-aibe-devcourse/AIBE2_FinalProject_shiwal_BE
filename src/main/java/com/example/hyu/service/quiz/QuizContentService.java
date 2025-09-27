package com.example.hyu.service.quiz;

import com.example.hyu.dto.quiz.*;
import com.fasterxml.jackson.databind.JsonNode;

public interface QuizContentService {
    QuizCatalogResponse getCatalog();
    QuizMetaResponse getMeta(String code);
    JsonNode getQuestionsPayload(String code); // 원본 JSON 그대로 반환(프론트 채점)
}