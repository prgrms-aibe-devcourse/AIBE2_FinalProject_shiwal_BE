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

    @Override
    @Transactional(readOnly = true)
    public CmsContentResponse get(Long id) {
        CmsContent c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found: " + id));
        return toDto(c);
    }

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