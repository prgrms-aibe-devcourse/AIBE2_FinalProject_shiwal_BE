package com.example.hyu.repository.Assessment;

import com.example.hyu.entity.AssessmentRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// 점수 구간표
public interface AssessmentRangeRepository extends JpaRepository<AssessmentRange, Long> {

    // 제출된 총점이 어느 구간에 속하는지 알려줌
    @Query("""
        select r
        from AssessmentRange r
        where r.assessment.id = :aid
        and :score between r.minScore and r.maxScore
        """)
    Optional<AssessmentRange> findBand(@Param("aid") Long assessmentId,
                                       @Param("score") int score);

    //특정 검사에 등록된 모든 구간을 minScore 오름차순으로 가져옴
    List<AssessmentRange> findByAssessmentIdOrderByMinScoreAsc(Long assessmentId);

    //새 구간이 기존 구간들과 겹치는지 확인
    @Query("""
            select count(r)
            from AssessmentRange r
            where r.assessment.id = :aid
                    and not(:newMax < r.minScore or :newMin > r.maxScore)
        """)
    long countOverlaps(
            @Param("aid") Long assessmentId,
            @Param("newMin") int newMin,
            @Param("newMax") int newMax
    );


    //수정할 떄는 자기 자신은 제외하고 겹침 검사
    @Query("""
            select count(r)
            from AssessmentRange r
            where r.assessment.id = :aid
                    and r.id <> :excludeId
                    and not(:newMax < r.minScore or :newMin > r.maxScore)
        """)
    long countOverlapsExcludingId(
            @Param("aid") Long assessmentId,
            @Param("excludeId") Long excludeId,
            @Param("newMin") int newMin,
            @Param("newMax") int newMax
    );

}

