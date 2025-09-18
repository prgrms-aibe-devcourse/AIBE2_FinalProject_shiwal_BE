package com.example.hyu.dto.community;

// 좋아요 토글 결과 응답 DTO
public record LikeToggleResponse (
        boolean liked,  // 현재 사용자가 좋아요를 누른 상태인지 여부
        int likeCount  // 해당 게시글/댓글의 총 좋아요 수
){}
