package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_likes",
        uniqueConstraints = @UniqueConstraint(name="uk_post_like_user_post", columnNames={"user_id","post_id"}),
        indexes = {
                @Index(name="idx_post_like_post", columnList="post_id"),
                @Index(name="idx_post_like_user", columnList="user_id")
        })
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@AllArgsConstructor(access=AccessLevel.PRIVATE) @Builder
public class CommunityPostLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    private Users user;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="post_id", nullable=false)
    private CommunityPost post;
}