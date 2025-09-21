package com.example.hyu.dto.assessment.admin;

import com.example.hyu.enums.RiskLevel;

/**
 * CMS: 점수 구간 조회 응답 DTO
 */
public record CmsRangeRes(
        Long id,
        Integer minScore, //구간 점수 범위
        Integer maxScore,
        RiskLevel level, //위험도 단계
        String labelKo,//결과 레벨 이름 (예 : 약함)
        String summaryKo, //짧은 요약 (예 : "우울감이 낮습니다")
        String adviceKo //권고 문구 (예 : "규칙적인 생활을 유지하세요)
) {}
