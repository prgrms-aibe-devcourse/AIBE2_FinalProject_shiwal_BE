package com.example.hyu.dto.admin;

import com.example.hyu.entity.CmsContent.*;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record CmsContentRequest(
        Category category,     // MUSIC / MEDITATION / MOOD_BOOST
        String title,
        String text,           // LOB
        MediaType mediaType,   // AUDIO / VIDEO / TEXT / LINK
        Integer duration,
        String thumbnailUrl,
        Visibility visibility,  // PUBLIC / PRIVATE
        @NotBlank String groupKey,
        Instant publishedAt
) {}