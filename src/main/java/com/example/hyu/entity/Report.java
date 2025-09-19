// src/main/java/com/example/hyu/entity/Report.java
package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
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

    // 신고자
    @Column(name="reporter_id", nullable=false)
    private Long reporterId; // FK -> users.id (조인 없이 id만 사용해도 됨)

    // 신고 대상 유형
    @Enumerated(EnumType.STRING)
    @Column(name="target_type", length=20, nullable=false)
    private TargetType targetType;

    @Column(name="target_id", nullable=false)
    private Long targetId;

    // 신고 사유
    @Enumerated(EnumType.STRING)
    @Column(name="reason", length=20, nullable=false)
    private Reason reason;

    // 상세 설명
    @Lob
    @Column(name="description")
    private String description;

    //  첨부 1건만 URL로. 여러 개면 별도 테이블 권장.
    @Column(name="attachment_url")
    private String attachmentUrl;

    // 상태
    @Enumerated(EnumType.STRING)
    @Column(name="status", length=20, nullable=false)
    @Builder.Default
    private Status status = Status.PENDING;

    // 신고 시각
    @Column(name="reported_at", nullable=false, updatable=false)
    private Instant reportedAt;

    // 마지막 검토 시각
    @Column(name="last_reviewed_at")
    private Instant lastReviewedAt;

    // 관리자 메모
    @Column(name="admin_note", length=1000)
    private String adminNote;

    // 조치한 관리자 ID
    @Column(name = "reviewed_by")
    private Long handledByAdminId;

    // 편의 메서드

    // 검토 처리
    public void markReviewed(Long adminId, String note) {
        this.status = Status.REVIEWED;
        this.lastReviewedAt = Instant.now();
        this.handledByAdminId = adminId;
        if (note != null && !note.isBlank()) {
            this.adminNote = note.trim();
        }
    }

    // 조치 완료 처리
    public void markActionTaken(Long adminId, String note) {
        this.status = Status.ACTION_TAKEN;
        this.lastReviewedAt = Instant.now();
        this.handledByAdminId = adminId;
        if (note != null && !note.isBlank()) {
            this.adminNote = note.trim();
        }
    }
}