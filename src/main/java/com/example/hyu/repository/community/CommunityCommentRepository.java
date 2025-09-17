package com.example.hyu.repository.community;

import com.example.hyu.entity.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {
    // 특정 게시글에 달린 댓글 목록 가져오기
    List<CommunityComment> findByPostIdOrderByCreatedAtAsc(Long postId);
}
