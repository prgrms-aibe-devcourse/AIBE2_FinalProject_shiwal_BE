package com.example.hyu.dto.user;

// 문항 조회
public record AssessmentQuestionRes(Long id, Integer orderNo, String text, Integer scaleMin, Integer scaleMax) {}
