package com.example.hyu.service;

import com.example.hyu.dto.admin.CmsContentRequest;
import com.example.hyu.dto.admin.CmsContentResponse;
import com.example.hyu.entity.CmsContent;
import com.example.hyu.entity.CmsContent.Visibility;
import com.example.hyu.repository.CmsContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class CmsContentServiceImpl implements CmsContentService {

    private final CmsContentRepository repo;

    /**
     * Convert a CmsContent entity to a CmsContentResponse DTO.
     *
     * @param c the source CmsContent entity
     * @return a CmsContentResponse containing the entity's visible fields (id, category, title, text,
     *         mediaType, duration, thumbnailUrl, visibility, publishedAt, createdAt, updatedAt,
     *         createdBy, updatedBy)
     */
    private CmsContentResponse toDto(CmsContent c) {
        return new CmsContentResponse(
                c.getId(),
                c.getCategory(),
                c.getTitle(),
                c.getText(),
                c.getMediaType(),
                c.getDuration(),
                c.getThumbnailUrl(),
                c.getVisibility(),
                c.getPublishedAt(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getCreatedBy(),
                c.getUpdatedBy()
        );
    }

    /**
     * Creates and persists a new CmsContent from the given request and returns its DTO.
     *
     * The method requires a non-blank groupKey in the request, applies defaults (visibility → PUBLIC when absent;
     * publishedAt → now when absent), and sets created/updated metadata using the provided adminId.
     *
     * @param r request containing content fields; must include a non-blank `groupKey`
     * @param adminId identifier of the admin performing the creation; used for createdBy/updatedBy
     * @return the saved CmsContent converted to a CmsContentResponse
     * @throws org.springframework.web.server.ResponseStatusException if `groupKey` is null or blank (HTTP 400)
     */
    @Override
    public CmsContentResponse create(CmsContentRequest r, Long adminId) {
        // groupKey 필수
        if (r.groupKey() == null || r.groupKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "groupKey is required");
        }

        Instant now = Instant.now();

        // visibility 기본값: PUBLIC
        Visibility vis = (r.visibility() == null) ? Visibility.PUBLIC : r.visibility();

        // publishedAt 기본값: now (즉시 공개)
        Instant pub = (r.publishedAt() != null) ? r.publishedAt() : now;

        CmsContent c = CmsContent.builder()
                .category(r.category())
                .groupKey(r.groupKey())
                .title(r.title())
                .text(r.text())
                .mediaType(r.mediaType())
                .duration(r.duration())
                .thumbnailUrl(r.thumbnailUrl())
                .visibility(vis)
                .publishedAt(pub)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(adminId)
                .updatedBy(adminId)
                .build();

        return toDto(repo.save(c));
    }

    /**
     * Retrieve a CmsContent by its id and return it as a CmsContentResponse.
     *
     * @param id the content entity id
     * @return the content converted to a CmsContentResponse
     * @throws org.springframework.web.server.ResponseStatusException with status 404 if no content exists for the given id
     */
    @Override
    @Transactional(readOnly = true)
    public CmsContentResponse get(Long id) {
        CmsContent c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found: " + id));
        return toDto(c);
    }

    /**
     * Searches CMS content using optional filters and returns a page of DTOs.
     *
     * The method treats empty or blank `q` and `groupKey` values as null (no filter).
     *
     * @param q          full-text query string; blank/empty values are ignored
     * @param category   content category filter (nullable)
     * @param visibility visibility filter (nullable)
     * @param groupKey   group key filter; blank/empty values are ignored
     * @param pageable   pagination and sorting information
     * @return a page of CmsContentResponse matching the provided filters
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CmsContentResponse> search(String q,
                                           CmsContent.Category category,
                                           Visibility visibility,
                                           String groupKey,
                                           Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        String gk = (groupKey == null || groupKey.isBlank()) ? null : groupKey.trim();
        return repo.search(query, category, visibility, gk, pageable).map(this::toDto);
    }

    /**
     * Partially updates an existing CmsContent and returns the saved representation.
     *
     * Only fields present (non-null) in the request are applied. If `groupKey` is provided it must not be blank.
     * Always updates the entity's `updatedAt` and `updatedBy` metadata. If the resulting visibility is
     * PUBLIC and `publishedAt` is not set, `publishedAt` will be set to now.
     *
     * @param id the ID of the content to update
     * @param r the request containing fields to update; fields that are null are left unchanged
     * @param adminId the identifier of the administrator performing the update (stored in `updatedBy`)
     * @return the updated CmsContent mapped to CmsContentResponse
     * @throws org.springframework.web.server.ResponseStatusException with HttpStatus.NOT_FOUND if no content exists with the given id
     * @throws org.springframework.web.server.ResponseStatusException with HttpStatus.BAD_REQUEST if a provided `groupKey` is blank
     */
    @Override
    public CmsContentResponse update(Long id, CmsContentRequest r, Long adminId) {
        CmsContent c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found: " + id));

        if (r.category() != null)      c.setCategory(r.category());
        if (r.groupKey() != null) {
            if (r.groupKey().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "groupKey cannot be blank");
            }
            c.setGroupKey(r.groupKey());
        }
        if (r.title() != null)         c.setTitle(r.title());
        if (r.text() != null)          c.setText(r.text());
        if (r.mediaType() != null)     c.setMediaType(r.mediaType());
        if (r.duration() != null)      c.setDuration(r.duration());
        if (r.thumbnailUrl() != null)  c.setThumbnailUrl(r.thumbnailUrl());
        if (r.visibility() != null)    c.setVisibility(r.visibility());
        if (r.publishedAt() != null)   c.setPublishedAt(r.publishedAt());

        c.setUpdatedAt(Instant.now());
        c.setUpdatedBy(adminId);

        // 공개 전환인데 공개시각이 비어있으면 now로
        if (c.getVisibility() == Visibility.PUBLIC && c.getPublishedAt() == null) {
            c.setPublishedAt(Instant.now());
        }

        return toDto(repo.save(c));
    }

    /**
     * Sets the visibility of a CmsContent identified by id and persists the change.
     *
     * If changing to PUBLIC and the content has no publishedAt timestamp, publishedAt is set to the current time.
     * Also updates updatedAt and updatedBy for auditing and saves the entity.
     *
     * @param id the CmsContent primary key
     * @param value the new visibility value to apply
     * @param adminId identifier of the admin performing the change (stored in updatedBy)
     * @throws org.springframework.web.server.ResponseStatusException with HTTP 404 when no content exists for the given id
     */
    @Override
    public void toggleVisibility(Long id, Visibility value, Long adminId) {
        CmsContent c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found: " + id));

        Instant now = Instant.now();
        c.setVisibility(value);

        // PUBLIC 전환 시 공개시각 보정
        if (value == Visibility.PUBLIC && c.getPublishedAt() == null) {
            c.setPublishedAt(now);
        }

        c.setUpdatedAt(now);
        c.setUpdatedBy(adminId);

        repo.save(c);
    }

    /**
     * Performs a soft delete of the CmsContent with the given id.
     *
     * Marks the entity as deleted by calling {@code markDeleted(null)} and persists the change.
     *
     * @param id the id of the CmsContent to soft-delete
     * @throws org.springframework.web.server.ResponseStatusException with HTTP 404 if no entity exists for the given id
     */
    @Override
    public void delete(Long id) {
        // 소프트 삭제: 엔티티 로딩 → 마킹 → 저장
        CmsContent c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found: " + id));
        // 삭제자 기록을 남기고 싶으면 컨트롤러에서 adminId 받아서 markDeleted(adminId)로 넘겨.
        c.markDeleted(null);
        repo.save(c);
    }
}