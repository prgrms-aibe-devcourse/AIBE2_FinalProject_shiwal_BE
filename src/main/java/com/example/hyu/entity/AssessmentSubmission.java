package com.example.hyu.entity;

import com.example.hyu.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "assessment_submissions",
        indexes = {
                // 로그인 사용자용 드래프트/히스토리 조회
                @Index(name = "ix_subm_assessment_user_status", columnList = "assessment_id, user_id, status"),
                // 게스트용 드래프트 조회
                @Index(name = "ix_subm_assessment_guest_status", columnList = "assessment_id, guest_key, status"),
                // 제출 목록 정렬/필터 보조
                @Index(name = "ix_subm_status", columnList = "status"),
                @Index(name = "ix_subm_submitted_at", columnList = "submitted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AssessmentSubmission extends BaseTimeEntity { // 제출/결과

    // 제출 상태
    public enum Status { DRAFT, SUBMITTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // PK

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Status status = Status.DRAFT;

    // 총점 / 위험도 / 제출시각
    private Integer totalScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RiskLevel risk;

    @Column(updatable = false)
    private Instant submittedAt;

    // --- 소유자 식별자(둘 중 하나) ---
    // 로그인 사용자 식별자 (게스트일 땐 null)
    @Column(name = "user_id")
    private Long userId;

    // 게스트 식별 키 (로그인 사용자일 땐 null)
    @Column(name = "guest_key", length = 64)
    private String guestKey;

    // 어떤 검사에 대한 제출인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    // 한 제출에 속한 문항별 응답들
    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AssessmentAnswer> answers = new ArrayList<>();

    // 동시 수정 충돌 방지
    @Version
    private Long version;

    /* ====== 편의 메서드 / 비즈니스 규칙 ====== */

    public boolean isSubmitted() {
        return this.status == Status.SUBMITTED;
    }

    // 초안에서만 답변 upsert 허용
    public void upsertAnswer(AssessmentAnswer answer, int value, String raw) {
        if (isSubmitted()) {
            throw new IllegalStateException("이미 제출된 검사입니다. 답변을 수정할 수 없습니다");
        }
        var existing = answers.stream()
                .filter(a -> a.getQuestion().getId().equals(answer.getQuestion().getId()))
                .findFirst();
        if (existing.isPresent()) {
            existing.get().revise(value, raw);
        } else {
            answer.revise(value, raw);
            answer.attachTo(this);
        }
    }

    // 제출 확정
    public void submitWithResult(int total, RiskLevel riskLevel) {
        if (isSubmitted()) return;
        this.totalScore = total;
        this.risk = riskLevel;
        this.submittedAt = Instant.now();
        this.status = Status.SUBMITTED;
    }

    public void applyResult(int total, RiskLevel risk) {
        this.totalScore = total;
        this.risk = risk;
    }
}