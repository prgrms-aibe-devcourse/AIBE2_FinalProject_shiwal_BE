package com.example.hyu.dto.community;

// 댓글 생성 요청 DTO
public record CommentCreateRequest (
        String content, // 댓글 내용
        boolean isAnonymous // 익명 여부
){}
