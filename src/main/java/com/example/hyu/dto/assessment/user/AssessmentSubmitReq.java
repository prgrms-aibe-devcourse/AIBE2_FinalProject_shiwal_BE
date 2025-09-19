package com.example.hyu.dto.assessment.user;

// 제출 요청
public record AssessmentSubmitReq(
     Long assessmentId,
     java.util.List<AssessmentAnswerReq> answers,
     Long submissionId,
     String guestKey
) {}

