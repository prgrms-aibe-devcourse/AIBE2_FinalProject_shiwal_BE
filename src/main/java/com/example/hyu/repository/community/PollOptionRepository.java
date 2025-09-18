package com.example.hyu.repository.community;

import com.example.hyu.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {
    // 특정 투표에 속한 옵션 목록 조회
    List<PollOption> findByPollId(Long pollId);
}
