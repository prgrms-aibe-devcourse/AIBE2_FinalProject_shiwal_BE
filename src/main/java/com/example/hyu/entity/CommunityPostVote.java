package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "community_post_votes")
public class CommunityPostVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "항목 id")
    private Long id;

    @Column(name = "게시글 ID", nullable = false)
    private Long postId; // FK → CommunityPost

    @Column(name = "항목라벨", length = 60)
    private String label;

    @Column(name = "득표수", nullable = false)
    @Builder.Default
    private Integer voteCount = 0;
}