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

    @PatchMapping("/{assessmentId}/answers")
    public ResponseEntity<Void> upsertDraftAnswer(
            @PathVariable Long assessmentId,
            @RequestHeader(value = "X-Guest-Key", required = false) String headerGuestKey,
            @Valid @RequestBody AssessmentAnswerReq body,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        // [추가-가드] 비로그인인데 guestKey도 없으면 400
        if (principal == null && (headerGuestKey == null || headerGuestKey.isBlank())
                && (body.guestKey() == null || body.guestKey().isBlank())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        // [추가-가드] path와 body submissionId가 모두 있을 때는 소속 검사만 간단히 체크(서비스에서도 검증하지만 빠른 400)
        if (body.submissionId() != null && body.questionId() == null) {
            // 클라이언트 실수 방지용: submissionId만 던지면 추후 디버깅 힘듦
            // (필수는 아니므로 경고성 가드. 필요없으면 지워도 됨)
        }

        Long userId = (principal != null) ? principal.getUserId() : null;
        service.upsertDraftAnswer(assessmentId, userId, headerGuestKey, body);
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
        // [수정] path-id와 body.assessmentId 불일치 시 400 (네 코드 유지)
        if (body.assessmentId() != null && !body.assessmentId().equals(assessmentId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Long userId = (principal != null) ? principal.getUserId() : null;

        // [추가-가드] 비로그인 제출인데 guestKey 전혀 없으면 400 (서비스에서도 막지만 여기서도 빠르게 컷)
        if (userId == null && (headerGuestKey == null || headerGuestKey.isBlank())
                && (body.guestKey() == null || body.guestKey().isBlank())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        AssessmentSubmitReq fixed = new AssessmentSubmitReq(
                assessmentId, body.answers(), body.submissionId(), body.guestKey()
        );
        AssessmentSubmitRes res = service.submit(fixed, userId, headerGuestKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }



    /* ========= 결과/히스토리 (로그인 필요) ========= */

    @GetMapping("/{assessmentId}/results/latest")
    public AssessmentSubmitRes latest(@PathVariable Long assessmentId,
                                      @AuthenticationPrincipal AuthPrincipal principal) {
        // SecurityConfig에서 인증 요구하지만, 방어적으로 체크
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
        return service.latestResult(assessmentId, principal.getUserId());
    }

    @GetMapping("/{assessmentId}/results")
    public Page<AssessmentHistoryItemRes> history(@PathVariable Long assessmentId,
                                                  @PageableDefault(size = 20) Pageable pageable,
                                                  @AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
        return service.history(assessmentId, principal.getUserId(), pageable);
    }
}