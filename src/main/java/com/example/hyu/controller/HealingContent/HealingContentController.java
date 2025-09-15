package com.example.hyu.controller.HealingContent;

import com.example.hyu.dto.HealingContent.HealingContentDto;
import com.example.hyu.dto.HealingContent.HealingContentListResponse;
import com.example.hyu.entity.CmsContent;
import com.example.hyu.service.HealingContent.user.HealingContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class HealingContentController {

    private final HealingContentService service;

    // 공백/빈문자 → null 정규화 유틸
    private static String norm(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

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

    @GetMapping("/{id}")
    public HealingContentDto one(@PathVariable Long id) {
        return service.getOne(id);
    }
}