package com.example.hyu.dto.admin;

public record UpdateReportRequest(
        String status,   // REVIEWED | DISMISSED | ACTION_TAKEN
        String note      // 관리자 메모
) { }
