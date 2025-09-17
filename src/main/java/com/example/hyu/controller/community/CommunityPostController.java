package com.example.hyu.controller.community;

import com.example.hyu.dto.PageResponse;
import com.example.hyu.dto.community.CommunityPostCreateRequest;
import com.example.hyu.dto.community.CommunityPostDetailResponse;
import com.example.hyu.dto.community.CommunityPostSummaryResponse;
import com.example.hyu.dto.community.CommunityPostUpdateRequest;
import com.example.hyu.security.AuthPrincipal;
import com.example.hyu.service.community.CommunityPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/community-posts")
@RequiredArgsConstructor
public class CommunityPostController {

    private final CommunityPostService service;

    // 생성
    @PostMapping
    public ResponseEntity<?> create(
            @AuthenticationPrincipal AuthPrincipal me,
            @Valid @RequestBody CommunityPostCreateRequest req
    ) {
        Long id = service.create(me.getUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of("postId", id));
    }

    // 단건 조회
    @GetMapping("/{id}")
    public CommunityPostDetailResponse getOne(@PathVariable Long id) {
        return service.getOne(id);
    }

    // 목록 조회
    @GetMapping
    public PageResponse<CommunityPostSummaryResponse> getList(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "authorId", required = false) Long authorId
    ) {
        return service.getList(pageable, q, authorId);
    }

    // 수정
    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal me,
            Authentication auth,
            @Valid @RequestBody CommunityPostUpdateRequest req
    ) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        service.update(id, me.getUserId(), isAdmin, req);
        return ResponseEntity.noContent().build();
    }

    //삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal me,
            Authentication auth
    ) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        service.delete(id, me.getUserId(), isAdmin);
        return ResponseEntity.noContent().build();
    }

}
