package com.example.hyu.dto.Assessment.user;

public record AssessmentAnswerReq (
    Long questionId,
    Integer value, //사용자가 선택한 값(0~3)
    String rawAnswer, //선택지 라벨
    Long submissionId,
    String guestKey
){}
