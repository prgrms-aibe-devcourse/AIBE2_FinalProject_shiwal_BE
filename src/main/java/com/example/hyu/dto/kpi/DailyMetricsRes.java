package com.example.hyu.dto.kpi;

// =====================
// 하루 단위 KPI 응답
// =====================

import java.time.LocalDate;

public record DailyMetricsRes(
        LocalDate day,               // <일자> 해당 데이터가 계산된 날짜 (yyyy-MM-dd)
        long dailyActiveUsers,       // <일일 활성 사용자 수> 하루 동안 이벤트 발생한 고유 사용자 수
        long newSignups,             // <신규 가입자 수> 해당 일자에 가입한 사용자 수
        long aiActiveUsers,          // <AI 상담 사용자 수> 하루 동안 AI 채팅 메시지를 보낸 사용자 수
        long mildEventCount,         // <MILD 위험 이벤트 수> 자가진단 결과가 mild로 판정된 횟수
        long moderateEventCount,     // <MODERATE 위험 이벤트 수>
        long riskEventCount,         // <RISK 위험 이벤트 수>
        long highRiskEventCount,     // <HIGH_RISK 위험 이벤트 수> (관리자 대시보드에서 핵심 지표)
        long checkinCount            // <자가진단 완료 수> 하루 동안 제출된 검사 수
) {}