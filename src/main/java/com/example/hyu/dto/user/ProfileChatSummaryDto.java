package com.example.hyu.dto.user;

import java.time.Instant;
import java.util.UUID;

public record ProfileChatSummaryDto(
        UUID sessionId,
        String lastRole,
        String lastContent,
        Instant lastAt
) {}