package com.example.hyu.dto.healingContent;

import com.example.hyu.entity.CmsContent.*;
import java.time.Instant;

public record CmsContentResponse(
        Long id,
        Category category,
        String title,
        String text,
        MediaType mediaType,
        Integer duration,
        String thumbnailUrl,
        Visibility visibility,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        Long createdBy,
        Long updatedBy,
        boolean deleted,
        Instant deletedAt,
        Long deletedBy
) {}