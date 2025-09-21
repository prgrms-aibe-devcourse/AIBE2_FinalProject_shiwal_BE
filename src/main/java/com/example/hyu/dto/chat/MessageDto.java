package com.example.hyu.dto.chat;

import java.time.Instant;

public record MessageDto(
        Long id,
        String role,     // USER / ASSISTANT / SYSTEM
        String content,
        Instant createdAt
) {}