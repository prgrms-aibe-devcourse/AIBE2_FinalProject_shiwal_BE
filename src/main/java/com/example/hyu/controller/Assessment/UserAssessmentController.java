package com.example.hyu.controller.Assessment;

import com.example.hyu.dto.Assessment.user.*;
import com.example.hyu.security.AuthPrincipal;
import com.example.hyu.service.Assessment.UserAssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
@Validated
public class UserAssessmentController {

    private final UserAssessmentService service;

    /* ========= 목록/상세 ========= */

    // 활성 검사 목록
    @GetMapping
    public Page<AssessmentRes> listActive(@PageableDefault(size = 20) Pageable pageable) {
        return service.listActive(pageable);
    }

    // 코드로 단건 조회
    @GetMapping("/by-code/{code}")
    public AssessmentRes getByCode(@PathVariable String code) {
        return service.getActiveByCode(code);
    }

    // 문항 조회
    @GetMapping("/{assessmentId}/questions")
    public List<AssessmentQuestionsRes> getQuestions(@PathVariable Long assessmentId) {
        return service.getQuestions(assessmentId);
    }

    /* ========= 답변 임시저장(업서트) ========= */

    //
    @PatchMapping("/{assessmentId}/answers")
    public ResponseEntity<Void> upsertDraftAnswer(
            @PathVariable Long assessmentId,
            @RequestHeader(value = "X-Guest-Key", required = false) String headerGuestKey,
            @Valid @RequestBody AssessmentAnswerReq body,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long userId = (principal != null) ? principal.id() : null;
        service.upsertDraftAnswer(assessmentId, userId, headerGuestKey, body); // ← 헤더 값 그대로 전달
        return ResponseEntity.noContent().build();
    }



    /* ========= 최종 제출 ========= */

    @PostMapping("/{assessmentId}/submit")
    public ResponseEntity<AssessmentSubmitRes> submit(
            @PathVariable Long assessmentId,
            @RequestHeader(value = "X-Guest-Key", required = false) String headerGuestKey,
            @Valid @RequestBody AssessmentSubmitReq body,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        if (body.assessmentId() != null && !body.assessmentId().equals(assessmentId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Long userId = (principal != null) ? principal.id() : null;

        // path 기준으로 id만 보정해서 전달
        AssessmentSubmitReq fixed = new AssessmentSubmitReq(
                assessmentId, body.answers(), body.submissionId(), body.guestKey()
        );
        AssessmentSubmitRes res = service.submit(fixed, userId, headerGuestKey); // ← 헤더 값 그대로 전달
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }



    /* ========= 결과/히스토리 (로그인 필요) ========= */

    @GetMapping("/{assessmentId}/results/latest")
    public AssessmentSubmitRes latest(@PathVariable Long assessmentId,
                                      @AuthenticationPrincipal AuthPrincipal principal) {
        // SecurityConfig에서 인증 요구하지만, 방어적으로 체크
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
        return service.latestResult(assessmentId, principal.id());
    }

    @GetMapping("/{assessmentId}/results")
    public Page<AssessmentHistoryItemRes> history(@PathVariable Long assessmentId,
                                                  @PageableDefault(size = 20) Pageable pageable,
                                                  @AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
        return service.history(assessmentId, principal.id(), pageable);
    }
}