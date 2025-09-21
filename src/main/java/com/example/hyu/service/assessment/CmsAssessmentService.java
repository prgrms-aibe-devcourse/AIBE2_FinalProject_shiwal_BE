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

    @Transactional
    public Long createAssessment(CmsAssessmentUpsertReq req) {
        if (assessmentRepo.countAnyByCode(req.code()) > 0) {
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

    @Transactional
    public void updateAssessment(Long id, CmsAssessmentUpsertReq req) {
        Assessment a = assessmentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "assessment not found: " + id));
        if (req.code() != null && !req.code().equals(a.getCode())) {
            long dup = assessmentRepo.countActiveByCodeExcludingId(req.code(), id);
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

    @Transactional(readOnly = true)
    public Page<CmsAssessmentRes> listAssessments(Pageable pageable) {
        return assessmentRepo.findAll(pageable)
                .map(a -> new CmsAssessmentRes(
                        a.getId(), a.getCode(), a.getName(), a.getCategory(),
                        a.getStatus(), a.isDeleted(), a.getDeletedAt()
                ));
    }

    @Transactional(readOnly = true)
    public CmsAssessmentRes getOne(Long id) {
        Assessment a = assessmentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "assessment not found: " + id));
        return new CmsAssessmentRes(
                a.getId(), a.getCode(), a.getName(), a.getCategory(),
                a.getStatus(), a.isDeleted(), a.getDeletedAt()
        );
    }

    @Transactional(readOnly = true)
    public Page<CmsAssessmentRes> adminListIncludingDeleted(Pageable pageable) {
        return assessmentRepo.findAllIncludingDeleted(pageable)
                .map(a -> new CmsAssessmentRes(
                        a.getId(), a.getCode(), a.getName(), a.getCategory(),
                        a.getStatus(), a.isDeleted(), a.getDeletedAt()
                ));
    }

    @Transactional(readOnly = true)
    public CmsAssessmentRes adminGetOneIncludingDeleted(Long id) {
        Assessment a = assessmentRepo.findAnyById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "assessment not found: " + id));
        return new CmsAssessmentRes(
                a.getId(), a.getCode(), a.getName(), a.getCategory(),
                a.getStatus(), a.isDeleted(), a.getDeletedAt()
        );
    }

    @Transactional
    public void softDelete(Long id) {
        Assessment a = assessmentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "assessment not found: " + id));
        if (a.isDeleted()) return; // 멱등
        a.setDeleted(true);
        a.setDeletedAt(Instant.now());
        a.setStatus(Assessment.Status.ARCHIVED);
    }

    @Transactional
    public void restore(Long id) {
        Assessment a = assessmentRepo.findAnyById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "assessment not found: " + id));
        if (!a.isDeleted()) return; // 멱등
        a.restore();
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
        Set<Integer> seen = new HashSet<>();
        for (CmsQuestionUpsertReq q : items) {
            if (q.orderNo() == null || !seen.add(q.orderNo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderNo duplicated or null");
            }
            if (q.text() == null || q.text().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text required");
            }
        }
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
            validateRangeRequired(r); // [추가] 최소 필수값 검증
            if (prevEnd != null && r.minScore() <= prevEnd)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "overlapping ranges");
            prevEnd = r.maxScore();
        }

        rangeRepo.deleteAllInBatch(rangeRepo.findByAssessmentIdOrderByMinScoreAsc(assessmentId));
        for (CmsRangeUpsertReq r : sorted) {
            rangeRepo.save(AssessmentRange.builder()
                    .assessment(a)
                    .minScore(r.minScore())
                    .maxScore(r.maxScore())
                    .level(r.level())
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

    /* =========================
       점수 구간(Range) — 단건 CRUD (추가)
       ========================= */

    @Transactional
    public Long createRange(Long assessmentId, CmsRangeUpsertReq req) { // [추가]
        var a = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "assessment not found: " + assessmentId));
        validateRange(assessmentId, null, req); // [추가] 겹침/필수 검증
        var saved = rangeRepo.save(AssessmentRange.builder()
                .assessment(a)
                .minScore(req.minScore())
                .maxScore(req.maxScore())
                .level(req.level())
                .labelKo(req.labelKo())
                .summaryKo(req.summaryKo())
                .adviceKo(req.adviceKo())
                .build());
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public CmsRangeRes getRange(Long rangeId) { // [추가]
        var r = rangeRepo.findById(rangeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "range not found: " + rangeId));
        return new CmsRangeRes(r.getId(), r.getMinScore(), r.getMaxScore(),
                r.getLevel(), r.getLabelKo(), r.getSummaryKo(), r.getAdviceKo());
    }

    @Transactional
    public void updateRange(Long rangeId, CmsRangeUpsertReq req) { // [추가]
        var r = rangeRepo.findById(rangeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "range not found: " + rangeId));
        Long aid = r.getAssessment().getId();
        validateRange(aid, rangeId, req); // [추가] 자기 자신 제외 겹침검증
        r.setMinScore(req.minScore());
        r.setMaxScore(req.maxScore());
        r.setLevel(req.level());
        r.setLabelKo(req.labelKo());
        r.setSummaryKo(req.summaryKo());
        r.setAdviceKo(req.adviceKo());
    }

    @Transactional
    public void deleteRange(Long rangeId) { // [추가]
        if (!rangeRepo.existsById(rangeId)) return; // 멱등
        rangeRepo.deleteById(rangeId);
    }

    /* =========================
       공통 검증
       ========================= */

    private static void validateRangeRequired(CmsRangeUpsertReq r) { // [추가]
        if (r.minScore() == null || r.maxScore() == null || r.minScore() > r.maxScore()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "invalid range: " + r.minScore() + "~" + r.maxScore());
        }
        if (r.level() == null || isBlank(r.labelKo()) || isBlank(r.summaryKo()) || isBlank(r.adviceKo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "level/label/summary/advice required");
        }
    }

    private void validateRange(Long assessmentId, Long excludeId, CmsRangeUpsertReq r) { // [추가]
        validateRangeRequired(r);
        long overlaps = (excludeId == null)
                ? rangeRepo.countOverlaps(assessmentId, r.minScore(), r.maxScore())
                : rangeRepo.countOverlapsExcludingId(assessmentId, excludeId, r.minScore(), r.maxScore());
        if (overlaps > 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "overlapping ranges");
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); } // [추가]
}