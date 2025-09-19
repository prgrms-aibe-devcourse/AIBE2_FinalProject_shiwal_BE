package com.example.hyu.controller.community;

// 좋아요 API

import com.example.hyu.dto.community.LikeCountResponse;
import com.example.hyu.dto.community.LikeToggleResponse;
import com.example.hyu.security.AuthPrincipal;
import com.example.hyu.service.community.CommunityLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/community-posts")
@RequiredArgsConstructor
public class CommunityLikeController {
    private final CommunityLikeService likeService;

    /** 게시글 좋아요 */

    //게시글 좋아요 토글 (로그인 필요)
    @PostMapping("/{postId}/likes/toggle")
    public LikeToggleResponse togglePostLike(@PathVariable Long postId,
                                             @AuthenticationPrincipal AuthPrincipal me) {
        return likeService.togglePostLike(me.getUserId(), postId);
    }

    // 게시글 좋아요 수 조회
    @GetMapping("/{postId}/likes/count")
    public LikeCountResponse getPostLikeCount(@PathVariable Long postId) {
        return likeService.getPostLikeCount(postId);
    }


    /** 댓글 좋아요 */

    // 댓글 좋아요 토글 (로그인 필요)
    @PostMapping("/{postId}/comments/{commentId}/likes/toggle")
    public LikeToggleResponse toggleCommentLike(@PathVariable Long postId,
                                                @PathVariable Long commentId,
                                                @AuthenticationPrincipal AuthPrincipal me) {
        return likeService.toggleCommentLike(me.getUserId(), commentId);
    }

    // 댓글 좋아요 수 조회 (공개)
    @GetMapping("/{postId}/comments/{commentId}/likes/count")
    public LikeCountResponse getCommentLikeCount(@PathVariable Long postId,
                                                 @PathVariable Long commentId) {
        return likeService.getCommentLikeCount(commentId);
    }
}



