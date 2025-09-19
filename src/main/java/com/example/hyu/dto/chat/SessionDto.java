package com.example.hyu.dto.chat;

import java.time.Instant;
import java.util.UUID;

public record SessionDto(
        UUID id,
        String status,   // OPEN / CLOSED
        Instant updatedAt
) {}
