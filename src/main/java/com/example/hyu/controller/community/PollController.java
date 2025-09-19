package com.example.hyu.controller.community;

import com.example.hyu.dto.community.PollCreateRequest;
import com.example.hyu.dto.community.PollDetailResponse;
import com.example.hyu.dto.community.PollResultResponse;
import com.example.hyu.dto.community.PollVoteRequest;
import com.example.hyu.security.AuthPrincipal;

import com.example.hyu.service.community.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // ★ 스프링 시큐리티 Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    /* =========================================================
       1) 투표 생성 (게시글당 1개 제한)
       - URL: POST /api/community-posts/{postId}/polls
       - 인증 필요 (작성자 또는 권한 정책은 서비스/시큐리티로)
       ========================================================= */
    @PostMapping("/api/community-posts/{postId}/polls")
    public ResponseEntity<?> createPoll(@PathVariable Long postId,
                                        @AuthenticationPrincipal AuthPrincipal me,
                                        Authentication auth,
                                        @Valid @RequestBody PollCreateRequest req) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        Long pollId = pollService.create(postId, req, me.getUserId(), isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("pollId", pollId));
    }

    /* =========================================================
       2) 게시글에 달린 투표 조회 (단건) — 게시글 상세에 붙이기 용
       - URL: GET /api/community-posts/{postId}/polls
       - 공개(permitAll) 가능
       - 없으면 404
       ========================================================= */
    @GetMapping("/api/community-posts/{postId}/polls")
    public ResponseEntity<PollDetailResponse> getPollByPost(@PathVariable Long postId) {
        return pollService.getByPostId(postId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /* =========================================================
       3) 투표 단건 상세
       - URL: GET /api/polls/{pollId}
       - 공개(permitAll) 가능
       ========================================================= */
    @GetMapping("/api/polls/{pollId}")
    public PollDetailResponse getPoll(@PathVariable Long pollId) {
        return pollService.getDetail(pollId);
    }

    /* =========================================================
       4) 투표하기 (단일 선택)
       - URL: POST /api/polls/{pollId}/vote
       - 인증 필요
       ========================================================= */
    @PostMapping("/api/polls/{pollId}/vote")
    public ResponseEntity<Void> vote(@PathVariable Long pollId,
                                     @AuthenticationPrincipal AuthPrincipal me,
                                     @Valid @RequestBody PollVoteRequest req) {
        pollService.vote(me.getUserId(), pollId, req);
        return ResponseEntity.noContent().build();
    }

    /* =========================================================
       5) 투표 결과 (집계 + 비율)
       - URL: GET /api/polls/{pollId}/results
       - 공개(permitAll) 가능
       - “누가” 투표했는지는 포함하지 않음(옵션별 집계만)
       ========================================================= */
    @GetMapping("/api/polls/{pollId}/results")
    public PollResultResponse getResults(@PathVariable Long pollId) {
        return pollService.getResults(pollId);
    }

    /* =========================================================
       6) 투표 삭제 (Soft delete)
       - URL: DELETE /api/polls/{pollId}
       - 인증 필요 (게시글 작성자 or ADMIN)
       - isAdmin 판별은 Authentication으로 ROLE 확인
       ========================================================= */
    @DeleteMapping("/api/polls/{pollId}")
    public ResponseEntity<Void> delete(@PathVariable Long pollId,
                                       @AuthenticationPrincipal AuthPrincipal me,
                                       Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        pollService.delete(pollId, me.getUserId(), isAdmin);
        return ResponseEntity.noContent().build();
    }
}