package com.example.hyu.repository;

import com.example.hyu.entity.AssessmentRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// 점수 구간표
public interface AssessmentRangeRepository extends JpaRepository<AssessmentRange, Long> {

    // 제출 시 범위 찾기: 서비스에선 리스트 받아서 필터링하지만, 직접 한방 쿼리도 가능
    @Query("select r from AssessmentRange r " +
            "where r.assessment.id = :aid and :score between r.minScore and r.maxScore")
    Optional<AssessmentRange> findBand(@Param("aid") Long assessmentId,
                                       @Param("score") int score);

    List<AssessmentRange> findByAssessmentIdOrderByMinScoreAsc(Long assessmentId);
}

