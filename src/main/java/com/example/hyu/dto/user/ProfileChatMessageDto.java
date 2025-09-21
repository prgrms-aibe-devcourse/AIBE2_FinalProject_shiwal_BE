package com.example.hyu.dto.user;

import java.time.Instant;
import java.util.UUID;

public record ProfileChatMessageDto(
        Long messageId,
        UUID sessionId,
        String role,       // USER / ASSISTANT / SYSTEM
        String content,
        Instant createdAt
) {}