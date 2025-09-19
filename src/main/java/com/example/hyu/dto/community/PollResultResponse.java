package com.example.hyu.dto.community;

import java.time.Instant;
import java.util.List;

/**
 * 투표 결과 응답
 * - 옵션별 득표수 + 전체 대비 비율(%) 포함
 * - 소수점 자리수는 프론트에서 포맷팅 권장(백엔드도 반올림 가능)
 */
public record PollResultResponse(
        Long pollId,
        String question,
        Instant deadline,
        long totalVotes,              // 총 투표 수(= 모든 옵션 votes 합)
        List<OptionResult> options
) {
    public record OptionResult(
            Long optionId,
            String content,
            long votes,
            double percentage         // (votes / totalVotes) * 100.0 (total=0이면 0.0)
    ) {}
}
