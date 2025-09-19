// MetricsMonthly.java
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
    @Column(name = "기준 월", nullable = false)
    private LocalDate month; // 예: 2025-08-01

    @Column(name = "월간 활성 사용자 합계")
    private Long monthlyActiveUsers;

    @Column(name = "월 신규 가입 수")
    private Long monthlyNewSignups;

    @Column(name = "월 AI 상담 세션 수")
    private Long monthlyAiSessions;

    @Column(name = "평균 세션 길이", precision = 10, scale = 2)
    private BigDecimal avgSessionLengthSeconds;

    @Column(name = "HIGH 이벤트 수")
    private Long highEventCount;

    @Column(name = "일일 체크인 수")
    private Long dailyCheckinCount;

    @Column(name = "생성일시")
    private Instant createdAt;
}