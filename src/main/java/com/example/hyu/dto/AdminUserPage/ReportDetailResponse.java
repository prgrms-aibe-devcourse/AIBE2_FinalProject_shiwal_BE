package com.example.hyu.dto.AdminUserPage;

import java.time.Instant;

public record ReportDetailResponse(
        Long id,
        Instant reportedAt,
        Long reporterId,
        String targetType,
        Long targetId,
        String reason,
        String status,
        String description,
        String attachmentUrl,
        Instant lastReviewedAt,
        String adminNote
) { }