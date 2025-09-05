package com.example.hyu.dto.user;

import java.time.Instant;

public record AssessmentHistoryItemRes(
        Long submissionId,
        Instant submittedAt,
        Integer totalScore,
        Integer maxScore,
        String level,
        String labelKo
) {}
