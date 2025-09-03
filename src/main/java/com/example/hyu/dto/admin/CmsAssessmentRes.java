package com.example.hyu.dto.admin;

// 검사 목록 응답
public record CmsAssessmentRes(
        Long id,
        String code,
        String name,
        String category,
        String status
) {}
