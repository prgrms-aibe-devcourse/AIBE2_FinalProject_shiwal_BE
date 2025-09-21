package com.example.hyu.dto.assessment.user;

import com.example.hyu.enums.RiskLevel;

public record AssessmentHistoryItemRes(
        Long submissionId,
        java.time.Instant submittedAt,
        RiskLevel level
) {}
