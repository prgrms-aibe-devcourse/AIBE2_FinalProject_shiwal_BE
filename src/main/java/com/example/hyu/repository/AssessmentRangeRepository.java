package com.example.hyu.repository;

import com.example.hyu.entity.AssessmentRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// 점수 구간표
public interface AssessmentRangeRepository extends JpaRepository<AssessmentRange, Long> {
    // 총점이 들어가는 구간 찾기 (min <= score <= max)
    @Query("select r from AssessmentRange r where r.assessment.id = :aid " +
            "and :score between r.minScore and r.maxScore")
    Optional<AssessmentRange> findBand(@Param("aid") Long assessmentId, @Param("score") int score);

    List<AssessmentRange> findByAssessmentIdOrderByMinScoreAsc(Long assessmentId);
}
