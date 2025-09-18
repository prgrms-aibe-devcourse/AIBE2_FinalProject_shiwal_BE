package com.example.hyu.dto.community;

import jakarta.validation.constraints.NotNull;

/**
 * 투표 참여 요청
 * - 단일 선택만 지원하므로 optionId 하나만 받음
 */
public record PollVoteRequest(
        @NotNull Long optionId
) {}
