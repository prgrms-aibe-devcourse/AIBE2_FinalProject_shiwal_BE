package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity @Table(name = "trigger_event")
public class TriggerEvent {

    public enum TriggerType { SUICIDE, VIOLENCE, ETC }
    public enum Status { NEW, REVIEWED, RESOLVED }
    public enum Risk { HIGH, MEDIUM, LOW }
    public enum Source { CHAT, JOURNAL, SELF_TEST }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    // PK

    @Column(name = "user_id", nullable = false)
    private Long userId;
    // 이벤트 발생한 사용자 ID

    @Lob
    @Column(name = "detected_text", nullable = false)
    private String detectedText;
    // 감지된 문구 (예: 위험 신호)

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 16, nullable = false)
    private TriggerType type;
    // 트리거 유형 (자살, 폭력 등)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private Status status;
    // 사건 상태 (NEW → REVIEWED → RESOLVED)

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    // 생성 시각

    @Enumerated(EnumType.STRING)
    @Column(name = "risk", length = 10, nullable = false)
    private Risk risk;
    // 위험도 (HIGH / MEDIUM / LOW)

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 16, nullable = false)
    private Source source;
    // 이벤트 발생 경로 (채팅, 일기, 자가진단 등)

    @Column(name = "source_id")
    private Long sourceId;
    // 원본 데이터 ID (예: 채팅 메시지 ID)
}