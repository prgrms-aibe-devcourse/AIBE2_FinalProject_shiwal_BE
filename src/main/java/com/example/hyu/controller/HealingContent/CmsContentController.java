package com.example.hyu.controller.HealingContent;

import com.example.hyu.dto.HealingContent.CmsContentRequest;
import com.example.hyu.dto.HealingContent.CmsContentResponse;
import com.example.hyu.entity.CmsContent.Category;
import com.example.hyu.entity.CmsContent.Visibility;
import com.example.hyu.security.AuthPrincipal;
import com.example.hyu.service.HealingContent.admin.CmsContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cms-contents")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class CmsContentController {

    private final CmsContentService service;

    // 생성
    @PostMapping
    public CmsContentResponse create(@Valid @RequestBody CmsContentRequest req,
                                     @AuthenticationPrincipal AuthPrincipal principal) {
        return service.create(req, principal.id()); // 토큰의 userId 사용
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
            @RequestParam(required = false, name = "includeDeleted") Boolean includeDeleted,
            @RequestParam(required = false) String groupKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        String[] s = sort.split(",");
        Sort.Direction dir = (s.length > 1 && "asc".equalsIgnoreCase(s[1]))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, s[0]));
        //  service.search에 groupKey를 전달 (서비스 시그니처도 맞춰둬야 함)
        return service.search(q, category, visibility, includeDeleted, groupKey, pageable);
    }

    // 수정
    @PutMapping("/{id}")
    public CmsContentResponse update(@PathVariable Long id,
                                     @Valid @RequestBody CmsContentRequest req,
                                     @AuthenticationPrincipal AuthPrincipal principal) {
        return service.update(id, req, principal.id());
    }

    // 공개/비공개 토글
    @PatchMapping("/{id}/visibility")
    public void setVisibility(@PathVariable Long id,
                              @RequestParam("value") Visibility value,
                              @AuthenticationPrincipal AuthPrincipal principal) {
        service.toggleVisibility(id, value, principal.id());
    }

    // 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        // principal.id() 가 삭제자 ID
        service.delete(id /*, principal.id()*/); // ← 서비스 시그니처 늘리면 전달
        return ResponseEntity.noContent().build();
    }
}