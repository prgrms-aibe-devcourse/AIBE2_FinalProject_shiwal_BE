package com.example.hyu.dto.admin;

/**
 * CMS: 문항 업서트(교체) 요청 DTO
 * - reverseScore: 역문항 여부 (true면 0~3 스코어를 3-값으로 뒤집어 채점)
 */
public record CmsQuestionUpsertReq(
        Integer orderNo,     // 문항 순서 (1부터)
        String text,         // 질문 문구
        Boolean reverseScore // 역문항 여부
) {}
