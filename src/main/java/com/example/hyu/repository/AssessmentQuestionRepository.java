package com.example.hyu.repository;

import com.example.hyu.entity.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 문항
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, Long> {

    List<AssessmentQuestion> findByAssessmentIdOrderByOrderNoAsc(Long assessmentId);

    long countByAssessmentId(Long assessmentId);     // 결과 maxScore 계산용

    void deleteByAssessmentId(Long assessmentId);    // CMS 교체(replace)에서 사용
}

