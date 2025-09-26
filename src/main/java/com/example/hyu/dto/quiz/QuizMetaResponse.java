package com.example.hyu.dto.quiz;

public record QuizMetaResponse(
        String code,
        String title,
        String category,
        Integer questionCount,
        String shortDescription,
        String durationEstimate,
        String ogImage,
        Integer version,
        String lang
) {}
