package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "assessment_answers")
public class AssessmentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "응답 ID")
    private Long id;

    @Column(name = "점수")
    private Integer score;

    @Column(name = "원시응답", length = 100)
    private String rawAnswer;

    @Column(name = "제출 ID", nullable = false)
    private Long submissionId; // FK → AssessmentSubmission

    @Column(name = "문항 ID", nullable = false)
    private Long questionId; // FK → AssessmentQuestion

    @Column(name = "선택지 ID", nullable = false)
    private Long optionId; // FK → AssessmentOption
}