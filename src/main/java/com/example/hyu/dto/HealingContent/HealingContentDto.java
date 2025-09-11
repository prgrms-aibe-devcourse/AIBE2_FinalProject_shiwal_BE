package com.example.hyu.dto.HealingContent;

import com.example.hyu.entity.CmsContent;

import java.time.Instant;

public record HealingContentDto(
        Long id,
        String title,
        String text,
        String thumbnailUrl,
        CmsContent.MediaType mediaType, // ← enum
        Integer duration,
        String groupKey,
        CmsContent.Category category,   // ← enum
        Instant publishedAt
) {}