// MetricsRetention.java
// NOTE: 원본 테이블명이 'metrucs_retention'로 오타지만, 매핑은 그대로 맞춰둠.
package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity
@Table(
        name = "metrucs_retention",
        indexes = {
                @Index(name = "idx_retention_date", columnList = "코호트 일자(가입 일자)"),
                @Index(name = "idx_retention_window", columnList = "윈도우")
        }
)
public class MetricsRetention {

    public enum Window { D1, D7, D30 }

    @EmbeddedId
    private Id id;

    @Column(name = "코호트 수")
    private Long cohortSize;

    @Column(name = "복귀 사용자 수")
    private Long returnedUsers;

    @Column(name = "복귀율(%)", precision = 5, scale = 2)
    private BigDecimal returnRate;

    @Column(name = "생성 일시")
    private Instant createdAt;

    // ---- Composite PK ----
    @Embeddable @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Id implements Serializable {
        @Column(name = "코호트 일자(가입 일자)", nullable = false)
        private LocalDate cohortDate;

        @Enumerated(EnumType.STRING)
        @Column(name = "윈도우", nullable = false, length = 10)
        private Window window;
    }
}