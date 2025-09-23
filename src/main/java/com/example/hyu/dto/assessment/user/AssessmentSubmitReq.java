package com.example.hyu.dto.assessment.user;

import com.fasterxml.jackson.annotation.JsonInclude;

// 제출 요청
@JsonInclude(JsonInclude.Include.ALWAYS)
public record AssessmentSubmitReq(
     Long assessmentId,
     java.util.List<AssessmentAnswerReq> answers,
     Long submissionId,
     @com.fasterxml.jackson.annotation.JsonProperty("guestKey")
     String guestKey
) {}

