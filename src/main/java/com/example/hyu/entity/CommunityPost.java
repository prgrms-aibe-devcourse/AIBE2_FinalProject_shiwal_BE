package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "community_posts")
public class CommunityPost {

    public enum PostType { NORMAL, VOTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "게시글 ID")
    private Long id;

    @Column(name = "작성자", nullable = false)
    private Long authorId; // FK → User

    @Enumerated(EnumType.STRING)
    @Column(name = "글 타입", length = 10, nullable = false)
    @Builder.Default
    private PostType type = PostType.NORMAL;

    @Column(name = "제목", length = 120)
    private String title;

    @Lob
    @Column(name = "내용")
    private String content;

    @Column(name = "익명 여부", nullable = false)
    @Builder.Default
    private boolean anonymous = false;

    @Column(name = "조회수", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "공감수", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "댓글수", nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    @Column(name = "생성일시", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "수정일시")
    private Instant updatedAt;
}