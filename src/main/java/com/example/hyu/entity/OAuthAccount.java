package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity @Table(name = "oauth_accounts")
public class OAuthAccount {

    @EmbeddedId
    private OAuthAccountId id;

    @Column(name = "제공자", length = 32, nullable = false)
    private String provider; // google, kakao, naver

    @Column(name = "제공자사용자아이디", length = 255, nullable = false)
    private String providerUserId; // UNIQUE(provider, provider_user_id) 의도

    @Column(name = "제공자이메일", length = 255)
    private String providerEmail;

    @Column(name = "제공자이름", length = 255)
    private String providerName;

    @Column(name = "연결시각", nullable = false)
    private java.time.Instant linkedAt;

    @Lob
    @Column(name = "액세스토큰(암호화)")
    private String accessTokenEncrypted;

    @Lob
    @Column(name = "리프레시토큰(암호화)")
    private String refreshTokenEncrypted;

    @Embeddable
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OAuthAccountId implements java.io.Serializable {
        @Column(name = "매핑아이디", nullable = false)
        private Integer mappingId;

        @Column(name = "유저아이디2", nullable = false)
        private Integer userId2; // FK → user(유저아이디)
    }
}