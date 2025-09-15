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
@Table(name = "metrics_retention")
public class MetricsRetention {

    public enum Window { D1, D7, D30 }

    @Embeddable @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Id implements Serializable {
        @Column(name = "cohort_day", nullable = false)
        private LocalDate cohortDay;
        // 코호트 기준일 (가입일자)

        @Enumerated(EnumType.STRING)
        @Column(name = "ret_window", nullable = false, length = 10)
        private Window window;
        // 리텐션 윈도우 (D1, D7, D30)
    }

    @EmbeddedId
    private Id id;

    @Column(name = "users_total", nullable = false)
    private Long cohortSize;
    // 해당 코호트(가입일)의 전체 사용자 수

    @Column(name = "users_returned", nullable = false)
    private Long returnedUsers;
    // 해당 윈도우 안에 다시 돌아온 사용자 수

    @Column(name = "rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal returnRate;
    // 복귀율 (0.00 ~ 100.00)

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
    // 집계 생성 시각
}