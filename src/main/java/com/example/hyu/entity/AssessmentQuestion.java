package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="assessment_questions",
        uniqueConstraints=@UniqueConstraint(name="uq_assessment_order", columnNames={"assessment_id","order_no"}))
@Getter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AssessmentQuestion {

    // 문항 유형 → 여기서는 SCALE(0~3 점수)만 사용
    public enum QuestionType { SCALE }

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;  // PK, 문항 고유번호

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="assessment_id", nullable=false)
    private Assessment assessment;  // 어떤 검사(Assessment)에 속한 문항인지

    @Column(name="order_no", nullable=false)
    private Integer orderNo;  // 문항 순서 번호 (1,2,3…)

    @Lob
    @Column(nullable=false)
    private String text;  // 질문 문구

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    @Builder.Default
    private QuestionType type = QuestionType.SCALE;  // 문항 유형 (SCALE 고정)

    @Column(nullable=false)
    @Builder.Default
    private boolean reverseScore = false;
    // 역문항 여부 → true면 점수 계산 시 (3 - value)로 뒤집음
}