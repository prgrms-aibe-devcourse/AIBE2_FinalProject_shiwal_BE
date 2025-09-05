package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "messages",
        indexes = {
                @Index(name="idx_messages_session_time", columnList = "세션아이디, 생성시각"),
                @Index(name="idx_messages_sender", columnList = "발신자사용자아이디")
        }
)
public class Message {

    public enum ContentType { TEXT, SYSTEM }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "메시지아이디")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "발신자사용자아이디", foreignKey = @ForeignKey(name = "FK_messages_user"))
    private Users sender;

    @Column(name = "AI여부", nullable = false)
    @Builder.Default
    private boolean ai = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "콘텐츠타입", length = 16, nullable = false)
    @Builder.Default
    private ContentType contentType = ContentType.TEXT;

    @Lob
    @Column(name = "내용", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "답글대상메시지아이디", foreignKey = @ForeignKey(name = "FK_messages_parent"))
    private Message replyTo;

    @Column(name = "생성시각", nullable = false)
    private Instant sentAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "세션아이디", foreignKey = @ForeignKey(name = "FK_messages_session"))
    private Session session;

    @Column(name = "읽은시각")
    private Instant readAt;
}
