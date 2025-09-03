package com.example.hyu.service;

import com.example.hyu.dto.user.HealingContentDto;
import com.example.hyu.dto.user.HealingContentListResponse;
import com.example.hyu.entity.CmsContent;

public interface HealingContentService {
    /**
                                        * Retrieves a page of healing content for an infinite-scroll feed using cursor-based pagination.
                                        *
                                        * This method returns a list of content filtered and/or searched according to the provided criteria.
                                        *
                                        * @param cursor    opaque cursor from a previous response; pass null or empty to fetch the first page
                                        * @param category  content category to filter by; may be null to include all categories
                                        * @param groupKey  optional group identifier to further restrict results; may be null
                                        * @param q         optional full-text search query; may be null or empty to disable searching
                                        * @param size      maximum number of items to return in this page
                                        * @return          a HealingContentListResponse containing the page of items and pagination cursor(s)
                                        */
    HealingContentListResponse getFeed(String cursor,
                                       CmsContent.Category category,
                                       String groupKey,
                                       String q,
                                       int size);

    /**
 * Retrieve a single healing content item by its identifier.
 *
 * Only content with PUBLIC visibility and a publishedAt timestamp less than or equal to the current time are considered eligible.
 *
 * @param id the identifier of the content to retrieve
 * @return the content as a {@code HealingContentDto}, or {@code null} if no eligible content exists for the given id
 */
    HealingContentDto getOne(Long id);
}