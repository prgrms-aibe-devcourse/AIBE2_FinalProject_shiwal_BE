package com.example.hyu.repository;

import com.example.hyu.entity.AssessmentSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

// 제출/결과
public interface AssessmentSubmissionRepository extends JpaRepository<AssessmentSubmission, Long> {
    Page<AssessmentSubmission> findByAssessmentIdAndUserIdOrderBySubmittedAtDesc(
            Long assessmentId, Long userId, Pageable pageable);
}

