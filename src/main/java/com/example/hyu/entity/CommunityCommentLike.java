package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "comment_likes",
        uniqueConstraints = @UniqueConstraint(name="uk_comment_like_user_comment", columnNames={"user_id","comment_id"}),
        indexes = {
                @Index(name="idx_comment_like_comment", columnList="comment_id"),
                @Index(name="idx_comment_like_user", columnList="user_id")
        })
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@AllArgsConstructor(access=AccessLevel.PRIVATE) @Builder
public class CommunityCommentLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    private Users user;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="comment_id", nullable=false)
    private CommunityComment comment;
}
