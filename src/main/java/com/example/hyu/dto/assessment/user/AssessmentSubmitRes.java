package com.example.hyu.dto.assessment.user;

import com.example.hyu.enums.RiskLevel;

// 제출/결과 응답
public record AssessmentSubmitRes(
        Long submissionId,     // 저장된 제출 ID
        Long assessmentId,
        java.time.Instant submittedAt,
        RiskLevel level,  // 위험도
        String labelKo,  // 결과 레벨
        String summaryKo,  // 요약 해석
        String adviceKo  // 권고문구
) {}
