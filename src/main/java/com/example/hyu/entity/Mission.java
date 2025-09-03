package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity @Table(
        name = "missions",
        uniqueConstraints = @UniqueConstraint(name = "uk_missions_code", columnNames = {"코드"})
)
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "미션 ID")
    private Long id;

    @Column(name = "코드", length = 50, nullable = false)
    private String code;                 // 예: DAILY_CHECKIN

    @Column(name = "제목", length = 100, nullable = false)
    private String title;

    @Column(name = "보상", nullable = false)
    @Builder.Default
    private Integer reward = 0;

    @Column(name = "생성일시", nullable = false, updatable = false)
    private Instant createdAt;
}