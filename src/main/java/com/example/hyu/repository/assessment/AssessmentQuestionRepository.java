package com.example.hyu.repository.assessment;

import com.example.hyu.entity.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// 문항
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, Long> {

    // 검사별 문항을 순서대로 조회
    List<AssessmentQuestion> findByAssessmentIdOrderByOrderNoAsc(Long assessmentId);

    //해당 검사에 문항이 몇 개인지 카운트
    long countByAssessmentId(Long assessmentId);

    // 한 검사에 속한 모든 문항 삭제
    void deleteByAssessmentId(Long assessmentId);    // CMS 교체(replace)에서 사용

    // 문항 추가 시 중복 순서 방지
    boolean existsByAssessmentIdAndOrderNo(Long assessmentId, Integer orderNo);
    boolean existsByAssessmentIdAndOrderNoAndIdNot(Long assessmentId, Integer orderNo, Long excludeId);

    // 새 문항 추가할떄 자동으로 다음 순서 번호 할당
    Optional<AssessmentQuestion> findTop1ByAssessmentIdOrderByOrderNoDesc(Long assessmentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AssessmentQuestion q where q.assessment.id = :aid")
    void hardDeleteByAssessmentId(@Param("aid") Long assessmentId);

}

