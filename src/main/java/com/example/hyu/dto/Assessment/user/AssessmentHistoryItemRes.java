package com.example.hyu.dto.Assessment.user;

public record AssessmentHistoryItemRes(
        Long submissionId,
        java.time.Instant submittedAt,
        com.example.hyu.entity.AssessmentSubmission.RiskLevel level
) {}
