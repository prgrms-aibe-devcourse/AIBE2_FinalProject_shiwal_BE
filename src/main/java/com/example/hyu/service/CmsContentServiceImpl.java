package com.example.hyu.service;

import com.example.hyu.dto.HealingContent.CmsContentRequest;
import com.example.hyu.dto.HealingContent.CmsContentResponse;
import com.example.hyu.entity.CmsContent;
import com.example.hyu.entity.CmsContent.Visibility;
import com.example.hyu.repository.HealingContent.CmsContentRepository;
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

    Instant now = Instant.now();

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
                c.getUpdatedBy(),
                c.isDeleted(),
                c.getDeletedAt(),
                c.getDeletedBy()
        );
    }

    @Override
    public CmsContentResponse create(CmsContentRequest r, Long adminId) {

        // visibility 기본값: PUBLIC
        Visibility vis = (r.visibility() == null) ? Visibility.PUBLIC : r.visibility();

        // publishedAt 기본값: now (즉시 공개)
        Instant pub = (r.publishedAt() != null) ? r.publishedAt() : now;

        //groupKey 정규화 : 빈문자/공백이면 null로
        String normalizedGroup = (r.groupKey() == null || r.groupKey().isBlank()) ? null : r.groupKey().trim();

        CmsContent c = CmsContent.builder()
                .category(r.category())
                .groupKey(normalizedGroup)
                .title(r.title())
                .text(r.text())
                .mediaType(r.mediaType())
                .duration(r.duration())
                .thumbnailUrl(r.thumbnailUrl())
                .visibility(vis)
                .publishedAt(pub)
                .createdBy(adminId)
                .updatedBy(adminId)
                .build();

        return toDto(repo.save(c));
    }

    @Override
    @Transactional(readOnly = true)
    public CmsContentResponse get(Long id) {
        CmsContent c = repo.findAnyById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found: " + id));
        return toDto(c);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CmsContentResponse> search(String q,
                                           CmsContent.Category category,
                                           Visibility visibility,
                                           Boolean includeDeleted,
                                           String groupKey,
                                           Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        String gk = (groupKey == null || groupKey.isBlank()) ? null : groupKey.trim();

        if(Boolean.TRUE.equals(includeDeleted)){
            //삭제 포함 검색 -> native 쿼리 사용
            return repo.searchIncludingDeleted(
                    query,
                    category != null ? category.name() : null,
                    visibility != null ? visibility.name() : null,
                    gk,
                    true,
                    pageable
            ).map(this::toDto);
        }
        return repo.search(query, category, visibility, gk, pageable).map(this::toDto);
    }

    @Override
    public CmsContentResponse update(Long id, CmsContentRequest r, Long adminId) {
        CmsContent c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found: " + id));

        if (r.category() != null)      c.setCategory(r.category());
        if (r.groupKey() != null)      c.setGroupKey(r.groupKey().isBlank() ? null : r.groupKey().trim());
        if (r.title() != null)         c.setTitle(r.title());
        if (r.text() != null)          c.setText(r.text());
        if (r.mediaType() != null)     c.setMediaType(r.mediaType());
        if (r.duration() != null)      c.setDuration(r.duration());
        if (r.thumbnailUrl() != null)  c.setThumbnailUrl(r.thumbnailUrl());
        if (r.visibility() != null)    c.setVisibility(r.visibility());
        if (r.publishedAt() != null)   c.setPublishedAt(r.publishedAt());

        c.setUpdatedBy(adminId);

        // 공개 전환인데 공개시각이 비어있으면 now로
        if (c.getVisibility() == Visibility.PUBLIC && c.getPublishedAt() == null) {
            c.setPublishedAt(Instant.now());
        }

        return toDto(repo.save(c));
    }

    @Override
    public void toggleVisibility(Long id, Visibility value, Long adminId) {
        CmsContent c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found: " + id));

        c.setVisibility(value);
        c.setUpdatedBy(adminId);

        // PUBLIC 전환 시 공개시각 보정
        if (value == Visibility.PUBLIC && c.getPublishedAt() == null) {
            c.setPublishedAt(now);
        }
        repo.save(c);
    }

    @Override
    public void delete(Long id) {
        CmsContent c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found: " + id));

        if (c.isDeleted()) return; // 멱등

        // ✅ 삭제자 기록 포함 (컨트롤러에서 adminId 받아서 전달)
        c.setDeleted(true);
        c.setDeletedAt(Instant.now());
        repo.save(c);
    }
}