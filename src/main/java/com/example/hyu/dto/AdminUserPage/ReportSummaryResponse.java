package com.example.hyu.dto.AdminUserPage;

import java.time.Instant;

public record ReportSummaryResponse(
        Long id,
        Instant reportedAt,
        Long reporterId,
        String targetType,
        Long targetId,
        String reason,
        String status,
        Instant lastReviewedAt
) { }