package com.example.hyu.dto.adminUserPage;

import java.time.Instant;

public record UserSummaryResponse(
        Long id,
        String name,       // 이름
        String nickname,   // 닉네임
        String email,      // 이메일
        String role,  // USER / ADMIN
        String state,  //ACTIVE, ARCHIVED
        String riskLevel, //MILD, MODERATE, RISK, HIGH_RISK
        Instant joinedAt   // 가입일
) {}