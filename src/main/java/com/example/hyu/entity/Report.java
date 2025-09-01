package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity @Table(name = "reports")
public class Report {

    public enum TargetType { POST, COMMENT, CONTENT, USER }
    public enum Reason { SPAM, ABUSE, SUICIDE, VIOLENCE, OTHER }
    public enum Status { PENDING, REVIEWED, DISMISSED, ACTION_TAKEN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "신고 ID")
    private Long id;

    @Column(name = "신고자 ID", nullable = false)
    private Long reporterId; // FK → user

    @Enumerated(EnumType.STRING)
    @Column(name = "신고대상 종류", length = 20, nullable = false)
    private TargetType targetType;

    @Column(name = "대상 ID")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "신고 사유", length = 20, nullable = false)
    private Reason reason;

    @Lob
    @Column(name = "상세 설명")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "처리 상태", length = 20, nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "신고일시", nullable = false, updatable = false)
    private Instant reportedAt;

    @Column(name = "검토일시")
    private Instant reviewedAt;
}