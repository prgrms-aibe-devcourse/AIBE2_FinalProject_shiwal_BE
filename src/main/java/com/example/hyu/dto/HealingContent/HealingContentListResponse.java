package com.example.hyu.dto.HealingContent;

import java.util.List;

public record HealingContentListResponse(
        List<HealingContentDto> items,
        String nextCursor,
        boolean hasMore
) {}
