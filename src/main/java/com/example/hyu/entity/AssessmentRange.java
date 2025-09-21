package com.example.hyu.entity;

import com.example.hyu.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="assessment_ranges")
@Getter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@org.hibernate.annotations.Check(constraints = "min_score <= max_score")
public class AssessmentRange { //사용자가 제출한 점수에 따라 사용자에게 보여줄 것을 나타내는 엔티티

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;  // PK, 구간 고유번호

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="assessment_id", nullable=false)
    private Assessment assessment;
    // 어떤 검사에 속한 구간인지

    @Setter
    @Column(nullable=false)
    private Integer minScore;
    // 구간 시작 (총점)점수 (포함)

    @Setter
    @Column(nullable=false)
    private Integer maxScore;
    // 구간 끝 (총점)점수 (포함)

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private RiskLevel level;
    // 위험도 단계 (MILD/MODERATE/RISK/HIGH_RISK)

    @Setter
    @Column(nullable=false, length=50)
    private String labelKo;
    // 사용자에게 보여줄 결과 레벨 이름 (예: "약함")

    @Setter
    @Lob
    @Column(nullable=false, length=200)
    private String summaryKo;
    // 설명

    @Setter
    @Column(nullable=false)
    private String adviceKo;
    // 권고 문구
}
