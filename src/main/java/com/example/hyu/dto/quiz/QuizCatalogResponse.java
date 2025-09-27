package com.example.hyu.dto.quiz;


import java.util.List;

public record QuizCatalogResponse(
        Integer version,
        List<QuizCatalogItem> items
) {}