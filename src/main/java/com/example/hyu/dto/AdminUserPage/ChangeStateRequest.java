package com.example.hyu.dto.AdminUserPage;

//계정 상태 변경 요청
public record ChangeStateRequest(
        String state,    // ACTIVE | SUSPENDED
        String reason,   // (선택) 정지 사유 문자열
        String period,   // ISO-8601 기간: P1W, P2W, P1M, P100Y 등
        String riskLevel      // MILD, MODERATE, RISK, HIGH_RISK
) { }
