package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity @Table(name = "profile_change_logs")
public class ProfileChangeLog {

    public enum Field { AVATAR, NICKNAME, EMAIL, PASSWORD }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "로그 id")
    private Long id;

    @Column(name = "사용자", nullable = false)
    private Long userId; // FK → user

    @Enumerated(EnumType.STRING)
    @Column(name = "변경 필드", length = 20, nullable = false)
    private Field field;

    @Column(name = "변경 전", length = 255)
    private String beforeValue; // 비번은 저장X

    @Column(name = "변경 후", length = 255)
    private String afterValue; // 비번은 저장X

    @Column(name = "수정일시", nullable = false)
    private Instant modifiedAt;
}