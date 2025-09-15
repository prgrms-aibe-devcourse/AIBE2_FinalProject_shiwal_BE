package com.example.hyu.controller.Assessment;

import com.example.hyu.dto.Assessment.admin.*;
import com.example.hyu.security.AuthPrincipal;
import com.example.hyu.service.Assessment.CmsAssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/assessments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CmsAssessmentController {

    private final CmsAssessmentService service;

    /* =========================
       검사(Assessment)
       ========================= */

    /** 생성 (삭제 포함 코드중복 검사 반영됨) */
    @PostMapping
    public ResponseEntity<Long> create(@Valid @RequestBody CmsAssessmentUpsertReq req,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        Long id = service.createAssessment(req);
        // Location 헤더로 생성 리소스 경로 제공
        return ResponseEntity.created(URI.create("/api/admin/assessments/" + id)).body(id);
    }

    /** 수정 (코드 변경 시에도 삭제 포함 중복검사 반영) */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody CmsAssessmentUpsertReq req,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        service.updateAssessment(id, req);
        return ResponseEntity.noContent().build();
    }

    /** 목록 (삭제 제외: 기본 @Where 적용) */
    @GetMapping
    public Page<CmsAssessmentRes> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.listAssessments(pageable);
    }

    /** 목록 (삭제 포함: 관리자 확인용) */
    @GetMapping("/all")
    public Page<CmsAssessmentRes> adminList(@PageableDefault(size = 20) Pageable pageable) {
        return service.adminListIncludingDeleted(pageable);
    }

    /** 단건 (삭제 제외) */
    @GetMapping("/{id}")
    public CmsAssessmentRes getOne(@PathVariable Long id) {
        return service.getOne(id);
    }

    /** 단건 (삭제 포함: 관리자 확인/복구 판단용) */
    @GetMapping("/{id}/any")
    public CmsAssessmentRes adminGetOne(@PathVariable Long id) {
        return service.adminGetOneIncludingDeleted(id);
    }

    /** 소프트 삭제(+ ARCHIVED 전환) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable Long id,
                                           @AuthenticationPrincipal AuthPrincipal principal) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /** 복구(코드로만 가능, UI 버튼 없어도 됨) */
    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long id,
                                        @AuthenticationPrincipal AuthPrincipal principal) {
        service.restore(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /* =========================
       문항(Question)
       ========================= */

    /** 문항 전체 교체 (벌크 삭제 + 재삽입) */
    @PostMapping("/{id}/questions/replace")
    public ResponseEntity<Void> replaceQuestions(@PathVariable Long id,
                                                 @Valid @RequestBody List<@Valid CmsQuestionUpsertReq> body,
                                                 @AuthenticationPrincipal AuthPrincipal principal) {
        service.replaceQuestions(id, body);
        return ResponseEntity.noContent().build();
    }

    /** 문항 조회 */
    @GetMapping("/{id}/questions")
    public List<CmsQuestionRes> getQuestions(@PathVariable Long id) {
        return service.getQuestions(id);
    }

    /* =========================
       점수 구간표(Range)
       ========================= */

    /** 구간표 전체 교체 (겹침/연속성 검증은 서비스에서 수행) */
    @PostMapping("/{id}/ranges/replace")
    public ResponseEntity<Void> replaceRanges(@PathVariable Long id,
                                              @Valid @RequestBody List<@Valid CmsRangeUpsertReq> body,
                                              @AuthenticationPrincipal AuthPrincipal principal) {
        service.replaceRanges(id, body);
        return ResponseEntity.noContent().build();
    }

    /** 구간표 조회 */
    @GetMapping("/{id}/ranges")
    public List<CmsRangeRes> getRanges(@PathVariable Long id) {
        return service.getRanges(id);
    }
}