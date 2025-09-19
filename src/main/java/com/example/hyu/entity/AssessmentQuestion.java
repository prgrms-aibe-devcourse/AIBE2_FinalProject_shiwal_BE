package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "assessment_questions")
public class AssessmentQuestion {

    public enum QuestionType { SINGLE_CHOICE, MULTI_CHOICE, SCALE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "문항 ID")
    private Long id;

    @Column(name = "순서")
    private Integer orderNo;

    @Lob
    @Column(name = "질문 문구")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "유형", length = 20)
    private QuestionType type;

    @Column(name = "검사 ID", nullable = false)
    private Long assessmentId; // FK → Assessment
}