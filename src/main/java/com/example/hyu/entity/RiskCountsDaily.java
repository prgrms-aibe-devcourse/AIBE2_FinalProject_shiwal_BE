// RiskCountsDaily.java
// NOTE: 원본 테이블명이 'rist_counts_daily'로 오타지만, 매핑은 그대로 맞춰둠.
package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity
@Table(
        name = "rist_counts_daily",
        indexes = {
                @Index(name = "idx_risk_counts_date", columnList = "집계 일자")
        }
)
public class RiskCountsDaily {

    public enum Risk { HIGH, MEDIUM, LOW }
    public enum Source { CHAT, ASSESSMENT }

    @EmbeddedId
    private Id id;

    @Column(name = "건수")
    private Integer count;

    @Column(name = "생성 일시")
    private Instant createdAt;

    // ---- Composite PK ----
    @Embeddable @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Id implements Serializable {
        @Column(name = "집계 일자", nullable = false)
        private LocalDate date;

        @Enumerated(EnumType.STRING)
        @Column(name = "위험도", nullable = false, length = 10)
        private Risk risk;

        @Enumerated(EnumType.STRING)
        @Column(name = "발생 경로", nullable = false, length = 20)
        private Source source;
    }
}