package com.example.hyu.dto.adminUserPage;

import java.time.Instant;

public record ReportListItemResponse(
        Long id,
        Instant createdAt,
        Actor reporter,       // 신고자 요약
        TargetRef target,     // 대상(타입/ID)
        String reason,
        String status,
        Instant reviewedAt
) {
    public record Actor(Long id, String nickname, String emailMasked) {}
    public record TargetRef(String type, Long id) {}
}