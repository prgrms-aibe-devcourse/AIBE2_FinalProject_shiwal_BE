package com.example.hyu.dto.adminUserPage;

public record ReportUpdateRequest(
        String status,  // PENDING|REVIEWED|ACTION_TAKEN
        String note     // adminNote (null 가능)
) { }