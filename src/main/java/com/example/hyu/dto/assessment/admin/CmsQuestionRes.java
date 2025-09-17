package com.example.hyu.dto.assessment.admin;

/**
 * CMS: 문항 조회 응답 DTO
 */
public record CmsQuestionRes(
        Long id,
        Long assessmentId, //검사 id
        Integer orderNo, //검사 순번
        String text, // 질문 텍스트
        boolean reverseScore // 역문항 여부
) {}
