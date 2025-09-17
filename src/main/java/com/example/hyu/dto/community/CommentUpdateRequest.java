package com.example.hyu.dto.community;

// 댓글 수정 요청 DTO
public record CommentUpdateRequest (
        String content, //새 댓글 내용
        Boolean isAnonymous // 익명 여부 변경
){}
