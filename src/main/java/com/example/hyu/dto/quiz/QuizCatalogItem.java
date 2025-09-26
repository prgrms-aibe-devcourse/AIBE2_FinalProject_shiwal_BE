package com.example.hyu.dto.quiz;

public record QuizCatalogItem(
        String code,
        String title,
        String category,         // psych | personality | stress
        String shortDescription,
        Integer questionCount,
        String durationEstimate,
        Boolean isPublished,
        String ogImage,
        String updatedAt
) {}