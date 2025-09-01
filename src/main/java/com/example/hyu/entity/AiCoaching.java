package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity @Table(name = "ai_coaching")
public class AiCoaching {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "코칭아이디")
    private Long id;

    @Column(name = "유저아이디", nullable = false)
    private Long userId;

    @Column(name = "일기아이디", nullable = false)
    private Long diaryId;

    @Lob
    @Column(name = "입력 프롬프트")
    private String prompt;

    @Lob
    @Column(name = "AI 답변")
    private String response;

    @Column(name = "생성일", nullable = false, updatable = false)
    private Instant createdAt;
}