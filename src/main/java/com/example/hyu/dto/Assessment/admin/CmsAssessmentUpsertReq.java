package com.example.hyu.dto.Assessment.admin;

import com.example.hyu.entity.Assessment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 검사 생성/수정 요청
public record CmsAssessmentUpsertReq(
        @NotBlank String code,        // 검사 코드 (PHQ9, GAD7 등)
        @NotBlank String name,        // 검사 이름
        @NotBlank String category,    // 카테고리
        @Size(max = 200)String description, // 설명
        Assessment.Status status       // ACTIVE / ARCHIVED
) {}
