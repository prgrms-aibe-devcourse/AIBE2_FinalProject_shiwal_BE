package com.example.hyu.repository.assessment;

import com.example.hyu.entity.AssessmentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface AssessmentAnswerRepository extends JpaRepository<AssessmentAnswer, Long> {

    //한 제출의 모든 답변
    List<AssessmentAnswer> findBySubmissionId(Long submissionId);

    //문항 순서대로
    List<AssessmentAnswer> findBySubmissionIdOrderByQuestionOrderNoAsc(Long submissionId);

    //N+1 방지 + 문항 순서 정렬(fetch join)
    @Query("""
            select a
            from AssessmentAnswer a
            join fetch a.question q
            where a.submission.id = :submissionId
            order by q.orderNo asc
            """)
    List<AssessmentAnswer> findFetchJoinWithQuestionBySubmissionId(@Param("submissionId") Long submissionId);

    // upsert/검증 편의 : 동일(submission, question) 단건 조회/존재 확인
    Optional<AssessmentAnswer> findBySubmissionIdAndQuestionId(Long submissionId, Long questionId);
    boolean existsBySubmissionIdAndQuestionId(Long submissionId, Long questionId);

    // 합계 점수 계산 : 역문항 반영해서 db에 바로 합산
    @Query("""
            select coalesce(
                       sum(case when q.reverseScore = true then (3 - a.selectedValue) else a.selectedValue end)
                            ,0)
            from AssessmentAnswer a
            join a.question q
            where a.submission.id = :submissionId
           """)
    Integer sumTotalScore(@Param("submissionId") Long submissionId);

    long countBySubmissionId(Long submissionId);

    void deleteBySubmissionId(Long submissionId);



}

