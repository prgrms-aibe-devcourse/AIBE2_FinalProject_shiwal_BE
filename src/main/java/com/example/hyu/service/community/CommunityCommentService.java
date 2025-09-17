package com.example.hyu.service.community;

import com.example.hyu.dto.community.CommentCreateRequest;
import com.example.hyu.dto.community.CommentResponse;
import com.example.hyu.dto.community.CommentUpdateRequest;
import com.example.hyu.entity.CommunityComment;
import com.example.hyu.entity.CommunityPost;
import com.example.hyu.entity.Users;
import com.example.hyu.repository.community.CommunityCommentRepository;
import com.example.hyu.repository.community.CommunityPostRepository;
import com.example.hyu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityCommentService {

    private final CommunityCommentRepository commentRepo;
    private final CommunityPostRepository postRepo;
    private final UserRepository userRepo;

    // 댓글 생성
    @Transactional
    public Long create(Long authorId, Long postId, CommentCreateRequest req) {
        Users author = userRepo.findById(authorId)
                .orElseThrow(()-> new RuntimeException("USER_NOT_FOUND"));
        CommunityPost post = postRepo.findById(postId)
                .orElseThrow(()-> new RuntimeException("POST_NOT_FOUND"));

        CommunityComment comment = CommunityComment.builder()
                .author(author)
                .post(post)
                .content(req.content())
                .anonymous(req.isAnonymous())
                .deleted(false)
                .build();

        return commentRepo.save(comment).getId();
    }

    // 댓글 목록 조회
    public List<CommentResponse> getList(Long postId) {
        return commentRepo.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(this::toResponse)
                .toList();
    }

    // 댓글 수정
    @Transactional
    public void update(Long commentId, Long editorId, boolean isAdmin, CommentUpdateRequest req) {
        CommunityComment comment = commentRepo.findById(commentId)
                .orElseThrow(()-> new RuntimeException("COMMENT_NOT_FOUND"));

        // 작성자 OR 관리자만 수정 가능
        if(!comment.isAuthor(editorId) && !isAdmin) {
            throw new RuntimeException("FORBIDDEN_NOT_OWNER");
        }

        comment.update(req.content(), req.isAnonymous());
    }

    // 댓글 삭제
    @Transactional
    public void delete(Long commentId, Long requesterId, boolean isAdmin) {
        CommunityComment comment = commentRepo.findById(commentId)
                .orElseThrow(()-> new RuntimeException("COMMENT_NOT_FOUND"));

        if(!comment.isAuthor(requesterId) && !isAdmin) {
            throw new RuntimeException("FORBIDDEN_NOT_OWNER");
        }

        commentRepo.delete(comment); // @SQLDelete 적용으로 soft delete 처리됨
    }

    // 내부 변환 유틸
    private CommentResponse toResponse(CommunityComment c) {
        String nickname = c.isAnonymous() ? "익명" : c.getAuthor().getNickname();
        Long authorId = c.isAnonymous() ? null : c.getAuthor().getId();

        return new CommentResponse (
                c.getId(),
                authorId,
                nickname,
                c.getContent(),
                c.isAnonymous(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }


}
