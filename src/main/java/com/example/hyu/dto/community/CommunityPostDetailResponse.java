package com.example.hyu.dto.community;

import java.time.Instant;


// 게시글 단건 응답 DTO
public record CommunityPostDetailResponse(
        Long id,                      // 게시물 ID
        String title,                  // 제목
        String content,                // 본문 내용
        AuthorSummary author,          // 작성자 요약 (익명 처리 가능)
        boolean isAnonymous,           // 익명 여부
        int viewCount,                 // 조회수
        int likeCount,                 // 좋아요 수
        Instant createdAt,       // 작성일
        Instant updatedAt        // 수정일
) {
    public static record AuthorSummary(Long id, String nickname) {}
}