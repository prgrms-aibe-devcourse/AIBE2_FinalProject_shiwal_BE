package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "community_comments")
public class CommunityComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "댓글 id")
    private Long id;

    @Column(name = "게시글 ID", nullable = false)
    private Long postId; // FK → CommunityPost

    @Column(name = "작성자 ID", nullable = false)
    private Long authorId; // FK → User

    @Column(name = "부모 댓글 ID")
    private Long parentCommentId; // self FK

    @Lob
    @Column(name = "내용", nullable = false)
    private String content;

    @Column(name = "생성일시", nullable = false, updatable = false)
    private Instant createdAt;
}