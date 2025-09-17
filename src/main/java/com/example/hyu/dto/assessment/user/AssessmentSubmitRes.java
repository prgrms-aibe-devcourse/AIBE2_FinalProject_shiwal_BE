package com.example.hyu.dto.assessment.user;

// 제출/결과 응답
public record AssessmentSubmitRes(
        Long submissionId,     // 저장된 제출 ID
        Long assessmentId,
        java.time.Instant submittedAt,
        com.example.hyu.entity.AssessmentSubmission.RiskLevel level,
        String labelKo,
        String summaryKo,
        String adviceKo
) {}
