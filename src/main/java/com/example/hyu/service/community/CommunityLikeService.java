package com.example.hyu.service.community;

import com.example.hyu.dto.community.LikeCountResponse;
import com.example.hyu.dto.community.LikeToggleResponse;
import com.example.hyu.entity.*;
import com.example.hyu.repository.UserRepository;
import com.example.hyu.repository.community.CommunityCommentLikeRepository;
import com.example.hyu.repository.community.CommunityCommentRepository;
import com.example.hyu.repository.community.CommunityPostLikeRepository;
import com.example.hyu.repository.community.CommunityPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityLikeService {
    private final CommunityPostRepository postRepo;
    private final CommunityCommentRepository commentRepo;
    private final CommunityPostLikeRepository postLikeRepo;
    private final CommunityCommentLikeRepository commentLikeRepo;
    private final UserRepository userRepo;

    /** 게시글 좋아요 */



    //     게시글 좋아요 토글
    //     (user, post) 레코드가 있으면 → 삭제(좋아요 취소) + 카운트 감소
    //     없으면 → 생성(좋아요) + 카운트 증가
    //     동시성 높아지면 카운트 컬럼 대신 집계/캐시 고려
    @Transactional
    public LikeToggleResponse togglePostLike(Long userId, Long postId) {
        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
        CommunityPost post = postRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("POST_NOT_FOUND"));

        boolean exists = postLikeRepo.existsByUserIdAndPostId(userId, postId);
        if(exists) {
            // 이미 좋아요 -> 취소
            postLikeRepo.deleteByUserIdAndPostId(userId, postId);
            post.decreaseLikeCount();
            return new LikeToggleResponse(false, post.getLikeCount());
        } else {
            // 처음 좋아요 -> 생성
            postLikeRepo.save(CommunityPostLike.builder()
                    .user(user).post(post).build());
            post.increaseLikeCount();
            return new LikeToggleResponse(true, post.getLikeCount());
        }
    }

    // 게시글 좋아요 개수 조회
    public LikeCountResponse getPostLikeCount(Long postId) {
        CommunityPost post = postRepo.findById(postId)
                .orElseThrow(()-> new RuntimeException("POST_NOT_FOUND"));
        return new LikeCountResponse(post.getLikeCount());
    }

    /** 댓글 좋아요 */


    // 댓글 좋아요 토글
    // (user, comment) 레코드가 있으면 삭제(취소), 없으면 생성(좋아요)
    // CommunityComment.likeCount 컬럼을 즉시 반영
    @Transactional
    public LikeToggleResponse toggleCommentLike(Long userId, Long commentId) {
        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
        CommunityComment comment = commentRepo.findById(commentId)
                .orElseThrow(()-> new RuntimeException("COMMENT_NOT_FOUND"));

        boolean exists = commentLikeRepo.existsByUserIdAndCommentId(userId, commentId);
        if(exists) {
            // 이미 좋아요 -> 취소
            commentLikeRepo.deleteByUserIdAndCommentId(userId, commentId);
            comment.decreaseLikeCount();
            return new LikeToggleResponse(false, comment.getLikeCount());
        } else {
            // 처음 좋아요 -> 생성
            commentLikeRepo.save(CommunityCommentLike.builder()
                    .user(user).comment(comment).build());
            comment.increaseLikeCount();
            return new LikeToggleResponse(true, comment.getLikeCount());
        }
    }

    // 댓글 좋아요 개수 조회
    public LikeCountResponse getCommentLikeCount(Long commentId){
        CommunityComment c = commentRepo.findById(commentId)
                .orElseThrow(()-> new RuntimeException("COMMENT_NOT_FOUND"));
        return new LikeCountResponse(c.getLikeCount());
    }


}
