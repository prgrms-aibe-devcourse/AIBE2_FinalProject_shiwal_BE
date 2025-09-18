package com.example.hyu.repository.community;

import com.example.hyu.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    // 특정 유저가 이미 해당 투표에 참여했는지 확인
    Optional<PollVote> findByUserIdAndPollId(Long userId, Long pollId);

    // 특정 투표에 대한 옵션별 투표 수 집계
    // 결과 [optionId, count]
    @Query("SELECT v.option.id, COUNT(v) " +
           "FROM PollVote v " +
            "WHERE v.poll.id = :pollId " +
            "GROUP BY v.option.id")
    List<Object[]> countVotesByPollGrouped(Long pollId);

}
