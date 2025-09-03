package com.example.hyu.service;

import com.example.hyu.dto.user.HealingContentDto;
import com.example.hyu.dto.user.HealingContentListResponse;
import com.example.hyu.entity.CmsContent;
import com.example.hyu.entity.CmsContent.Category;
import com.example.hyu.entity.CmsContent.Visibility;
import com.example.hyu.repository.HealingContentRepository;
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

    /**
     * Returns a paginated list of public healing content matching the optional filters.
     *
     * The method enforces size bounds (1..50) and uses cursor-based pagination. If `cursor` is null or blank
     * the first page is returned; otherwise the cursor is decoded and results after that cursor are fetched.
     * The repository is queried for content with Visibility.PUBLIC and publishedAt <= current time.
     *
     * The returned list contains at most `size` items. Internally the method requests `size + 1` items to
     * determine whether there are more pages. If more pages exist, `nextCursor` is provided; it encodes the
     * last item's effective published time (publishedAt if present, otherwise createdAt) and id as a
     * URL-safe Base64 string of "<epochMillis>:<id>".
     *
     * @param cursor   optional pagination cursor (URL-safe Base64 of "<epochMillis>:<id>"); null/blank for first page
     * @param category optional content category filter
     * @param groupKey optional groupKey filter
     * @param q        optional full-text query filter
     * @param size     requested page size (will be clamped to the range 1..50)
     * @return a HealingContentListResponse containing up to `size` items, a `nextCursor` when more pages exist, and a hasMore flag
     */

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

    /**
     * Returns a public, already-published healing content item by its id.
     *
     * Retrieves the CmsContent with the given id only if its visibility is PUBLIC and its
     * publishedAt is either null (treated as immediately published) or not after the current time,
     * then converts it to a HealingContentDto.
     *
     * @param id the CMS content id to fetch
     * @return a DTO representing the found content
     * @throws org.springframework.web.server.ResponseStatusException with status 404 if the content
     *         does not exist, is not PUBLIC, or is not yet published
     */
    @Override
    public HealingContentDto getOne(Long id) {
        CmsContent c = repo.findById(id)
                .filter(it -> it.getVisibility() == Visibility.PUBLIC)
                // publishedAt == null 이면 즉시 공개로 간주
                .filter(it -> it.getPublishedAt() == null || !it.getPublishedAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found or not public: " + id));
        return toDto(c);
    }

    /**
     * Convert a CmsContent entity to a HealingContentDto.
     *
     * The DTO copies id, title, text, thumbnailUrl, mediaType, duration, groupKey,
     * category, and publishedAt directly from the entity. The `publishedAt` value
     * is preserved as-is and may be null.
     *
     * @param c the source CmsContent entity
     * @return a HealingContentDto with fields copied from the entity
     */

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

    /**
     * Returns the timestamp used for ordering/cursor purposes: the content's `publishedAt`
     * when present, otherwise its `createdAt`.
     *
     * @param c the content entity
     * @return the effective publish time to use for sorting or cursor generation
     */
    private Instant effectivePublishedAt(CmsContent c) {
        return (c.getPublishedAt() != null) ? c.getPublishedAt() : c.getCreatedAt();
    }

    /**
     * Encode a cursor as a URL-safe Base64 string in the form "<epochMillis>:<id>".
     *
     * @param publishedAt the instant used for the epoch milliseconds portion of the cursor
     * @param id the identifier appended after the colon
     * @return a URL-safe, padding-free Base64 encoding of "`epochMillis:id`"
     */
    private String encodeCursor(Instant publishedAt, Long id) {
        long millis = publishedAt.toEpochMilli();
        String raw = millis + ":" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes a URL-safe Base64 cursor into a Cursor containing a publish instant and id.
     *
     * The cursor must be the URL-safe Base64 (no padding) encoding of the string
     * "<epochMillis>:<id>" (e.g. "1690000000000:42"). Returns a Cursor whose
     * publishedAt is Instant.ofEpochMilli(epochMillis) and whose id is the parsed id.
     *
     * @param cursor Base64-encoded cursor string
     * @return decoded Cursor with publishedAt and id
     * @throws org.springframework.web.server.ResponseStatusException with HTTP 400 (BAD_REQUEST)
     *         if the cursor is missing, malformed, or cannot be parsed
     */
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