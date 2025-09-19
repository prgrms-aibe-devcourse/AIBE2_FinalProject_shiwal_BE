package com.example.hyu.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
        @NotBlank String content
) {}