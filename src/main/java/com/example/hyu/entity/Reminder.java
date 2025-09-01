package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity
@Table(name = "reminders")
public class Reminder {

    public enum Channel { EMAIL, PUSH, INAPP }
    public enum Kind { CHECKIN, SESSION, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "리마인더아이")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "채널", length = 16, nullable = false)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "종류", length = 32, nullable = false)
    private Kind kind;

    @Lob
    @Column(name = "페이로드JSON")
    private String payloadJson;          // 템플릿 변수, 딥링크 등

    @Column(name = "스케줄", length = 255)
    private String schedule;             // cron 또는 RRULE

    @Column(name = "다음실행시각")
    private Instant nextRunAt;

    @Column(name = "최종발송시각")
    private Instant lastSentAt;

    @Column(name = "활성화여부", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "생성시각", nullable = false)
    private Instant createdAt;

    @Column(name = "유저아이디", nullable = false)
    private Long userId;                 // FK → user(유저아이디)
}