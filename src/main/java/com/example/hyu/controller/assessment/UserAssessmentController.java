package com.example.hyu.controller.assessment;

import com.example.hyu.dto.assessment.user.*;
import com.example.hyu.security.AuthPrincipal;
import com.example.hyu.service.assessment.UserAssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @GetMapping
    public Page<AssessmentRes> listActive(@PageableDefault(size = 20) Pageable pageable) {
        return service.listActive(pageable);
    }

    @GetMapping("/by-code/{code}")
    public AssessmentRes getByCode(@PathVariable String code) {
        return service.getActiveByCode(code);
    }

    @GetMapping("/{assessmentId}/questions")
    public List<AssessmentQuestionsRes> getQuestions(@PathVariable Long assessmentId) {
        return service.getQuestions(assessmentId);
    }

    /* ========= 답변 임시저장(업서트) ========= */
    @PatchMapping("/{assessmentId}/answers")
    public ResponseEntity<Void> upsertDraftAnswer(
            @PathVariable Long assessmentId,
            @RequestHeader(value = "X-Guest-Key", required = false) String headerGuestKey,
            @Valid @RequestBody AssessmentAnswerReq body,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        // [추가-가드] 비로그인 + guestKey 전혀 없으면 400
        if (principal == null
                && (headerGuestKey == null || headerGuestKey.isBlank())
                && (body.guestKey() == null || body.guestKey().isBlank())) {
            return ResponseEntity.badRequest().build();
        }

        Long userId = (principal != null) ? principal.getUserId() : null;
        service.upsertDraftAnswer(assessmentId, userId, headerGuestKey, body);
        return ResponseEntity.noContent().build();
    }

    /* ========= 최종 제출 ========= */

    @PostMapping(
            value = "/{assessmentId}/submit",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AssessmentSubmitRes> submit(
            @PathVariable Long assessmentId,
            @RequestHeader(value = "X-Guest-Key", required = false) String headerGuestKey,
            @Valid @RequestBody AssessmentSubmitReq body,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        if (body.assessmentId() != null && !body.assessmentId().equals(assessmentId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Long userId = (principal != null) ? principal.getUserId() : null;
        boolean isGuest = (userId == null);
        boolean noHeaderKey = (headerGuestKey == null || headerGuestKey.isBlank());
        boolean noBodyKey = (body.guestKey() == null || body.guestKey().isBlank());
        if (isGuest && noHeaderKey && noBodyKey) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        AssessmentSubmitReq fixed = new AssessmentSubmitReq(
                assessmentId, body.answers(), body.submissionId(), body.guestKey()
        );
        AssessmentSubmitRes res = service.submit(fixed, userId, headerGuestKey);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(res);
    }


    /* ========= 결과/히스토리 (로그인 필요) ========= */

    @GetMapping("/{assessmentId}/results/latest")
    public AssessmentSubmitRes latest(@PathVariable Long assessmentId,
                                      @AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null)
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
        return service.latestResult(assessmentId, principal.getUserId());
    }

    @GetMapping("/{assessmentId}/results")
    public Page<AssessmentHistoryItemRes> history(@PathVariable Long assessmentId,
                                                  @PageableDefault(size = 20) Pageable pageable,
                                                  @AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null)
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
        return service.history(assessmentId, principal.getUserId(), pageable);
    }
}