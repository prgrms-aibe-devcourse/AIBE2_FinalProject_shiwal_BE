package com.example.hyu.dto.adminUserPage;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record UserSearchCond(
        String q,   // 이메일/닉네임/이름 검색
        String role, // USER / ADMIN
        String state, // ACTIVE / SUSPENDED / WITHDRAWN
        String riskLevel, // MILD / MODERATE / RISK / HIGH_RISK
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinedFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinedTo
) { }