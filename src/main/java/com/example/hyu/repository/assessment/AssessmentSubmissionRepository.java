package com.example.hyu.repository.assessment;

import com.example.hyu.entity.AssessmentSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 제출/결과
public interface AssessmentSubmissionRepository extends JpaRepository<AssessmentSubmission, Long> {

    /* =========================
       1) 로그인 사용자의 히스토리/최신
       ========================= */
    Page<AssessmentSubmission> findByAssessmentIdAndUserIdOrderBySubmittedAtDesc(
            Long assessmentId, Long userId, Pageable pageable);

    Page<AssessmentSubmission> findByUserIdOrderBySubmittedAtDesc(Long userId, Pageable pageable);

    Page<AssessmentSubmission> findByAssessmentIdOrderBySubmittedAtDesc(Long assessmentId, Pageable pageable);

    /* (선택) 최신 1건 편의 메서드 */
    Optional<AssessmentSubmission> findFirstByAssessmentIdAndUserIdAndStatusOrderBySubmittedAtDesc(
            Long assessmentId, Long userId, AssessmentSubmission.Status status);


    /* =========================
       2) 드래프트(임시저장) 조회/소유권 확인
       - 게스트/유저 공통 플로우에서 사용
       ========================= */

    // (A) 유저 드래프트 찾기: assessment + userId + DRAFT
    Optional<AssessmentSubmission> findFirstByAssessmentIdAndUserIdAndStatusOrderByIdDesc(
            Long assessmentId, Long userId, AssessmentSubmission.Status status);

    // (B) 게스트 드래프트 찾기: assessment + guestKey + DRAFT
    Optional<AssessmentSubmission> findFirstByAssessmentIdAndGuestKeyAndStatusOrderByIdDesc(
            Long assessmentId, String guestKey, AssessmentSubmission.Status status);

    // (C) 제출 엔티티가 해당 검사에 속하는지(상태 무관)
    Optional<AssessmentSubmission> findByIdAndAssessmentId(Long id, Long assessmentId);

    // (D) “내 드래프트(로그인)” 소유권 확인
    Optional<AssessmentSubmission> findByIdAndAssessmentIdAndUserId(
            Long id, Long assessmentId, Long userId);

    // (E) “내 드래프트(게스트)” 소유권 확인
    Optional<AssessmentSubmission> findByIdAndAssessmentIdAndGuestKey(
            Long id, Long assessmentId, String guestKey);

    // (선택) DRAFT까지 한 번에 확인하고 싶을 때
    Optional<AssessmentSubmission> findByIdAndAssessmentIdAndStatus(
            Long id, Long assessmentId, AssessmentSubmission.Status status);
}