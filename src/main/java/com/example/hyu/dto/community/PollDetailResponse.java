package com.example.hyu.dto.community;

import java.time.Instant;
import java.util.List;

/**
 * 투표 상세 응답
 * - 결과 노출 전/후 공통으로 사용할 수 있는 구조
 * - "누가" 투표했는지는 절대 포함하지 않음
 */
public record PollDetailResponse(
        Long pollId,
        String type,          // "BINARY" | "SINGLE"
        String question,
        Instant deadline,
        List<OptionItem> options
) {
    public record OptionItem(
            Long optionId,
            String content
    ) {}
}
