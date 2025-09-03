package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "community_post_likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_post_like_user", columnNames = {"게시글 ID", "사용자 ID"})
        }
)
public class CommunityPostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "공감id")
    private Long id;

    @Column(name = "게시글 ID", nullable = false)
    private Long postId; // FK → CommunityPost

    @Column(name = "사용자 ID", nullable = false)
    private Long userId; // FK → User

    @Column(name = "생성일시", nullable = false, updatable = false)
    private Instant createdAt;
}