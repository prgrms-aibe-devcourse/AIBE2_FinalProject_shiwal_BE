package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity @Table(name = "user_logins")
public class UserLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "로그 ID")
    private Long id;

    @Column(name = "로그인 시각")
    private Instant loggedAt;

    @Column(name = "접속 ip", length = 45)
    private String ip;

    @Column(name = "Field", length = 255)
    private String field; // 원본 스키마 그대로 존치(예비 필드)

    @Column(name = "유저아이디", nullable = false)
    private Long userId; // FK → user(유저아이디)
}