package com.example.hyu.controller;

import com.example.hyu.dto.user.*;
import com.example.hyu.security.AuthPrincipal;
import com.example.hyu.service.UserAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
public class UserAssessmentController {

    private final UserAssessmentService service;

    /** (1) 사이드바: 카테고리별 묶음 */
    @GetMapping("/sidebar")
    public Map<String, List<AssessmentListRes>> sidebar() {
        return service.sidebar();
    }

    /** (2) 문항 조회 */
    @GetMapping("/{code}/questions")
    public AssessmentQuestionsRes getQuestions(@PathVariable String code) {
        return service.getQuestions(code);
    }

    /** (3) 제출 + 결과 반환 */
    @PostMapping("/{code}/submit")
    public AssessmentSubmitRes submit(@PathVariable String code,
                                      @RequestBody AssessmentSubmitReq req,
                                      @AuthenticationPrincipal AuthPrincipal me) {
        Long userId = (me != null ? me.id() : null);
        return service.submit(code, req, userId);
    }

    /** (4) 결과 재조회 (옵션) */
    @GetMapping("/{code}/result/{submissionId}")
    public AssessmentSubmitRes getResult(@PathVariable String code,
                                         @PathVariable Long submissionId) {
        return service.getResult(code, submissionId);
    }

    /** 지난 검사 이력 (로그인 유저만) */
    @GetMapping("/{code}/history")
    public Page<AssessmentHistoryItemRes> history(@PathVariable String code,
                                                  @PageableDefault(size = 10, sort = "submittedAt", direction = Sort.Direction.DESC)
                                                  Pageable pageable,
                                                  @AuthenticationPrincipal AuthPrincipal me) {
        if (me == null) {
            // 이력은 본인 소유만 보여줘야 하므로 익명은 막는다
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return service.history(code, me.id(), pageable);
    }

}