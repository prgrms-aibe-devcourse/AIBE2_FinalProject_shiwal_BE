package com.example.hyu.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserSignupRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 255) String password,
        @NotBlank String name,
        @NotBlank String nickname
) {}
