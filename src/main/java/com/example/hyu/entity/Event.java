package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "events",
        indexes = {
                @Index(name="idx_events_time", columnList="eventTime"),
                @Index(name="idx_events_user_time", columnList="userId,eventTime"),
                @Index(name="idx_events_name_time", columnList="eventName,eventTime")
        },
        uniqueConstraints = {
                @UniqueConstraint(name="uk_events_idem", columnNames="idempotencyKey")
        }
)
@Getter @Setter @NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String eventName;
    // page_view(사용자가 화면 진입/ 탭 전환  등 일반 방문)
    // ai_chat_user_message (AI 채팅에서 유저가 메시지 보냄)
    // self_assessment_completed (자가진단 완료 시)
    // risk_detected (ai/자가진단으로 위험 판단 났을때)

    @Column(columnDefinition = "datetime(3)")
    private LocalDateTime eventTime;   // UTC로 저장할 예정

    private String status = "ok";      // 기본값 ok

    private String level;              // 위험레벨

    private String sessionId;

    private String channel;

    private String idempotencyKey;

    @Column(columnDefinition = "json")
    private String meta;

    @Column(nullable = false, columnDefinition="timestamp default current_timestamp")
    private Instant createdAt;

    @PrePersist
    public void prePersist(){
        if(createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
