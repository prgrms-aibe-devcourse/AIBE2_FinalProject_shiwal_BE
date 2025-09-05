package com.example.hyu.controller;

import com.example.hyu.dto.admin.*;
import com.example.hyu.security.AuthPrincipal; // 선택: 주입해서 감사로그 등에 사용
import com.example.hyu.service.CmsAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/assessments")   // ✅ 관리자 전용 프리픽스
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")          // ✅ ADMIN만 접근
public class CmsAssessmentController {

    private final CmsAssessmentService service;

    /* ===== 평가(Assessment) ===== */

    /** 생성 (C) */
    @PostMapping
    public Long create(@RequestBody CmsAssessmentUpsertReq req,
                       @AuthenticationPrincipal AuthPrincipal principal) {
        // 필요시 principal.id()로 감사로그/작성자 기록
        return service.createAssessment(req);
    }

    /** 수정 (U) */
    @PutMapping("/{id}")
    public void update(@PathVariable Long id,
                       @RequestBody CmsAssessmentUpsertReq req,
                       @AuthenticationPrincipal AuthPrincipal principal) {
        service.updateAssessment(id, req);
    }

    /** 목록 조회 (R-list) */
    @GetMapping
    public Page<CmsAssessmentRes> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.listAssessments(pageable);
    }

    /** 단건 조회 (R-one) */
    @GetMapping("/{id}")
    public CmsAssessmentRes getOne(@PathVariable Long id) {
        return service.getOne(id);
    }

    /** 소프트 삭제 (D-soft: 상태를 ARCHIVED로) */
    @DeleteMapping("/{id}")
    public void softDelete(@PathVariable Long id,
                           @AuthenticationPrincipal AuthPrincipal principal) {
        service.softDelete(id);
    }

    /* ===== 문항(Question) ===== */

    /** 문항 교체 (전체 업서트) */
    @PostMapping("/{id}/questions:replace")
    public void replaceQuestions(@PathVariable Long id,
                                 @RequestBody List<CmsQuestionUpsertReq> body,
                                 @AuthenticationPrincipal AuthPrincipal principal) {
        service.replaceQuestions(id, body);
    }

    /** 문항 조회 */
    @GetMapping("/{id}/questions")
    public List<CmsQuestionRes> getQuestions(@PathVariable Long id) {
        return service.getQuestions(id);
    }

    /* ===== 점수 구간표(Range) ===== */

    /** 구간표 교체 (전체 업서트) */
    @PostMapping("/{id}/ranges:replace")
    public void replaceRanges(@PathVariable Long id,
                              @RequestBody List<CmsRangeUpsertReq> body,
                              @AuthenticationPrincipal AuthPrincipal principal) {
        service.replaceRanges(id, body);
    }

    /** 구간표 조회 */
    @GetMapping("/{id}/ranges")
    public List<CmsRangeRes> getRanges(@PathVariable Long id) {
        return service.getRanges(id);
    }
}
