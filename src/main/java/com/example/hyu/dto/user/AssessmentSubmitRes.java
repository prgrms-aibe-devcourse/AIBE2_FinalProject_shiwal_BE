package com.example.hyu.dto.user;

// 제출/결과 응답
public record AssessmentSubmitRes(
        Long submissionId,     // 저장된 제출 ID
        String assessmentCode, // 예: "PHQ9"
        Integer totalScore,    // 총점
        Integer maxScore,      // 최대점수(문항수*3) → 퍼센트/게이지 계산 용이
        String level,          // ENUM 문자열 (MILD/MODERATE/RISK/HIGH_RISK)
        String labelKo,        // "약함/중간/위험/초위험"
        String summaryKo,      // 한 줄 요약
        String adviceKo        // 권고 문구(긴 텍스트)
) {}
