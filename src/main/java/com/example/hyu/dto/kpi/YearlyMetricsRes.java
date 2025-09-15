package com.example.hyu.dto.kpi;
// =====================
// 연도 단위 KPI 응답
// =====================

public record YearlyMetricsRes(
        int year,                    // <연도> 예: 2025
        long yearlyActiveUsers,      // <연간 활성 사용자 수> (YAU)
        long yearlyNewSignups,       // <연간 신규 가입자 수>
        long yearlyAiActiveUsers,    // <연간 AI 상담 사용자 수>
        long mildEventCount,         // <MILD 이벤트 수>
        long moderateEventCount,     // <MODERATE 이벤트 수>
        long riskEventCount,         // <RISK 이벤트 수>
        long highRiskEventCount,     // <HIGH_RISK 이벤트 수>
        long yearlyCheckinCount      // <연간 자가진단 제출 수>
) {}
