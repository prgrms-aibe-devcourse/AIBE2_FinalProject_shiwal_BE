// MetricsDaily.java
package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity
@Table(
        name = "metrics_daily",
        indexes = {
                // 일자 범위 조회 최적화 (최근 N일)
                @Index(name = "idx_metrics_daily_date", columnList = "기준일자")
        }
)
public class MetricsDaily {

    @Id
    @Column(name = "기준일자", nullable = false)
    private LocalDate date; // PK

    @Column(name = "일 활성 사용자")
    private Integer dailyActiveUsers;

    @Column(name = "신규 가입자 수")
    private Integer newSignups;

    @Column(name = "AI 상담 세션 수")
    private Integer aiSessionCount;

    @Column(name = "평균 세션 길이")
    private Integer avgSessionLengthSeconds; // 초

    @Column(name = "생성 시각")
    private Instant createdAt;

    @Column(name = "HIGH 이벤트 수")
    private Integer highEventCount;

    @Column(name = "체크인 수")
    private Integer checkinCount;

    @Column(name = "생성일시")
    private Instant generatedAt;
}