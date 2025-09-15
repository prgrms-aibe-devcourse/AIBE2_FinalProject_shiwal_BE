package com.example.hyu.dto.Assessment.user;

// 문항 조회 응답: 척도 정보는 공통으로 한 번만 내려줌
public record AssessmentQuestionsRes(
       Long id,
       Integer orderNo,
       String text
) { }