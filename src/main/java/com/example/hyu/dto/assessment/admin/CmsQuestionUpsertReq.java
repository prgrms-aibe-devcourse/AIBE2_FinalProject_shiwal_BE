package com.example.hyu.dto.assessment.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * CMS: 문항 업서트(교체) 요청 DTO
 * - reverseScore: 역문항 여부 (true면 0~3 스코어를 3-값으로 뒤집어 채점)
 */
public record CmsQuestionUpsertReq(
        @NotNull(message = "문항 순서는 필수입니다")
        @Min(value = 1, message = "문항 순서는 1 이상이어야 합니다")
        Integer orderNo,     // 문항 순서

        @NotBlank(message = "질문 문구는 비어있을 수 없습니다")
        String text,         // 질문 문구

        Boolean reverseScore // 역문항 여부
) {}
