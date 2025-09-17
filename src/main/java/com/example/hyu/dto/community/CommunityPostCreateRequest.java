package com.example.hyu.dto.community;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 게시글 생성 요청 dto
public record CommunityPostCreateRequest (
        @NotBlank(message = "제목은 필수입니다")
        @Size(min = 1 ,max = 150)
        String title,

        @NotBlank(message = "글 내용은 필수입니다")
        String content,

        // 익명 글 여부
        boolean isAnonymous
){}
