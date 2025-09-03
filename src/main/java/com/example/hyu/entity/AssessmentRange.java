package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name="assessment_ranges")
@Getter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AssessmentRange {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;  // PK, 구간 고유번호

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="assessment_id", nullable=false)
    private Assessment assessment;
    // 어떤 검사에 속한 구간인지

    @Column(nullable=false)
    private Integer minScore;
    // 구간 시작 점수 (포함)

    @Column(nullable=false)
    private Integer maxScore;
    // 구간 끝 점수 (포함)

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private AssessmentSubmission.RiskLevel level;
    // 위험도 단계 (MILD/MODERATE/RISK/HIGH_RISK)

    @Column(nullable=false, length=50)
    private String labelKo;
    // 결과 레벨 이름 (예: "약함")

    @Column(nullable=false, length=200)
    private String summaryKo;
    // 짧은 설명 (예: "우울감이 낮습니다.")

    @Lob
    @Column(nullable=false)
    private String adviceKo;
    // 권고 문구 (예: "규칙적인 생활을 유지하세요.")
}
