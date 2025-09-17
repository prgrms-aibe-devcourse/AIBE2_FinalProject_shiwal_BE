package com.example.hyu.controller.community;


import com.example.hyu.dto.community.CommentCreateRequest;
import com.example.hyu.dto.community.CommentResponse;
import com.example.hyu.dto.community.CommentUpdateRequest;
import com.example.hyu.security.AuthPrincipal;
import com.example.hyu.service.community.CommunityCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community-posts/{postId}/comments")
@RequiredArgsConstructor
public class CommunityCommentController {

    private final CommunityCommentService service;

    // 댓글 생성 (인증 필요)
    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long postId,
                                    @AuthenticationPrincipal AuthPrincipal me,
                                    @RequestBody @Valid CommentCreateRequest req){
        Long id = service.create(me.getUserId(), postId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("commentId", id));
    }

    // 댓글 목록 조회
    @GetMapping
    public List<CommentResponse> list(@PathVariable Long postId) {
        return service.getList(postId);
    }

    // 댓글 수정 (작성자 or 관ㄹ지ㅏ)
    @PatchMapping("/{commentId}")
    public ResponseEntity<Void> update(@PathVariable Long postId,
                                       @PathVariable Long commentId,
                                       @AuthenticationPrincipal AuthPrincipal me,
                                       Authentication auth,
                                       @RequestBody @Valid CommentUpdateRequest req) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        service.update(commentId, me.getUserId(), isAdmin, req);
        return ResponseEntity.noContent().build();
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long postId,
                                       @PathVariable Long commentId,
                                       @AuthenticationPrincipal AuthPrincipal me,
                                       Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        service.delete(commentId, me.getUserId(), isAdmin);
        return ResponseEntity.noContent().build();
    }













}
