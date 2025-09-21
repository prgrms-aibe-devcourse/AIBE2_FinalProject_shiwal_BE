package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.*;

@Entity
@Table(
        name = "weekly_summaries",
        uniqueConstraints = @UniqueConstraint(
                name="uk_weekly_summary_user_week",
                columnNames = {"user_id","week_start"}
        )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WeeklySummary {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false)
    private Long userId;

    /** 주간 구간(월~일) */
    @Column(name="week_start", nullable=false)
    private LocalDate weekStart;

    @Column(name="week_end", nullable=false)
    private LocalDate weekEnd;

    @Lob
    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable=false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}