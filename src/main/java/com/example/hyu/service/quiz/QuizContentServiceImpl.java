package com.example.hyu.service.quiz;

import com.example.hyu.dto.quiz.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizContentServiceImpl implements QuizContentService {

    private final ObjectMapper om;

    private QuizCatalogResponse catalog;
    // code -> payload(Json)
    private final Map<String, JsonNode> quizByCode = new HashMap<>();

    @PostConstruct
    public void init() {
        try (InputStream is = new ClassPathResource("content/catalog.json").getInputStream()) {
            JsonNode root = om.readTree(is);
            Integer version = root.get("version").asInt();
            List<QuizCatalogItem> items = om.convertValue(root.get("items"), new TypeReference<>() {});
            // 게시용만
            List<QuizCatalogItem> published = items.stream()
                    .filter(it -> Boolean.TRUE.equals(it.isPublished()))
                    .collect(Collectors.toList());
            this.catalog = new QuizCatalogResponse(version, published);

            // 각 퀴즈 JSON 로드 (파일명 규칙: /content/quizzes/{code}.v1.json)
            for (QuizCatalogItem it : published) {
                String path = "content/quizzes/%s.v1.json".formatted(it.code());
                try (InputStream qi = new ClassPathResource(path).getInputStream()) {
                    JsonNode payload = om.readTree(qi);
                    // 간단 검증: code 일치
                    String codeInFile = payload.get("code").asText();
                    if (!Objects.equals(codeInFile, it.code())) {
                        throw new IllegalStateException("Code mismatch in " + path);
                    }
                    quizByCode.put(it.code(), payload);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load quiz content", e);
        }
    }

    @Override
    public QuizCatalogResponse getCatalog() {
        return catalog;
    }

    @Override
    public QuizMetaResponse getMeta(String code) {
        JsonNode p = quizByCode.get(code);
        if (p == null) throw new NoSuchElementException("quiz not found");
        JsonNode meta = p.get("meta");
        return new QuizMetaResponse(
                p.get("code").asText(),
                meta.path("title").asText(),
                // category는 catalog에서 찾음
                findCategory(code),
                meta.path("questionCount").isNumber() ? meta.get("questionCount").asInt() : null,
                findShortDescription(code),
                findDuration(code),
                findOgImage(code),
                p.path("version").isNumber() ? p.get("version").asInt() : null,
                p.path("lang").asText(null)
        );
    }

    @Override
    public JsonNode getQuestionsPayload(String code) {
        JsonNode p = quizByCode.get(code);
        if (p == null) throw new NoSuchElementException("quiz not found");
        return p;
    }

    private String findCategory(String code) {
        return catalog.items().stream()
                .filter(i -> i.code().equals(code))
                .map(QuizCatalogItem::category)
                .findFirst().orElse(null);
    }

    private String findShortDescription(String code) {
        return catalog.items().stream()
                .filter(i -> i.code().equals(code))
                .map(QuizCatalogItem::shortDescription)
                .findFirst().orElse(null);
    }

    private String findDuration(String code) {
        return catalog.items().stream()
                .filter(i -> i.code().equals(code))
                .map(QuizCatalogItem::durationEstimate)
                .findFirst().orElse(null);
    }

    private String findOgImage(String code) {
        return catalog.items().stream()
                .filter(i -> i.code().equals(code))
                .map(QuizCatalogItem::ogImage)
                .findFirst().orElse(null);
    }
}
