package com.example.hyu.service;

import com.example.hyu.dto.Assessment.user.*;
import com.example.hyu.entity.*;
import com.example.hyu.repository.Assessment.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAssessmentService {

    private final AssessmentRepository assessmentRepo;
    private final AssessmentQuestionRepository questionRepo;
    private final AssessmentSubmissionRepository submissionRepo;
    private final AssessmentAnswerRepository answerRepo;
    private final AssessmentRangeRepository rangeRepo;

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
        // 활성 검사가 아니라면 차단(선택)
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
       - 로그인: userId 기반 드래프트
       - 비로그인: guestKey 기반 드래프트
       - submissionId가 오면 소유권 검증 후 그 드래프트 사용
       ========================= */

    @Transactional
    public void upsertDraftAnswer(Long assessmentId,
                                  Long userId,            // null이면 비로그인
                                  String headerGuestKey,  // 컨트롤러에서 전달
                                  AssessmentAnswerReq req) {

        // 1) 게스트키 최종 병합
        String guestKey = StringUtils.hasText(req.guestKey()) ? req.guestKey() : headerGuestKey;

        // 2) 로그인/비로그인 가드
        if (userId == null && !StringUtils.hasText(guestKey)) {
            throw new IllegalArgumentException("guestKey required for anonymous");
        }

        // 3) 제출 초안 확보(로그인/게스트 분기)
        AssessmentSubmission submission = getOrCreateDraftSubmission(assessmentId, userId, guestKey);

        if (submission.isSubmitted()) {
            throw new IllegalStateException("already submitted; cannot modify answers");
        }

        // 4) 문항 소속 검증
        AssessmentQuestion question = questionRepo.findById(req.questionId())
                .orElseThrow(() -> new IllegalArgumentException("question not found: " + req.questionId()));
        if (!question.getAssessment().getId().equals(assessmentId)) {
            throw new IllegalArgumentException("question does not belong to assessment");
        }

        // 5) upsert
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
       - answers가 동봉되면 일괄 업서트 후 제출
       - 미동봉이면 기존 드래프트 기준으로 제출
       - 게스트/유저 공통
       ========================= */

    @Transactional
    public AssessmentSubmitRes submit(AssessmentSubmitReq req,
                                      Long userId,
                                      String headerGuestKey) {

        // 1) 게스트키 병합
        String guestKey = StringUtils.hasText(req.guestKey()) ? req.guestKey() : headerGuestKey;

        // 2) 로그인/비로그인 가드
        if (userId == null && !StringUtils.hasText(guestKey)) {
            throw new IllegalArgumentException("guestKey required for anonymous");
        }

        Long assessmentId = req.assessmentId();

        // 3) 초안 확보(로그인/게스트 분기) — submissionId가 있으면 우선 사용
        AssessmentSubmission submission = null;
        if (req.submissionId() != null) {
            submission = submissionRepo.findByIdAndAssessmentId(req.submissionId(), assessmentId)
                    .orElseThrow(() -> new IllegalArgumentException("invalid submissionId"));
            if (userId != null) {
                if (!userId.equals(submission.getUserId()))
                    throw new IllegalArgumentException("submission does not belong to user");
            } else {
                if (!guestKey.equals(submission.getGuestKey()))
                    throw new IllegalArgumentException("submission does not belong to guestKey");
            }
            if (submission.isSubmitted()) throw new IllegalStateException("already submitted");
        } else {
            submission = getOrCreateDraftSubmission(assessmentId, userId, guestKey);
        }

        // 4) 요청에 answers 있으면 최신화
        if (req.answers() != null && !req.answers().isEmpty()) {
            for (AssessmentAnswerReq a : req.answers()) {
                // 헤더값을 함께 넘겨 동일 병합 규칙 유지
                upsertDraftAnswer(assessmentId, userId, headerGuestKey, a);
            }
        }

        // 5) 전부 응답했는지 검증
        long totalQ = questionRepo.countByAssessmentId(assessmentId);
        long answered = answerRepo.countBySubmissionId(submission.getId());
        if (answered != totalQ) {
            throw new IllegalStateException("not all questions answered: answered=" + answered + ", total=" + totalQ);
        }

        // 6) 점수 계산 → 밴드 매핑
        int total = answerRepo.sumTotalScore(submission.getId());
        AssessmentRange band = rangeRepo.findBand(assessmentId, total)
                .orElseThrow(() -> new IllegalStateException("score band not defined for score: " + total));

        // 7) 제출 확정
        submission.applyResult(total, band.getLevel());
        submission.setSubmittedAt(Instant.now());
        submission.setStatus(AssessmentSubmission.Status.SUBMITTED);
        submissionRepo.save(submission);

        // 8) 응답
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

        AssessmentRange band = rangeRepo.findBand(assessmentId, s.getTotalScore())
                .orElseThrow(() -> new IllegalStateException("score band not found"));

        return new AssessmentSubmitRes(
                s.getId(),
                assessmentId,
                s.getSubmittedAt(),
                band.getLevel(),
                band.getLabelKo(),
                band.getSummaryKo(),
                band.getAdviceKo()
        );
    }

    /* =========================
       내부: 드래프트 찾기/생성 공통 로직
       ========================= */

    /**
     * upsert/submit에서 사용할 드래프트를 확정한다.
     * - submissionId가 오면 소유권/소속 검사 후 그 드래프트 사용
     * - 없으면 로그인/게스트 키에 따라 기존 DRAFT 재사용 또는 신규 생성
     */
    private AssessmentSubmission resolveDraftForWrite(Long assessmentId,
                                                      Long userId /*nullable*/,
                                                      String guestKey /*nullable*/,
                                                      Long submissionId /*nullable*/) {
        // 0) 검사 활성 확인
        Assessment a = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("assessment not found: " + assessmentId));
        if (a.getStatus() != Assessment.Status.ACTIVE) {
            throw new IllegalStateException("assessment inactive");
        }

        // 1) submissionId가 명시되면: 소유권/소속/상태 확인 후 사용
        if (submissionId != null) {
            AssessmentSubmission sub = submissionRepo.findByIdAndAssessmentId(submissionId, assessmentId)
                    .orElseThrow(() -> new IllegalArgumentException("submission not found"));

            // 로그인 유저인 경우
            if (userId != null) {
                if (sub.getUserId() == null || !sub.getUserId().equals(userId)) {
                    throw new IllegalStateException("not owner of the submission");
                }
            } else {
                // 게스트인 경우
                if (guestKey == null || guestKey.isBlank()) {
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

        // 2) submissionId가 없으면: 기존 DRAFT 재사용 or 신규 생성
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
            if (guestKey == null || guestKey.isBlank()) {
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

    /* 로그인/게스트 공용 초안 확보 */
    private AssessmentSubmission getOrCreateDraftSubmission(Long assessmentId, Long userId, String guestKey) {
        Assessment a = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("assessment not found: " + assessmentId));
        if (a.getStatus() != Assessment.Status.ACTIVE) {
            throw new IllegalStateException("assessment inactive");
        }

        if (userId != null) {
            return submissionRepo.findFirstByAssessmentIdAndUserIdAndStatusOrderByIdDesc(
                            assessmentId, userId, AssessmentSubmission.Status.DRAFT)
                    .orElseGet(() -> submissionRepo.save(AssessmentSubmission.builder()
                            .assessment(a).userId(userId)
                            .status(AssessmentSubmission.Status.DRAFT).build()));
        } else {
            return submissionRepo.findFirstByAssessmentIdAndGuestKeyAndStatusOrderByIdDesc(
                            assessmentId, guestKey, AssessmentSubmission.Status.DRAFT)
                    .orElseGet(() -> submissionRepo.save(AssessmentSubmission.builder()
                            .assessment(a).guestKey(guestKey)
                            .status(AssessmentSubmission.Status.DRAFT).build()));
        }
    }
}