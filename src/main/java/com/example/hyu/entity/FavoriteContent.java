package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity @Table(
        name = "favorite_contents",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fav_user_content", columnNames = {"사용자 ID", "콘텐츠 ID"})
)
public class FavoriteContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "찜 ID")
    private Long id;

    @Column(name = "사용자 ID", nullable = false)
    private Long userId;          // FK → user(유저아이디)

    @Column(name = "콘텐츠 ID", nullable = false)
    private Long contentId;       // FK → cms_contents(콘텐츠 ID) 등

    @Column(name = "생성일시", nullable = false, updatable = false)
    private Instant createdAt;
}