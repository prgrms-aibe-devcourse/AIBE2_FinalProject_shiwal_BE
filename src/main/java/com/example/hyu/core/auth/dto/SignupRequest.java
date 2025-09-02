package com.example.hyu.core.auth.dto;

import jakarta.validation.constraints.*;

public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min=8, max=64) String password,
        @NotBlank @Size(min=2, max=20)
        @Pattern(
                regexp="^[a-zA-Z0-9가-힣_\\-]+$",
                message="닉네임은 한글/영문/숫자/_/-만 허용"
        )
        String nickname
) {}