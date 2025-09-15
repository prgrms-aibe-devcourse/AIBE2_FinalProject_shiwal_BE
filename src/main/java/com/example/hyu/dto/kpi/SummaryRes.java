package com.example.hyu.dto.kpi;

// =====================
// 관리자 대시보드 요약
// =====================

public record SummaryRes(
        long highRiskTotal,          // <전체 HIGH_RISK 이벤트 수>
        long highRiskFromChat,       // <AI 상담에서 발생한 HIGH_RISK 이벤트 수>
        long highRiskFromAssessment, // <자가진단에서 발생한 HIGH_RISK 이벤트 수>
        long aiActiveUsers,          // <AI 상담 사용자 수> (distinct userId)
        long selfAssessmentUsers     // <자가진단 완료 사용자 수> (distinct userId)
) {}
