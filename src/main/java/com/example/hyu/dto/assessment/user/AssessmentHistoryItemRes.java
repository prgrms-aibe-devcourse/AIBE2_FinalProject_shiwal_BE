package com.example.hyu.dto.assessment.user;

public record AssessmentHistoryItemRes(
        Long submissionId,
        java.time.Instant submittedAt,
        com.example.hyu.entity.AssessmentSubmission.RiskLevel level
) {}
