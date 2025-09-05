package com.example.hyu.service;

import com.example.hyu.dto.user.*;
import com.example.hyu.entity.*;
import com.example.hyu.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAssessmentService {

    private final AssessmentRepository assessmentRepo;
    private final AssessmentQuestionRepository questionRepo;
    private final AssessmentRangeRepository rangeRepo;
    private final AssessmentSubmissionRepository submissionRepo;
    private final AssessmentAnswerRepository answerRepo;

    /** (1) 사이드바: ACTIVE 평가를 카테고리별로 그룹핑해서 반환 */
    public Map<String, List<AssessmentListRes>> sidebar() {
        var list = assessmentRepo.findByStatus(Assessment.Status.ACTIVE);

        return list.stream()
                .sorted(Comparator.comparing(Assessment::getCategory)
                        .thenComparing(Assessment::getName))
                .map(a -> new AssessmentListRes(a.getCode(), a.getName(), a.getCategory()))
                .collect(Collectors.groupingBy(
                        AssessmentListRes::category,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    /** (2) 문항 조회 */
    public AssessmentQuestionsRes getQuestions(String code) {
        var a = assessmentRepo.findByCodeAndStatus(code, Assessment.Status.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "검사를 찾을 수 없습니다: " + code));

        var qs = questionRepo.findByAssessmentIdOrderByOrderNoAsc(a.getId());

        // ✅ 중첩 record 풀네임으로 생성
        var items = qs.stream()
                .map(q -> new AssessmentQuestionsRes.QuestionItem(
                        q.getId(), q.getOrderNo(), q.getText()))
                .toList();

        var scale = new AssessmentQuestionsRes.Scale(
                0, 3, List.of("전혀 아님","가끔","자주","거의 항상")
        );

        var meta = new AssessmentQuestionsRes.Meta(
                a.getCode(), a.getName(), qs.size()
        );

        return new AssessmentQuestionsRes(meta, scale, items);
    }

    /** (3) 제출 + 결과 반환 */
    @Transactional
    public AssessmentSubmitRes submit(String code, AssessmentSubmitReq req, Long userId) {
        if (req == null || req.answers() == null || req.answers().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "응답이 비었습니다.");
        }

        // 1) 검사 조회 (ACTIVE만)
        var a = assessmentRepo.findByCodeAndStatus(code, Assessment.Status.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "검사를 찾을 수 없습니다: " + code));

        // 2) 문항 조회/매핑
        var questions = questionRepo.findByAssessmentIdOrderByOrderNoAsc(a.getId());
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "문항이 설정되어 있지 않습니다.");
        }
        var qMap = questions.stream()
                .collect(Collectors.toMap(AssessmentQuestion::getId, q -> q));

        // 3) 값 검증 + 점수 합계
        int total = 0;
        for (var item : req.answers()) {
            var q = qMap.get(item.questionId());
            if (q == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "유효하지 않은 문항: " + item.questionId());
            }
            Integer v = item.value();
            if (v == null || v < 0 || v > 3) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "값은 0~3이어야 합니다. questionId=" + item.questionId());
            }
            total += q.isReverseScore() ? (3 - v) : v;
        }
        int finalTotal = total; // 람다에서 사용할 불변 변수

        // 4) 점수 구간 매핑 (없으면 409)
        var range = rangeRepo.findByAssessmentIdOrderByMinScoreAsc(a.getId()).stream()
                .filter(r -> finalTotal >= r.getMinScore() && finalTotal <= r.getMaxScore())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "점수 구간이 설정되어 있지 않습니다. total=" + finalTotal));

        // 5) 제출 엔티티: total/risk를 채워서 저장 (NOT NULL 보호)
        var sub = AssessmentSubmission.builder()
                .assessment(a)
                .userId(userId)
                .totalScore(finalTotal)
                .risk(range.getLevel())      // ⚠ enum 이름 DB와 일치해야 함
                .submittedAt(Instant.now())
                .build();
        submissionRepo.save(sub);

        // 6) 답안 저장 (FK는 sub 먼저 저장되어 있어야 안정)
        try {
            for (var item : req.answers()) {
                var q = qMap.get(item.questionId());
                answerRepo.save(AssessmentAnswer.builder()
                        .submission(sub)
                        .question(q)
                        .selectedValue(item.value())
                        .rawAnswer(null)
                        .build());
            }
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // FK/NOT NULL/enum 등 제약 위반 → 409로 노출
            throw new ResponseStatusException(HttpStatus.CONFLICT, "데이터 제약 위반: " + root(e));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "답안 저장 실패: " + root(e));
        }

        int maxScore = questions.size() * 3;

        return new AssessmentSubmitRes(
                sub.getId(),
                a.getCode(),
                finalTotal,
                maxScore,
                range.getLevel().name(),
                range.getLabelKo(),
                range.getSummaryKo(),
                range.getAdviceKo()
        );
    }

    private String root(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null) r = r.getCause();
        return r.getMessage();
    }

    /** (4) 결과 재조회 */
    // 제출 직후 결과 페이지를 새로고침/공유 링크에서 다시 열기 기능
    public AssessmentSubmitRes getResult(String code, Long submissionId) {
        var sub = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "제출을 찾을 수 없습니다."));

        var a = sub.getAssessment();
        if (!a.getCode().equals(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "코드가 일치하지 않습니다.");
        }

        var range = rangeRepo.findByAssessmentIdOrderByMinScoreAsc(a.getId()).stream()
                .filter(r -> sub.getTotalScore() >= r.getMinScore() && sub.getTotalScore() <= r.getMaxScore())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "점수 구간 미설정"));

        int maxScore = (int) questionRepo.countByAssessmentId(a.getId()) * 3;

        return new AssessmentSubmitRes(
                sub.getId(),
                a.getCode(),
                sub.getTotalScore(),
                maxScore,
                range.getLevel().name(),
                range.getLabelKo(),
                range.getSummaryKo(),
                range.getAdviceKo()
        );
    }

    /** (5) 지난 검사 이력 **/
    // 해당 유저가 같은 검사에서 과거에 제출한 기록들(페이지네이션)
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AssessmentHistoryItemRes> history(
            String code, Long userId, org.springframework.data.domain.Pageable pageable) {

        var a = assessmentRepo.findByCodeAndStatus(code, Assessment.Status.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "검사를 찾을 수 없습니다: " + code));

        var page = submissionRepo.findByAssessmentIdAndUserIdOrderBySubmittedAtDesc(
                a.getId(), userId, pageable);

        int maxScore = (int) questionRepo.countByAssessmentId(a.getId()) * 3;

        return page.map(sub -> {
            var label = rangeRepo.findByAssessmentIdOrderByMinScoreAsc(a.getId()).stream()
                    .filter(r -> sub.getTotalScore() >= r.getMinScore() && sub.getTotalScore() <= r.getMaxScore())
                    .findFirst()
                    .map(AssessmentRange::getLabelKo)
                    .orElse("미정의");
            return new AssessmentHistoryItemRes(
                    sub.getId(), sub.getSubmittedAt(), sub.getTotalScore(), maxScore,
                    sub.getRisk().name(), label
            );
        });
    }
}