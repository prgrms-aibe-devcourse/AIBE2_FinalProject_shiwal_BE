package com.example.hyu.service.assessment;

import com.example.hyu.dto.assessment.admin.*;
import com.example.hyu.entity.*;
import com.example.hyu.repository.assessment.AssessmentQuestionRepository;
import com.example.hyu.repository.assessment.AssessmentRangeRepository;
import com.example.hyu.repository.assessment.AssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CmsAssessmentService {

    private final AssessmentRepository assessmentRepo;
    private final AssessmentQuestionRepository questionRepo;
    private final AssessmentRangeRepository rangeRepo;

    /* =========================
       검사(Assessment)
       ========================= */

    /** 생성 */
    @Transactional
    public Long createAssessment(CmsAssessmentUpsertReq req) {
        // 삭제 포함 전체에서 코드 중복 체크
        if (assessmentRepo.countAnyByCode(req.code()) > 0) {     // ← existsAnyByCode → countAnyByCode
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 코드입니다: " + req.code());
        }

        Assessment.Status status = Optional.ofNullable(req.status()).orElse(Assessment.Status.ACTIVE);

        Assessment a = Assessment.builder()
                .code(req.code())
                .name(req.name())
                .category(req.category())
                .description(req.description())
                .status(status)
                .build();

        return assessmentRepo.save(a).getId();
    }

    /** 수정 (코드 포함 모든 필드 수정 가능 / 단, 코드 중복은 방지) */
    @Transactional
    public void updateAssessment(Long id, CmsAssessmentUpsertReq req) {
        Assessment a = assessmentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "assessment not found: " + id));

        // 코드 변경 시: '미삭제' 검사들만 대상으로, 자기 자신 제외 중복 체크
        if (req.code() != null && !req.code().equals(a.getCode())) {
            long dup = assessmentRepo.countActiveByCodeExcludingId(req.code(), id); // ← findAnyByCodeExcludingId → countActiveByCodeExcludingId
            if (dup > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 코드입니다: " + req.code());
            }
            a.setCode(req.code());
        }

        if (req.name() != null) a.setName(req.name());
        if (req.category() != null) a.setCategory(req.category());
        if (req.description() != null) a.setDescription(req.description());
        if (req.status() != null) a.setStatus(req.status());
    }

    /** 사용자/일반 목록 (삭제 제외: @Where에 의해 자동) */
    @Transactional(readOnly = true)
    public Page<CmsAssessmentRes> listAssessments(Pageable pageable) {
        return assessmentRepo.findAll(pageable)
                .map(a -> new CmsAssessmentRes(
                        a.getId(), a.getCode(), a.getName(), a.getCategory(),
                        a.getStatus(), a.isDeleted(), a.getDeletedAt()
                ));
    }

    /** 사용자/일반 단건 (삭제 제외) */
    @Transactional(readOnly = true)
    public CmsAssessmentRes getOne(Long id) {
        Assessment a = assessmentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "assessment not found: " + id));
        return new CmsAssessmentRes(
                a.getId(), a.getCode(), a.getName(), a.getCategory(),
                a.getStatus(), a.isDeleted(), a.getDeletedAt()
        );
    }

    /** 관리자 목록 (삭제 포함) */
    @Transactional(readOnly = true)
    public Page<CmsAssessmentRes> adminListIncludingDeleted(Pageable pageable) {
        return assessmentRepo.findAllIncludingDeleted(pageable)
                .map(a -> new CmsAssessmentRes(
                        a.getId(), a.getCode(), a.getName(), a.getCategory(),
                        a.getStatus(), a.isDeleted(), a.getDeletedAt()
                ));
    }

    /** 관리자 단건 (삭제 포함) */
    @Transactional(readOnly = true)
    public CmsAssessmentRes adminGetOneIncludingDeleted(Long id) {
        Assessment a = assessmentRepo.findAnyById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "assessment not found: " + id));
        return new CmsAssessmentRes(
                a.getId(), a.getCode(), a.getName(), a.getCategory(),
                a.getStatus(), a.isDeleted(), a.getDeletedAt()
        );
    }

    /** 소프트 삭제 (+ 사용자 노출 차단용 ARCHIVED) */
    @Transactional
    public void softDelete(Long id) {
        Assessment a = assessmentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "assessment not found: " + id));
        if (a.isDeleted()) return; // 멱등
        a.setDeleted(true);
        a.setDeletedAt(Instant.now());
        a.setStatus(Assessment.Status.ARCHIVED); // 사용자 노출 차단
    }

    /** 복구(코드로만 가능, UI 버튼은 없어도 유지) */
    @Transactional
    public void restore(Long id) {
        Assessment a = assessmentRepo.findAnyById(id) // 삭제 포함
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "assessment not found: " + id));
        if (!a.isDeleted()) return; // 멱등
        a.restore();
        // 상태 복원 규칙이 따로 있다면 여기서 처리(예: ARCHIVED 유지)
    }

    /* =========================
       문항(Question) — 전체 교체
       ========================= */

    @Transactional
    public void replaceQuestions(Long assessmentId, List<CmsQuestionUpsertReq> items) {
        Assessment a = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "assessment not found: " + assessmentId));

        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "questions required");
        }

        // 입력 검증
        Set<Integer> seen = new HashSet<>();
        for (CmsQuestionUpsertReq q : items) {
            if (q.orderNo() == null || !seen.add(q.orderNo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderNo duplicated or null");
            }
            if (q.text() == null || q.text().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text required");
            }
        }

        // 유니크 제약(assessment_id, order_no) 충돌 방지: 벌크 삭제 + flush 뒤 재삽입
        questionRepo.hardDeleteByAssessmentId(assessmentId);
        questionRepo.flush();

        List<AssessmentQuestion> newOnes = items.stream()
                .sorted(Comparator.comparingInt(CmsQuestionUpsertReq::orderNo))
                .map(q -> AssessmentQuestion.builder()
                        .assessment(a)
                        .orderNo(q.orderNo())
                        .text(q.text())
                        .reverseScore(Boolean.TRUE.equals(q.reverseScore()))
                        .build())
                .toList();

        questionRepo.saveAll(newOnes);
    }

    @Transactional(readOnly = true)
    public List<CmsQuestionRes> getQuestions(Long assessmentId) {
        return questionRepo.findByAssessmentIdOrderByOrderNoAsc(assessmentId).stream()
                .map(q -> new CmsQuestionRes(
                        q.getId(), q.getAssessment().getId(), q.getOrderNo(), q.getText(), q.isReverseScore()
                ))
                .collect(Collectors.toList());
    }

    /* =========================
       점수 구간(Range) — 전체 교체
       ========================= */

    @Transactional
    public void replaceRanges(Long assessmentId, List<CmsRangeUpsertReq> items) {
        Assessment a = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "assessment not found: " + assessmentId));

        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ranges required");
        }

        var sorted = items.stream()
                .sorted(Comparator.comparingInt(CmsRangeUpsertReq::minScore))
                .toList();

        Integer prevEnd = null;
        for (CmsRangeUpsertReq r : sorted) {
            if (r.minScore() == null || r.maxScore() == null || r.minScore() > r.maxScore())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "invalid range: " + r.minScore() + "~" + r.maxScore());
            if (prevEnd != null && r.minScore() <= prevEnd)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "overlapping ranges");
            if (r.level() == null || r.labelKo() == null || r.summaryKo() == null || r.adviceKo() == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "level/label/summary/advice required");

            prevEnd = r.maxScore();
        }

        // 기존 전부 삭제 후 재삽입
        rangeRepo.deleteAllInBatch(rangeRepo.findByAssessmentIdOrderByMinScoreAsc(assessmentId));

        for (CmsRangeUpsertReq r : sorted) {
            rangeRepo.save(AssessmentRange.builder()
                    .assessment(a)
                    .minScore(r.minScore())
                    .maxScore(r.maxScore())
                    .level(r.level())     // enum 그대로
                    .labelKo(r.labelKo())
                    .summaryKo(r.summaryKo())
                    .adviceKo(r.adviceKo())
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<CmsRangeRes> getRanges(Long assessmentId) {
        return rangeRepo.findByAssessmentIdOrderByMinScoreAsc(assessmentId).stream()
                .map(r -> new CmsRangeRes(
                        r.getId(), r.getMinScore(), r.getMaxScore(),
                        r.getLevel(), r.getLabelKo(), r.getSummaryKo(), r.getAdviceKo()
                ))
                .toList();
    }
}