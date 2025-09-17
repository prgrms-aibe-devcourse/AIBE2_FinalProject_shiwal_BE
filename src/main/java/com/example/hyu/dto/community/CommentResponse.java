package com.example.hyu.dto.community;

import java.time.Instant;

// 댓글 응답 DTO
public record CommentResponse (
        Long id, // 댓글 id
        Long authorId, // 작성자 ID (익명이면 null)
        String authorNickname, // 작성자 닉네임 (익명이면 "익명")
        String content, // 댓글 본문
        boolean isAnonymous, // 익명 여부
        Instant createdAt, // 생성 시각
        Instant updatedAt // 수정 시각
){}

