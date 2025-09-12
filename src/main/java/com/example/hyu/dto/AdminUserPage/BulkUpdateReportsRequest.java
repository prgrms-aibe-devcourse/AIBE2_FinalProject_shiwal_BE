package com.example.hyu.dto.AdminUserPage;

import java.util.List;

public record BulkUpdateReportsRequest(
        List<Long> ids,
        String status,   // REVIEWED | DISMISSED | ACTION_TAKEN
        String note
) { }
