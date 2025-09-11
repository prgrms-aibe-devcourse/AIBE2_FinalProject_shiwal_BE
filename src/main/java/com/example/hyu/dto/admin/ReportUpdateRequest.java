package com.example.hyu.dto.admin;

public record ReportUpdateRequest(
        String status,  // REVIEWED|DISMISSED|ACTION_TAKEN
        String note     // adminNote (null 가능)
) { }