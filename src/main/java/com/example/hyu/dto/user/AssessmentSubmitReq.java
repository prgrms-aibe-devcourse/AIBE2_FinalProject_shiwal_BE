package com.example.hyu.dto.user;

// 제출 요청
public record AssessmentSubmitReq(java.util.List<Item> answers) {
    public record Item(Long questionId, Integer value) {} // value: 0~3
}

