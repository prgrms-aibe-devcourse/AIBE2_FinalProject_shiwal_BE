package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "assessments")
public class Assessment {

    public enum Status { ACTIVE, ARCHIVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "검사 ID")
    private Long id;

    @Column(name = "검사 이름", length = 30)
    private String name;

    @Lob
    @Column(name = "설명")
    private String description;

    @Lob
    @Column(name = "채점 규칙")
    private String scoringRule; // JSON string

    @Lob
    @Column(name = "위험 레벨 규칙")
    private String riskRule; // JSON string

    @Enumerated(EnumType.STRING)
    @Column(name = "상태", length = 10)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(name = "생성일시", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "수정일시")
    private Instant updatedAt;
}