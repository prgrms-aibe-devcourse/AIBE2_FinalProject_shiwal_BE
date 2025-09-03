package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "daily_checkin")
public class DailyCheckin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "checkin_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // FK → user(유저아이디)

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "mood", nullable = false)
    private Integer mood;            // 1~10

    @Column(name = "sleep_quality", nullable = false)
    private Integer sleepQuality;    // 1~10

    @Column(name = "energy", nullable = false)
    private Integer energy;          // 1~10

    @Column(name = "stress", nullable = false)
    private Integer stress;          // 1~10

    @Lob
    @Column(name = "memo")
    private String memo;             // nullable
}