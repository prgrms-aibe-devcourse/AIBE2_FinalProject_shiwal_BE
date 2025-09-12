// src/main/java/com/example/hyu/entity/Report.java
package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity @Table(name = "reports",
        indexes = {
                @Index(name="idx_reports_status", columnList = "status"),
                @Index(name="idx_reports_reason", columnList = "reason"),
                @Index(name="idx_reports_target", columnList = "target_type, target_id"),
                @Index(name="idx_reports_reported_at", columnList = "reported_at")
        }
)
public class Report {

    public enum TargetType { POST, COMMENT, CONTENT, USER }
    public enum Reason { SPAM, ABUSE, SUICIDE, VIOLENCE, OTHER }
    public enum Status { PENDING, REVIEWED, ACTION_TAKEN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="reporter_id", nullable=false)
    private Long reporterId; // FK -> users.id (조인 없이 id만 사용해도 됨)

    @Enumerated(EnumType.STRING)
    @Column(name="target_type", length=20, nullable=false)
    private TargetType targetType;

    @Column(name="target_id", nullable=false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name="reason", length=20, nullable=false)
    private Reason reason;

    @Lob
    @Column(name="description")
    private String description;

    // (선택) 첨부 1건만 URL로. 여러 개면 별도 테이블 권장.
    @Column(name="attachment_url")
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name="status", length=20, nullable=false)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name="reported_at", nullable=false, updatable=false)
    private Instant reportedAt;

    @Column(name="last_reviewed_at")
    private Instant lastReviewedAt;

    // 관리자 메모
    @Column(name="admin_note", length=1000)
    private String adminNote;
}