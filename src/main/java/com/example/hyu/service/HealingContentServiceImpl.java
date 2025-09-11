package com.example.hyu.service;

import com.example.hyu.dto.HealingContent.HealingContentDto;
import com.example.hyu.dto.HealingContent.HealingContentListResponse;
import com.example.hyu.entity.CmsContent;
import com.example.hyu.entity.CmsContent.Category;
import com.example.hyu.entity.CmsContent.Visibility;
import com.example.hyu.repository.HealingContent.HealingContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealingContentServiceImpl implements HealingContentService {

    private final HealingContentRepository repo;

    // ---------- Public APIs ----------

    @Override
    public HealingContentListResponse getFeed(String cursor,
                                              Category category,
                                              String groupKey,
                                              String q,
                                              int size) {
        // size 가드
        if (size < 1) size = 1;
        if (size > 50) size = 50;

        final Visibility vis = Visibility.PUBLIC;
        final Instant now = Instant.now();
        final Pageable pageable = PageRequest.of(0, size + 1); // hasMore 판단용 +1

        List<CmsContent> rows;

        if (cursor == null || cursor.isBlank()) {
            // 첫 페이지
            rows = repo.fetchFirstPage(vis, now, category, groupKey, q, pageable);
        } else {
            // 커서 해독 후 다음 페이지
            Cursor decoded = decodeCursor(cursor);
            rows = repo.fetchAfterCursor(vis, now, category, groupKey, q,
                    decoded.publishedAt(), decoded.id(), pageable);
        }

        // page slicing
        boolean hasMore = rows.size() > size;
        List<CmsContent> pageItems = hasMore ? rows.subList(0, size) : rows;

        String nextCursor = null;
        if (hasMore) {
            CmsContent last = pageItems.get(pageItems.size() - 1);
            nextCursor = encodeCursor(effectivePublishedAt(last), last.getId());
        }

        return new HealingContentListResponse(
                pageItems.stream().map(this::toDto).toList(),
                nextCursor,
                hasMore
        );
    }

    @Override
    public HealingContentDto getOne(Long id) {
        CmsContent c = repo.findById(id)
                .filter(it -> it.getVisibility() == Visibility.PUBLIC)
                // publishedAt == null 이면 즉시 공개로 간주
                .filter(it -> it.getPublishedAt() == null || !it.getPublishedAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found or not public: " + id));
        return toDto(c);
    }

    // ---------- Helpers ----------

    private HealingContentDto toDto(CmsContent c) {
        // publishedAt은 실제 값 그대로(null 가능)
        return new HealingContentDto(
                c.getId(),
                c.getTitle(),
                c.getText(),
                c.getThumbnailUrl(),
                c.getMediaType(),
                c.getDuration(),
                c.getGroupKey(),
                c.getCategory(),
                c.getPublishedAt()
        );
    }

    /** publishedAt이 NULL이면 createdAt을 반환(정렬/커서 기준 통일) */
    private Instant effectivePublishedAt(CmsContent c) {
        return (c.getPublishedAt() != null) ? c.getPublishedAt() : c.getCreatedAt();
    }

    /** 커서 인코딩: "<epochMilli>:<id>" → base64 */
    private String encodeCursor(Instant publishedAt, Long id) {
        long millis = publishedAt.toEpochMilli();
        String raw = millis + ":" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** 커서 디코딩 */
    private Cursor decodeCursor(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.lastIndexOf(':');
            if (sep < 0) throw new IllegalArgumentException("bad cursor");
            long millis = Long.parseLong(raw.substring(0, sep));
            long id = Long.parseLong(raw.substring(sep + 1));
            return new Cursor(Instant.ofEpochMilli(millis), id);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
    }

    private record Cursor(Instant publishedAt, Long id) {}
}