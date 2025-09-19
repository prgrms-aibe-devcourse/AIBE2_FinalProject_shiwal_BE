package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.Instant;

@Entity
@Table(
        name = "chat_messages",
        indexes = { @Index(name = "idx_msg_session_created", columnList = "session_id, createdAt") }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 세션 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    /** 소유자(세션의 사용자 캐시; 접근 제어용) */
    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role; // USER, ASSISTANT, SYSTEM

    @Lob
    @Column(nullable = false, columnDefinition = "text")
    @Comment("메시지 내용")
    private String content;

    @Column(nullable = false)
    private Instant createdAt;

    public enum Role { USER, ASSISTANT, SYSTEM }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
