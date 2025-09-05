package com.example.hyu.dto.user;

import java.util.List;

// 제출 요청
public record AssessmentSubmitReq(
        List<Item> answers   // {questionId, value(0~3)}
) {
    public record Item(Long questionId, Integer value) {}
}

