package com.example.hyu.repository.community;

import com.example.hyu.entity.CommunityPostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, Long> {
    // 특정 사용자가 특정 게시글에 좋아요 했는지 여부 확인
    // 토글 로직에서 이미 좋아요를 눌렀는지 판단할 때 사용
    boolean existsByUserIdAndPostId(Long userId, Long PostId);

    // 특정 사용자가 특정 게시글 좋아요 기록을 삭제
    // 좋아요 취소 시 호출
    void deleteByUserIdAndPostId(Long userId, Long postId);

    // 특정 게시글의 좋아요 개수 조회
    // 화면에 좋아요 수 표시할 떄 사용
    long countByPostId(Long postId);
}
