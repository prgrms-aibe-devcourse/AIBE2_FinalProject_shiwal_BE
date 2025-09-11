package com.example.hyu.service;

import com.example.hyu.dto.HealingContent.HealingContentDto;
import com.example.hyu.dto.HealingContent.HealingContentListResponse;
import com.example.hyu.entity.CmsContent;

public interface HealingContentService {
    // 무한스크롤 피드
    HealingContentListResponse getFeed(String cursor,
                                       CmsContent.Category category,
                                       String groupKey,
                                       String q,
                                       int size);

    // 단건 (PUBLIC + publishedAt ≤ now 만 허용)
    HealingContentDto getOne(Long id);
}