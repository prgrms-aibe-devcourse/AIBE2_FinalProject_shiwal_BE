package com.example.hyu.dto.community;

import jakarta.validation.constraints.Size;

// 게시글 수정 요청 dto
public record CommunityPostUpdateRequest (
        @Size(min = 1, max = 150)
        String title,

        String content,

        Boolean isAnonymous
){}
