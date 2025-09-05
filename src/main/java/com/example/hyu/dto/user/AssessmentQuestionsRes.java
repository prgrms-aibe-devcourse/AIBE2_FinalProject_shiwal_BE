package com.example.hyu.dto.user;

import java.util.List;

// 문항 조회 응답: 척도 정보는 공통으로 한 번만 내려줌
public record AssessmentQuestionsRes(
        Meta assessment,     // 검사 기본정보
        Scale scale,         // 공통 척도 (예: 0~3)
        List<QuestionItem> questions // 문항 리스트
) {
    public record Meta(String code, String name, int questionCount) {}
    public record Scale(int min, int max, List<String> labels) {} // labels는 선택
    public record QuestionItem(Long id, Integer orderNo, String text) {}
}