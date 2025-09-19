package com.example.hyu.repository.community;

import com.example.hyu.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface PollRepository extends JpaRepository<Poll, Long> {
    // 특정 게시글에 투표가 이미 있는지 확인
    // 게시글 1개당 Poll은 하나만 허용 -> Optional 반환
    Optional<Poll> findByPostId(Long postId);
}
