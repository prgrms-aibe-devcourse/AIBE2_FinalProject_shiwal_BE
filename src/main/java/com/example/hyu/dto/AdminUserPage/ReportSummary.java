package com.example.hyu.dto.AdminUserPage;

import java.time.Instant;

public record ReportSummary(
        Integer open,          // 미처리/검토중 신고 수
        Integer total,         // 전체 신고 수
        Instant lastReportedAt // 마지막 신고 시각
) {
    public static ReportSummary empty() { return new ReportSummary(0, 0, null); }
}