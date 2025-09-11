package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_user_email", columnList = "email", unique = true),
                @Index(name = "idx_user_role", columnList = "role"),
                @Index(name = "idx_user_state", columnList = "state"),
                @Index(name = "idx_user_created_at", columnList = "created_at")
        }
)
public class Users extends BaseTimeEntity {

    public enum UserState { ACTIVE, SUSPENDED, WITHDRAWN }
    public enum RiskMode { WARN, EXEMPT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "nickname", length = 100, nullable = false)
    private String nickname;

    @Column(name = "role", length = 50, nullable = false) // USER / ADMIN
    private String role;

    /* === 관리자 기능용 추가 필드 === */

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 20, nullable = false)
    @Builder.Default
    private UserState state = UserState.ACTIVE;

    @Column(name = "suspend_until")
    private Instant suspendUntil;

    @Column(name = "phone", length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_mode", length = 10)
    @Builder.Default
    private RiskMode riskMode = RiskMode.WARN;

    @Column(name = "pwd_reset_token_hash")
    private String passwordResetTokenHash;

    @Column(name = "pwd_reset_expires_at")
    private Instant passwordResetExpiresAt;

    @Builder.Default
    @Column(name = "need_password_reset", nullable = false)
    private boolean needPasswordReset = false;
}