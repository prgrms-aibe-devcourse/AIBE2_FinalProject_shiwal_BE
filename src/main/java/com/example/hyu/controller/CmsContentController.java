package com.example.hyu.controller;

import com.example.hyu.dto.admin.*;
import com.example.hyu.entity.CmsContent.Category;
import com.example.hyu.entity.CmsContent.Visibility;
import com.example.hyu.service.CmsContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

// 🔹 테스트 단계에선 PreAuthorize 잠시 막자 (로그인/권한 미구현이면 401/403 나옴)
// @PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/cms-contents")
@RequiredArgsConstructor
@Validated
public class CmsContentController {

    private final CmsContentService service;

    // 생성
    @PostMapping
    public CmsContentResponse create(@Valid @RequestBody CmsContentRequest req,
                                     @RequestHeader("X-ADMIN-ID") Long adminId) {
        return service.create(req, adminId);
    }

    // 단건 조회
    @GetMapping("/{id}")
    public CmsContentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    // 목록/검색
    @GetMapping
    public Page<CmsContentResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Visibility visibility,
            @RequestParam(required = false) String groupKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        String[] s = sort.split(",");
        Sort.Direction dir = (s.length > 1 && "asc".equalsIgnoreCase(s[1]))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, s[0]));
        return service.search(q, category, visibility, groupKey, pageable);
    }

    // 수정
    @PutMapping("/{id}")
    public CmsContentResponse update(@PathVariable Long id,
                                     @Valid @RequestBody CmsContentRequest req,
                                     @RequestHeader("X-ADMIN-ID") Long adminId) {
        return service.update(id, req, adminId);
    }

    // 공개/비공개 토글 (value 파라미터 명시)
    @PatchMapping("/{id}/visibility")
    public void setVisibility(@PathVariable Long id,
                              @RequestParam("value") Visibility value,
                              @RequestHeader("X-ADMIN-ID") Long adminId) {
        service.toggleVisibility(id, value, adminId);
    }

    // 삭제
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}