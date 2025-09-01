package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "assessment_options")
public class AssessmentOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "선택지 ID")
    private Long id;

    @Column(name = "라벨", length = 100)
    private String label;

    @Column(name = "점수값")
    private Integer score;

    @Column(name = "순서")
    private Integer orderNo;

    @Column(name = "문항 ID", nullable = false)
    private Long questionId; // FK → AssessmentQuestion
}