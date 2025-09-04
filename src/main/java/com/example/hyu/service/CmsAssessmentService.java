package com.example.hyu.service;

import com.example.hyu.dto.admin.*;
import com.example.hyu.entity.*;
import com.example.hyu.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CmsAssessmentService {

    private final AssessmentRepository assessmentRepo;
    private final AssessmentQuestionRepository questionRepo;
    private final AssessmentRangeRepository rangeRepo;

    /* ===== 검사 ===== */

    /** 생성 */
    @Transactional
    public Long createAssessment(CmsAssessmentUpsertReq req) {
        Assessment a = Assessment.builder()
                .code(req.code())
                .name(req.name())
                .category(req.category())
                .description(req.description())
                .status(Assessment.Status.valueOf(req.status()))
                .build();
        return assessmentRepo.save(a).getId();
    }

    /** 수정 */
    @Transactional
    public void updateAssessment(Long id, CmsAssessmentUpsertReq req) {
        Assessment a = assessmentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("assessment not found: " + id));
        if (req.name() != null) a.setName(req.name());
        if (req.category() != null) a.setCategory(req.category());
        if (req.description() != null) a.setDescription(req.description());
        if (req.status() != null) a.setStatus(Assessment.Status.valueOf(req.status()));
    }

    /** 목록 조회 */
    @Transactional(readOnly = true)
    public Page<CmsAssessmentRes> listAssessments(Pageable pageable) {
        return assessmentRepo.findAll(pageable)
                .map(a -> new CmsAssessmentRes(
                        a.getId(), a.getCode(), a.getName(), a.getCategory(), a.getStatus().name()
                ));
    }

    /** 단건 조회 */
    @Transactional(readOnly = true)
    public CmsAssessmentRes getOne(Long id) {
        Assessment a = assessmentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("assessment not found: " + id));
        return new CmsAssessmentRes(
                a.getId(), a.getCode(), a.getName(), a.getCategory(), a.getStatus().name()
        );
    }

    /** 소프트 삭제 (상태를 ARCHIVED로 전환) */
    @Transactional
    public void softDelete(Long id) {
        Assessment a = assessmentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("assessment not found: " + id));
        a.setStatus(Assessment.Status.ARCHIVED);
    }

    /* ===== 문항 ===== */

    @Transactional
    public void replaceQuestions(Long assessmentId, List<CmsQuestionUpsertReq> items) {
        Assessment a = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("assessment not found: " + assessmentId));

        if (items == null || items.isEmpty()) throw new IllegalArgumentException("questions required");

        Set<Integer> seen = new HashSet<>();
        for (CmsQuestionUpsertReq q : items) {
            if (q.orderNo() == null || !seen.add(q.orderNo()))
                throw new IllegalArgumentException("orderNo duplicated or null");
            if (q.text() == null || q.text().isBlank())
                throw new IllegalArgumentException("text required");
        }

        // 기존 삭제 후 재삽입
        var old = questionRepo.findByAssessmentIdOrderByOrderNoAsc(assessmentId);
        if (!old.isEmpty()) questionRepo.deleteAllInBatch(old);

        items.stream()
                .sorted(Comparator.comparingInt(CmsQuestionUpsertReq::orderNo))
                .forEach(q -> questionRepo.save(AssessmentQuestion.builder()
                        .assessment(a)
                        .orderNo(q.orderNo())
                        .text(q.text())
                        .reverseScore(Boolean.TRUE.equals(q.reverseScore()))
                        .build()));
    }

    @Transactional(readOnly = true)
    public List<CmsQuestionRes> getQuestions(Long assessmentId) {
        return questionRepo.findByAssessmentIdOrderByOrderNoAsc(assessmentId).stream()
                .map(q -> new CmsQuestionRes(q.getId(), q.getOrderNo(), q.getText(), q.isReverseScore()))
                .collect(Collectors.toList());
    }

    /* ===== 점수 구간표 ===== */

    @Transactional
    public void replaceRanges(Long assessmentId, List<CmsRangeUpsertReq> items) {
        Assessment a = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("assessment not found: " + assessmentId));

        if (items == null || items.isEmpty()) throw new IllegalArgumentException("ranges required");

        var sorted = items.stream()
                .sorted(Comparator.comparingInt(CmsRangeUpsertReq::minScore))
                .toList();

        Integer prevEnd = null;
        for (CmsRangeUpsertReq r : sorted) {
            if (r.minScore() == null || r.maxScore() == null || r.minScore() > r.maxScore())
                throw new IllegalArgumentException("invalid range: " + r.minScore() + "~" + r.maxScore());
            if (prevEnd != null && r.minScore() <= prevEnd)
                throw new IllegalArgumentException("overlapping ranges");
            if (r.level() == null || r.labelKo() == null || r.summaryKo() == null || r.adviceKo() == null)
                throw new IllegalArgumentException("level/label/summary/advice required");
            prevEnd = r.maxScore();
        }

        rangeRepo.deleteAllInBatch(rangeRepo.findByAssessmentIdOrderByMinScoreAsc(assessmentId));
        for (CmsRangeUpsertReq r : sorted) {
            rangeRepo.save(AssessmentRange.builder()
                    .assessment(a)
                    .minScore(r.minScore())
                    .maxScore(r.maxScore())
                    .level(AssessmentSubmission.RiskLevel.valueOf(r.level()))
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
                        r.getLevel().name(), r.getLabelKo(), r.getSummaryKo(), r.getAdviceKo()
                ))
                .toList();
    }
}