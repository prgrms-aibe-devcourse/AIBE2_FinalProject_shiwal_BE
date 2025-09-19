package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity @Table(name = "user_logins")
public class UserLogin {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "로그 ID")
    private Long id;

    @Column(name = "로그인 시각", nullable = false)
    private Instant loggedAt;

    @Column(name = "접속 ip", length = 45)
    private String ip;

    @Column(name = "Field", length = 255)
    private String field;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "유저아이디", nullable = false) // FK → users.id
    private User user;
}