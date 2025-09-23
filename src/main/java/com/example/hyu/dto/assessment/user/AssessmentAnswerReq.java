package com.example.hyu.dto.assessment.user;

import jakarta.validation.constraints.NotBlank;

public record AssessmentAnswerReq(
        Long questionId,
        Integer value,
        String rawAnswer,
        Long submissionId,
        @NotBlank(message = "guestKey must not be blank")
        String guestKey
) {}