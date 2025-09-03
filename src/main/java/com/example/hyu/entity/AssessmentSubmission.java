package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name="assessment_submissions",
        indexes=@Index(name="ix_assessment_user_date", columnList="assessment_id, user_id, submitted_at"))
@Getter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AssessmentSubmission {

    // 위험도 단계 → 총점 범위에 따라 결정됨
    public enum RiskLevel { MILD, MODERATE, RISK, HIGH_RISK }

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;  // PK, 제출 고유번호

    @Column(nullable=false)
    private Integer totalScore;
    // 사용자가 제출한 검사에서 계산된 총점

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    @Builder.Default
    private RiskLevel risk = RiskLevel.MILD;
    // 위험도 레벨 (AssessmentRange로 매핑 후 저장)

    @Column(nullable=false, updatable=false)
    private Instant submittedAt;
    // 제출 시각

    @Column(name="user_id", nullable=false)
    private Long userId;
    // 응답한 사용자 식별자 (User 엔티티 없으면 Long으로 유지)

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="assessment_id", nullable=false)
    private Assessment assessment;
    // 어떤 검사(Assessment)에 대한 제출인지

    @PrePersist
    void pre(){ if(this.submittedAt==null) this.submittedAt = Instant.now(); }
    // 저장 시 제출시각 자동 입력
}