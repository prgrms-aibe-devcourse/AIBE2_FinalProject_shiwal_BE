// MetricsYearly.java
package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity
@Table(name = "metrics_yearly")
public class MetricsYearly {

    @Id
    @Column(name = "기준 연도", nullable = false)
    private LocalDate year; // 예: 2025-01-01

    @Column(name = "연간 활성 사용자 합계")
    private Long yearlyActiveUsers;

    @Column(name = "연 신규 가입 수")
    private Long yearlyNewSignups;

    @Column(name = "연 AI 상담 세션 수")
    private Long yearlyAiSessions;

    @Column(name = "평균 세션 길이", precision = 10, scale = 2)
    private BigDecimal avgSessionLengthSeconds;

    @Column(name = "HIGH 이벤트 수")
    private Long highEventCount;

    @Column(name = "일일 체크인 수")
    private Long dailyCheckinCount;

    @Column(name = "생성일시")
    private Instant createdAt;
}