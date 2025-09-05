package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name="assessments")
@Getter
@Setter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Assessment {

    // 검사 상태 → 활성/비활성 여부 관리
    public enum Status { ACTIVE, ARCHIVED }

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;  // PK, 검사 고유번호

    @Column(nullable = false, unique = true, length=50)
    private String code;  // 내부 식별 코드 (예: PHQ9, GAD7)

    @Column(nullable = false, length=100)
    private String name;  // 검사 이름 (예: 우울감 검사)

    @Column(nullable = false, length=50)
    private String category;  // 검사 카테고리 (예: 감정&기분, 집중&습관)

    @Lob
    private String description;  // 검사 설명 (간단 소개/사용 목적)

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=10)
    @Builder.Default
    private Status status = Status.ACTIVE;  // 상태 (기본 ACTIVE)

    @Column(nullable=false, updatable=false)
    private Instant createdAt;  // 생성일시

    private Instant updatedAt;  // 수정일시

    @PrePersist
    void pre(){ this.createdAt = Instant.now(); }  // 저장될 때 생성일시 자동 입력
    @PreUpdate
    void upd(){ this.updatedAt = Instant.now(); }  // 수정될 때 수정일시 자동 갱신
}