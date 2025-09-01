package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "user",
        indexes = {
                @Index(name = "idx_user_email", columnList = "이메일", unique = true)
        }
)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "유저아이디")
    private Long id;

    @Column(name = "비밀번호", length = 255)
    private String password;

    @Column(name = "이메일", length = 255)
    private String email;

    @Column(name = "이름", length = 255)
    private String name;

    @Column(name = "닉네임", length = 100)
    private String nickname;

    @Column(name = "역할", length = 50)
    private String role; // USER / ADMIN

    // 원본 스키마의 '생성일'은 기본값 CURRENT_TIMESTAMP 의미였음 → Auditing으로 대체됨.
}