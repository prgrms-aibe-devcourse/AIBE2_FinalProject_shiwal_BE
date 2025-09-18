package com.example.hyu.repository.community;

import com.example.hyu.entity.CommunityCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityCommentLikeRepository extends JpaRepository<CommunityCommentLike, Long> {
    // 특정 사용자가 특정 댓글에 좋아요 했는지 여부 확인
    // 댓글 좋아요 토글 로직에 사용
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    // 특정 사용자가 특정 댓글의 좋아요 기록 삭제
    // 좋아요 취소 시 호출
    void deleteByUserIdAndCommentId(Long userId, Long commentId);

    // 특정 댓글의 좋아요 개수 조회
    // 화면에서 좋아요 수 표시용
    long countByCommentId(Long commentId);
}
