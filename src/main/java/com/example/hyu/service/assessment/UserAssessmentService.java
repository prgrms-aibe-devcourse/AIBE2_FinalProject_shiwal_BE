package com.example.hyu.service.assessment;

import com.example.hyu.dto.assessment.user.*;
import com.example.hyu.dto.kpi.EventRequest;
import com.example.hyu.entity.*;
import com.example.hyu.repository.assessment.*;
import com.example.hyu.service.kpi.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAssessmentService {

    private final AssessmentRepository assessmentRepo;
    private final AssessmentQuestionRepository questionRepo;
    private final AssessmentSubmissionRepository submissionRepo;
    private final AssessmentAnswerRepository answerRepo;
    private final AssessmentRangeRepository rangeRepo;
    private final EventService eventService;

    /* =========================
       1) 검사 목록 / 상세
       ========================= */

    @Transactional(readOnly = true)
    public Page<AssessmentRes> listActive(Pageable pageable) {
        return assessmentRepo
                .findByStatus(Assessment.Status.ACTIVE, pageable)
                .map(a -> new AssessmentRes(
                        a.getId(), a.getCode(), a.getName(), a.getCategory(), a.getDescription()
                ));
    }

    @Transactional(readOnly = true)
    public AssessmentRes getActiveByCode(String code) {
        Assessment a = assessmentRepo.findByCodeAndStatus(code, Assessment.Status.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("assessment not found or inactive: " + code));
        return new AssessmentRes(a.getId(), a.getCode(), a.getName(), a.getCategory(), a.getDescription());
    }

    /* =========================
       2) 문항 조회
       ========================= */

    @Transactional(readOnly = true)
    public List<AssessmentQuestionsRes> getQuestions(Long assessmentId) {
        Assessment a = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("assessment not found: " + assessmentId));
        if (a.getStatus() != Assessment.Status.ACTIVE) {
            throw new IllegalStateException("assessment inactive");
        }
        return questionRepo.findByAssessmentIdOrderByOrderNoAsc(assessmentId).stream()
                .map(q -> new AssessmentQuestionsRes(q.getId(), q.getOrderNo(), q.getText()))
                .toList();
    }

    /* =========================
       3) 임시저장(업서트)
       ========================= */

    @Transactional
    public void upsertDraftAnswer(Long assessmentId,
                                  Long userId,
                                  String headerGuestKey,
                                  AssessmentAnswerReq req) {

        // [추가] 값 검증
        if (req.value() == null || req.value() < 0 || req.value() > 3) {
            throw new IllegalArgumentException("answer value must be 0~3");
        }

        String guestKey = StringUtils.hasText(req.guestKey()) ? req.guestKey() : headerGuestKey;
        if (userId == null && !StringUtils.hasText(guestKey)) {
            throw new IllegalArgumentException("guestKey required for anonymous");
        }

        // [수정] 통합 로직 사용
        AssessmentSubmission submission = resolveDraftForWrite(
                assessmentId, userId, guestKey, req.submissionId() /*nullable*/);

        AssessmentQuestion question = questionRepo.findById(req.questionId())
                .orElseThrow(() -> new IllegalArgumentException("question not found: " + req.questionId()));
        if (!question.getAssessment().getId().equals(assessmentId)) {
            throw new IllegalArgumentException("question does not belong to assessment");
        }

        var existing = answerRepo.findBySubmissionIdAndQuestionId(submission.getId(), req.questionId());
        if (existing.isPresent()) {
            existing.get().setSelectedValue(req.value());
            existing.get().setRawAnswer(req.rawAnswer());
        } else {
            AssessmentAnswer a = AssessmentAnswer.builder()
                    .submission(submission)
                    .question(question)
                    .selectedValue(req.value())
                    .rawAnswer(req.rawAnswer())
                    .build();
            submission.getAnswers().add(a);
            answerRepo.save(a);
        }
    }

    /* =========================
       4) 최종 제출
       ========================= */

    @Transactional
    public AssessmentSubmitRes submit(AssessmentSubmitReq req,
                                      Long userId,
                                      String headerGuestKey) {

        String guestKey = StringUtils.hasText(req.guestKey()) ? req.guestKey() : headerGuestKey;
        if (userId == null && !StringUtils.hasText(guestKey)) {
            throw new IllegalArgumentException("guestKey required for anonymous");
        }
        Long assessmentId = req.assessmentId();

        // [수정] 통합 로직 사용(소유권/상태 검증 포함)
        AssessmentSubmission submission = resolveDraftForWrite(
                assessmentId, userId, guestKey, req.submissionId());

        // 요청에 answers가 있으면 최신화
        if (req.answers() != null && !req.answers().isEmpty()) {
            for (AssessmentAnswerReq a : req.answers()) {
                upsertDraftAnswer(assessmentId, userId, headerGuestKey, a);
            }
        }

        // 전체 응답 확인
        long totalQ = questionRepo.countByAssessmentId(assessmentId);
        long answered = answerRepo.countBySubmissionId(submission.getId());
        if (answered != totalQ) {
            throw new IllegalStateException("not all questions answered: answered=" + answered + ", total=" + totalQ);
        }

        // 점수 계산 → 구간 매핑
        int total = answerRepo.sumTotalScore(submission.getId());
        AssessmentRange band = rangeRepo.findBand(assessmentId, total)
                .orElseThrow(() -> new IllegalStateException("score band not defined for score: " + total));

        // 제출 확정
        submission.applyResult(total, band.getLevel());
        submission.setSubmittedAt(Instant.now());
        submission.setStatus(AssessmentSubmission.Status.SUBMITTED);
        submissionRepo.save(submission);

        // 이벤트 기록
        EventRequest evCompleted = new EventRequest(
                userId, "self_assessment_completed", Instant.now().toString(),
                "ok", null, submission.getId().toString(),
                Map.of("assessmentId", assessmentId)
        );
        eventService.ingest(evCompleted, null);

        EventRequest evRisk = new EventRequest(
                userId, "risk_detected", Instant.now().toString(),
                "ok", band.getLevel().name().toLowerCase(),
                submission.getId().toString(),
                Map.of("assessmentId", assessmentId, "score", total)
        );
        eventService.ingest(evRisk, null);

        return new AssessmentSubmitRes(
                submission.getId(), assessmentId, submission.getSubmittedAt(),
                band.getLevel(), band.getLabelKo(), band.getSummaryKo(), band.getAdviceKo()
        );
    }

    /* =========================
       5) 히스토리/최근 결과 (로그인 전용)
       ========================= */

    @Transactional(readOnly = true)
    public Page<AssessmentHistoryItemRes> history(Long assessmentId, Long userId, Pageable pageable) {
        return submissionRepo
                .findByAssessmentIdAndUserIdOrderBySubmittedAtDesc(assessmentId, userId, pageable)
                .map(s -> new AssessmentHistoryItemRes(
                        s.getId(), s.getSubmittedAt(), s.getRisk()
                ));
    }

    @Transactional(readOnly = true)
    public AssessmentSubmitRes latestResult(Long assessmentId, Long userId) {
        AssessmentSubmission s = submissionRepo
                .findByAssessmentIdAndUserIdOrderBySubmittedAtDesc(assessmentId, userId, Pageable.ofSize(1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no submission"));

        // 주의: Range가 업데이트되면 과거 결과의 해석이 달라질 수 있음(현재 정책: 최신 Range로 재해석)
        AssessmentRange band = rangeRepo.findBand(assessmentId, s.getTotalScore())
                .orElseThrow(() -> new IllegalStateException("score band not found"));

        return new AssessmentSubmitRes(
                s.getId(), assessmentId, s.getSubmittedAt(),
                band.getLevel(), band.getLabelKo(), band.getSummaryKo(), band.getAdviceKo()
        );
    }

    /* =========================
       내부: 드래프트 찾기/생성 & 소유권 검증 (통합)
       ========================= */

    private AssessmentSubmission resolveDraftForWrite(Long assessmentId,
                                                      Long userId /*nullable*/,
                                                      String guestKey /*nullable*/,
                                                      Long submissionId /*nullable*/) {
        // 검사 활성 확인
        Assessment a = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("assessment not found: " + assessmentId));
        if (a.getStatus() != Assessment.Status.ACTIVE) {
            throw new IllegalStateException("assessment inactive");
        }

        // submissionId가 명시된 경우: 소유권/상태 검증 후 사용
        if (submissionId != null) {
            AssessmentSubmission sub = submissionRepo.findByIdAndAssessmentId(submissionId, assessmentId)
                    .orElseThrow(() -> new IllegalArgumentException("submission not found"));
            if (userId != null) {
                if (sub.getUserId() == null || !sub.getUserId().equals(userId)) {
                    throw new IllegalStateException("not owner of the submission");
                }
            } else {
                if (!StringUtils.hasText(guestKey)) {
                    throw new IllegalArgumentException("guestKey required for anonymous");
                }
                if (sub.getGuestKey() == null || !guestKey.equals(sub.getGuestKey())) {
                    throw new IllegalStateException("not owner of the submission");
                }
            }
            if (sub.isSubmitted()) {
                throw new IllegalStateException("already submitted; cannot modify");
            }
            return sub;
        }

        // 없는 경우: 기존 DRAFT 재사용 또는 신규 생성
        if (userId != null) {
            return submissionRepo
                    .findFirstByAssessmentIdAndUserIdAndStatusOrderByIdDesc(
                            assessmentId, userId, AssessmentSubmission.Status.DRAFT)
                    .orElseGet(() -> submissionRepo.save(AssessmentSubmission.builder()
                            .assessment(a)
                            .userId(userId)
                            .status(AssessmentSubmission.Status.DRAFT)
                            .build()));
        } else {
            if (!StringUtils.hasText(guestKey)) {
                throw new IllegalArgumentException("guestKey required for anonymous");
            }
            return submissionRepo
                    .findFirstByAssessmentIdAndGuestKeyAndStatusOrderByIdDesc(
                            assessmentId, guestKey, AssessmentSubmission.Status.DRAFT)
                    .orElseGet(() -> submissionRepo.save(AssessmentSubmission.builder()
                            .assessment(a)
                            .guestKey(guestKey)
                            .status(AssessmentSubmission.Status.DRAFT)
                            .build()));
        }
    }
}