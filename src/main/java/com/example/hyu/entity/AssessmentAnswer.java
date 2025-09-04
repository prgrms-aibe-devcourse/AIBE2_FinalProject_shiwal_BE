package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="assessment_answers")
@Getter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AssessmentAnswer {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;  // PK, 응답 고유번호

    @Column(nullable=false)
    private Integer selectedValue;
    // 사용자가 선택한 값 (0~3)

    @Column(length=100)
    private String rawAnswer;
    // 원시 응답 (선택지 라벨 등 저장하고 싶으면 사용, 필수 아님)

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="submission_id", nullable=false)
    private AssessmentSubmission submission;
    // 이 응답이 속한 검사 제출(Submission)

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="question_id", nullable=false)
    private AssessmentQuestion question;
    // 어떤 문항에 대한 응답인지
}