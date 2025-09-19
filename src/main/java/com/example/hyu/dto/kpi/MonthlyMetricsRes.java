package com.example.hyu.dto.kpi;
// =====================
// 월 단위 KPI 응답
// =====================

import java.time.LocalDate;

public record MonthlyMetricsRes(
        LocalDate month,             // <해당 월의 시작일> (예: 2025-09-01)
        long monthlyActiveUsers,     // <월간 활성 사용자 수> (MAU)
        long monthlyNewSignups,      // <월 신규 가입자 수>
        long monthlyAiActiveUsers,   // <월간 AI 상담 사용자 수>
        long mildEventCount,         // <MILD 이벤트 수>
        long moderateEventCount,     // <MODERATE 이벤트 수>
        long riskEventCount,         // <RISK 이벤트 수>
        long highRiskEventCount,     // <HIGH_RISK 이벤트 수>
        long monthlyCheckinCount     // <월간 자가진단 제출 수>
) {}