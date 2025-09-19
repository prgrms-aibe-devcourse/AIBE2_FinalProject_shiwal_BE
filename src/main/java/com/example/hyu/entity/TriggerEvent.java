package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity @Table(name = "Trigger_Event")
public class TriggerEvent {

    public enum TriggerType { SUICIDE, VIOLENCE, ETC }
    public enum Status { NEW, REVIEWED, RESOLVED }
    public enum Risk { HIGH, MEDIUM, LOW }
    public enum Source { CHAT, JOURNAL, SELF_TEST } // 채팅, 일기, 자가진단

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "트리거아이디")
    private Long id;

    @Column(name = "유저아이디", nullable = false)
    private Long userId;

    @Lob
    @Column(name = "감지문구")
    private String detectedText;

    @Enumerated(EnumType.STRING)
    @Column(name = "트리거유형", length = 16)
    private TriggerType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "상태", length = 16)
    private Status status;

    @Column(name = "생성일")
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "위험도", length = 10)
    private Risk risk;

    @Enumerated(EnumType.STRING)
    @Column(name = "발생 경로", length = 16)
    private Source source;

    @Column(name = "원본 ID")
    private Long sourceId; // 실제 FK는 안 걸고 코드에서 매핑
}