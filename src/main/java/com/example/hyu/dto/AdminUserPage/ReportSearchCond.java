package com.example.hyu.dto.AdminUserPage;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public record ReportSearchCond(
        String q,                 // description 키워드
        String status,            // PENDING|REVIEWED|DISMISSED|ACTION_TAKEN
        String reason,            // SPAM|ABUSE|SUICIDE|VIOLENCE|OTHER
        String targetType,        // POST|COMMENT|CONTENT|USER
        Long targetId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
) { }