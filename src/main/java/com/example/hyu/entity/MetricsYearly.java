package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity
@Table(name = "metrics_yearly")
public class MetricsYearly {

    @Id
    @Column(name = "year", nullable = false)
    private Integer year;
    // PK: 연도 (예: 2025)

    @Column(name = "yearly_active_users")
    private Long yearlyActiveUsers;
    // 연간 활성 사용자 수 (YAU, 고유)

    @Column(name = "yearly_new_signups")
    private Long yearlyNewSignups;
    // 연간 신규 가입자 수

    @Column(name = "yearly_ai_active_users")
    private Long yearlyAiActiveUsers;
    // 연간 AI 상담 이용자 수 (고유)

    @Column(name = "avg_session_length_seconds", precision = 10, scale = 2)
    private BigDecimal avgSessionLengthSeconds;
    // 연 평균 세션 길이(초)

    // 위험 이벤트 단계별 연 집계
    @Column(name = "mild_event_count")
    private Long mildEventCount;

    @Column(name = "moderate_event_count")
    private Long moderateEventCount;

    @Column(name = "risk_event_count")
    private Long riskEventCount;

    @Column(name = "high_risk_event_count")
    private Long highRiskEventCount;

    @Column(name = "yearly_checkin_count")
    private Long yearlyCheckinCount;
    // 연간 자가진단 완료 건수

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
    // 집계가 생성된 시각
}