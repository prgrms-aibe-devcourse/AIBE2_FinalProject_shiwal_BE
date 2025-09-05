package com.example.hyu.dto.user;

// 사이드바/목록
public record AssessmentListRes(
        String code,      // 예: "PHQ9"
        String name,      // 예: "우울감 검사"
        String category   // 예: "감정&기분"
) {}