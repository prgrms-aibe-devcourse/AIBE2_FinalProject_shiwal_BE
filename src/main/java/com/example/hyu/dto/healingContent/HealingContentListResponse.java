package com.example.hyu.dto.healingContent;

import java.util.List;

public record HealingContentListResponse(
        List<HealingContentDto> items,
        String nextCursor,
        boolean hasMore
) {}
