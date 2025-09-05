package com.example.hyu.repository;

import com.example.hyu.entity.AssessmentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 문항별 응답
public interface AssessmentAnswerRepository extends JpaRepository<AssessmentAnswer, Long> {

    List<AssessmentAnswer> findBySubmissionId(Long submissionId);
}

