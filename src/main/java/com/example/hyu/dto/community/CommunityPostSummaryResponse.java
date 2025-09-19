package com.example.hyu.dto.community;

import java.time.Instant;
import java.time.LocalDateTime;

// 목록 조회용 DTO
public record CommunityPostSummaryResponse (
        Long id,  // 게시물 ID
        String title,  // 제목
        AuthorSummary author,  // 작성자 요약 (익명 처리 가능)
        boolean isAnonymous, // 익명 여부
        Instant createdAt // 작성일
){
    public static record AuthorSummary(Long id, String nickname) {}
}
