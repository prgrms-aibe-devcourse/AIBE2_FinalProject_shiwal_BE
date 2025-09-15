package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity
@Table(name = "metrics_monthly")
public class MetricsMonthly {

    @Id
    @Column(name = "month", nullable = false)
    private LocalDate month;
    // PK: 해당 월의 첫날 (yyyy-MM-01), 월 단위 집계 기준일

    @Column(name = "monthly_active_users")
    private Long monthlyActiveUsers;
    // 월간 활성 사용자 수 (MAU, 고유)

    @Column(name = "monthly_new_signups")
    private Long monthlyNewSignups;
    // 월간 신규 가입자 수

    @Column(name = "monthly_ai_active_users")
    private Long monthlyAiActiveUsers;
    // 월간 AI 상담 이용자 수 (고유)

    @Column(name = "avg_session_length_seconds", precision = 10, scale = 2)
    private BigDecimal avgSessionLengthSeconds;
    // 월 평균 세션 길이(초), 소수점 저장 가능

    // 위험 이벤트 단계별 월 집계
    @Column(name = "mild_event_count")
    private Long mildEventCount;

    @Column(name = "moderate_event_count")
    private Long moderateEventCount;

    @Column(name = "risk_event_count")
    private Long riskEventCount;

    @Column(name = "high_risk_event_count")
    private Long highRiskEventCount;

    @Column(name = "monthly_checkin_count")
    private Long monthlyCheckinCount;
    // 월간 자가진단 완료 건수

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
    // 집계가 생성된 시각
}