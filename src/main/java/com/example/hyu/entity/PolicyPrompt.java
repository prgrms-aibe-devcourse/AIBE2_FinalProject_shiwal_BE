package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity @Table(name = "policy_prompts")
public class PolicyPrompt {

    @Id
    @Column(name = "정책 키", length = 120)
    private String key; // 예: "ai.session.max_length"

    @Lob
    @Column(name = "값")
    private String value; // 프롬프트/문구/임계값 등

    @Column(name = "수정자", nullable = false)
    private Long modifiedBy; // FK → user

    @Column(name = "수정시각", nullable = false)
    private Instant modifiedAt;
}