package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "assessment_submissions")
public class AssessmentSubmission {

    public enum RiskLevel { LOW, MEDIUM, HIGH }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "제출 ID")
    private Long id;

    @Column(name = "총점")
    private Integer totalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "위험도", length = 10)
    @Builder.Default
    private RiskLevel risk = RiskLevel.LOW;

    @Column(name = "제출 시각", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "유저아이디", nullable = false)
    private Long userId; // FK → User

    @Column(name = "검사 ID", nullable = false)
    private Long assessmentId; // FK → Assessment
}