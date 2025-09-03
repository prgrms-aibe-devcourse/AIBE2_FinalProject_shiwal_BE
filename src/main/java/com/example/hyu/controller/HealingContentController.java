package com.example.hyu.controller;

import com.example.hyu.dto.user.HealingContentDto;
import com.example.hyu.dto.user.HealingContentListResponse;
import com.example.hyu.entity.CmsContent;
import com.example.hyu.service.HealingContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class HealingContentController {

    private final HealingContentService service;

    /**
     * Normalizes a string by trimming whitespace and converting empty or null inputs to {@code null}.
     *
     * @param s the input string (may be {@code null})
     * @return the trimmed string, or {@code null} if the input was {@code null} or empty after trimming
     */
    private static String norm(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * Returns a paginated list of healing contents filtered by the provided query parameters.
     *
     * The method normalizes empty string inputs for `groupKey` and `q` to `null`, and clamps
     * `size` to the range [1, 50] before delegating to the service layer.
     *
     * @param cursor   opaque pagination cursor; pass null or omit to fetch the first page
     * @param category optional content category to filter results
     * @param groupKey optional group key; blank values are treated as null
     * @param q        optional full-text query; blank values are treated as null
     * @param size     page size (default 12); values less than 1 are set to 1 and values greater than 50 are set to 50
     * @return a HealingContentListResponse containing the requested page of content items and pagination metadata
     */
    @GetMapping
    public HealingContentListResponse feed(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) CmsContent.Category category,
            @RequestParam(required = false) String groupKey,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "12") int size
    ) {
        // ✅ 빈 문자열을 null로 치환 (groupKey, q 둘 다)
        groupKey = norm(groupKey);
        q = norm(q);

        // (옵션) size 가드
        if (size < 1) size = 1;
        if (size > 50) size = 50;

        return service.getFeed(cursor, category, groupKey, q, size);
    }

    /**
     * Retrieve a single content item by its ID.
     *
     * @param id the content ID
     * @return the content as a {@link HealingContentDto}
     */
    @GetMapping("/{id}")
    public HealingContentDto one(@PathVariable Long id) {
        return service.getOne(id);
    }
}