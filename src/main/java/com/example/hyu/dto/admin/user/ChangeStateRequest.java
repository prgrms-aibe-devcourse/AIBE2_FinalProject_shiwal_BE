package com.example.hyu.dto.admin.user;

//계정 상태 변경 요청
public record ChangeStateRequest(
        String state,    // ACTIVE | SUSPENDED
        String reason,   // (선택) 정지 사유 문자열
        String period,   // ISO-8601 기간: P1W, P2W, P1M, P100Y 등
        String risk      // WARN | EXEMPT (선택)
) { }
