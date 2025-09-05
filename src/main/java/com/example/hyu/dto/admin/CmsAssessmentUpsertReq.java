package com.example.hyu.dto.admin;

// 검사 생성/수정 요청
public record CmsAssessmentUpsertReq(
        String code,        // 검사 코드 (PHQ9, GAD7 등)
        String name,        // 검사 이름
        String category,    // 카테고리
        String description, // 설명
        String status       // ACTIVE / ARCHIVED
) {}
