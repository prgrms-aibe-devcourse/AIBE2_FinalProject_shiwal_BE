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

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}