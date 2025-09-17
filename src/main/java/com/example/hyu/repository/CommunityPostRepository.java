package com.example.hyu.repository;

import com.example.hyu.entity.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    /**
     * 제목이나 본문에 검색어가 포함된 게시글 목록 조회
     * (is_deleted = false 조건은 엔티티 @Where 로 이미 필터링됨)
     */
    Page<CommunityPost> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            String titleKeyword,
            String contentKeyword,
            Pageable pageable
    );

    /**
     * 특정 사용자가 작성한 게시글 목록 조회
     */
    Page<CommunityPost> findByAuthorId(Long authorId, Pageable pageable);
}
