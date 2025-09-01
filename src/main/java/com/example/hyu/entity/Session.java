package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "sessions",
        indexes = {
                @Index(name="idx_sessions_user", columnList = "유저아이디2"),
                @Index(name="idx_sessions_status_updated", columnList = "상태, 최종메시지시각")
        }
)
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "세션아이디")
    private Long id;

    @Column(name = "상태", length = 16, nullable = false)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "AI프리셋키", length = 64)
    private String aiPresetKey;

    @Column(name = "시작시각", nullable = false)
    private Instant startedAt;

    @Column(name = "종료시각")
    private Instant endedAt;

    @Column(name = "최종메시지시각")
    private Instant lastMessageAt;

    @Column(name = "주제", length = 255)
    private String topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "유저아이디2", foreignKey = @ForeignKey(name = "FK_sessions_user"))
    private User user;
}