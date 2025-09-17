package com.example.hyu.dto.assessment.admin;

import com.example.hyu.entity.Assessment;

import java.time.Instant;

// 검사 목록 응답
public record CmsAssessmentRes(
        Long id,
        String code, // 검사 코드
        String name, // 검사 이름
        String category, // 검사 카테고리
        Assessment.Status status, // 활성/비활성 여부
        boolean deleted, //삭제 여부
        Instant deletedAt // 삭제 시각
) {}
