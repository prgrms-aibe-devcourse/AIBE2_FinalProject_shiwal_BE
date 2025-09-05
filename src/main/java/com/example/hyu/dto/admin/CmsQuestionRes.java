package com.example.hyu.dto.admin;

/**
 * CMS: 문항 조회 응답 DTO
 */
public record CmsQuestionRes(
        Long id,
        Integer orderNo,
        String text,
        boolean reverseScore
) {}
