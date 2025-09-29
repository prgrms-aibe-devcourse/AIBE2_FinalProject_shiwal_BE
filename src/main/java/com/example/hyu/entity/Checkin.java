package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(
        name = "checkins",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_checkins_user_date",
                columnNames = {"user_id","date"}
        )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Checkin {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false)
    private Long userId;

    /** 사용자 기준(KST) 날짜 */
    @Column(nullable=false)
    private LocalDate date;

    @Column(name = "created_at", nullable=false, updatable = false)
    private Instant createdAt;

    // ── 최소 확장 필드 ──
    private Integer mood;                 // 1~5 (nullable)
    @Column(length = 200)
    private String note;
    // 메모(200자 제한)
    private Integer energy;               // 1~5 (nullable)
    private Integer stress;               // 1~5 (nullable)

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}