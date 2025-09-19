package com.example.hyu.dto.kpi;

// =====================
// 위험 감지 타임라인 포인트( 막대 차트용)
// =====================

import java.time.LocalDate;

public record RiskTimelinePoint(
        LocalDate day,               // <일자> yyyy-MM-dd
        long highRiskEventCount      // <HIGH_RISK 이벤트 수> (하루 단위)
) {}
