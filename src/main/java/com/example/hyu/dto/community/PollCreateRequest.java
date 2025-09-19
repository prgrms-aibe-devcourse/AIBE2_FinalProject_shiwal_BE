package com.example.hyu.dto.community;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * 투표 생성 요청
 * - type == BINARY 인 경우: options는 비워두면 서비스에서 ["예","아니오"] 자동 생성
 * - type == SINGLE 인 경우: options 필수(2개 이상 권장)
 * - deadline 이 null이면 서비스에서 +24h로 기본값 적용
 */
public record PollCreateRequest(
        @NotNull PollType type,
        @NotBlank @Size(max = 255) String question,
        List<@NotBlank @Size(max = 200) String> options, // SINGLE일 때만 사용
        @FutureOrPresent Instant deadline // null 허용 → 서비스에서 resolve
) {
    /** 투표 유형: BINARY(예/아니오), SINGLE(단일선택) */
    public enum PollType { BINARY, SINGLE }
}