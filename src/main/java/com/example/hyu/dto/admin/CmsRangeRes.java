package com.example.hyu.dto.admin;

/**
 * CMS: 점수 구간 조회 응답 DTO
 */
public record CmsRangeRes(
        Long id,
        Integer minScore,
        Integer maxScore,
        String level,
        String labelKo,
        String summaryKo,
        String adviceKo
) {}
