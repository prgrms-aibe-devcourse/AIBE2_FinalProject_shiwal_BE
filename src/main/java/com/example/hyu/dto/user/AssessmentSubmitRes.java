package com.example.hyu.dto.user;

// 제출 응답(결과)
public record AssessmentSubmitRes(
        String assessmentCode, Integer totalScore,
        String level, String labelKo, String summaryKo, String adviceKo,
        Long submissionId
) {}
