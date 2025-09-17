package com.example.hyu.service.community;

import com.example.hyu.dto.PageResponse;
import com.example.hyu.dto.community.CommunityPostCreateRequest;
import com.example.hyu.dto.community.CommunityPostDetailResponse;
import com.example.hyu.dto.community.CommunityPostSummaryResponse;
import com.example.hyu.dto.community.CommunityPostUpdateRequest;
import com.example.hyu.entity.CommunityPost;
import com.example.hyu.entity.Users;
import com.example.hyu.repository.community.CommunityPostRepository;
import com.example.hyu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityPostService {
    private final CommunityPostRepository postRepo;
    private final UserRepository userRepo;

    // 생성
    @Transactional
    public Long create(Long authorId, CommunityPostCreateRequest req) {
        // 작성자 검증
        Users author = userRepo.findById(authorId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
        CommunityPost post = CommunityPost.builder()
                .author(author)
                .title(req.title())
                .content(req.content())
                .viewCount(0)
                .likeCount(0)
                .isAnonymous(req.isAnonymous())
                .deleted(false)
                .build();
        return postRepo.save(post).getId();
    }


    // 단건 조회
    @Transactional
    public CommunityPostDetailResponse getOne(Long postId) {
        CommunityPost p = postRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("POST_NOT_FOUND"));

        // 조회수 증가 (중복 방지는 나중에 캐시/세션에서)
        p.increaseViewCount();

        return toDetailDto(p);
    }

    // 목록 조회
    public PageResponse<CommunityPostSummaryResponse> getList(Pageable pageable, String q, Long authorId) {
        Page<CommunityPost> page;
        if (q != null && !q.isBlank()) {
            page = postRepo.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(q, q, pageable);
        } else if (authorId != null) {
            page = postRepo.findByAuthorId(authorId, pageable);
        } else {
            page = postRepo.findAll(pageable);
        }
        return PageResponse.from(page.map(this::toSummaryDto));
    }

    // 수정
    @Transactional
    public void update(Long postId, Long editorId, boolean isAdmin, CommunityPostUpdateRequest req) {
        CommunityPost post = postRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("POST_NOT_FOUND"));

        checkOwnershipOrAdmin(post, editorId, isAdmin);

        post.update(req.title(), req.content());
        if (req.isAnonymous() != null) {
            post.changeAnonymous(req.isAnonymous());
        }
    }

    // 삭제
    @Transactional
    public void delete(Long postId, Long requesterId, boolean isAdmin) {
        CommunityPost post = postRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("POST_NOT_FOUND"));

        checkOwnershipOrAdmin(post, requesterId, isAdmin);

        //@SQLDelete 적용되어있으니 delete( 호출만 해도 soft delete 처리
        post.softDelete();
    }


    // 내부 유틸
    private void checkOwnershipOrAdmin(CommunityPost post, Long userId, boolean isAdmin) {
        if (!post.isAuthor(userId) && !isAdmin) {
            throw new RuntimeException("FORBIDDEN_NOT_OWNER");
        }
    }

    private CommunityPostSummaryResponse toSummaryDto(CommunityPost p) {
        var author = p.isAnonymous()
                ? new CommunityPostSummaryResponse.AuthorSummary(null, "익명")
                : new CommunityPostSummaryResponse.AuthorSummary(
                p.getAuthor().getId(),
                p.getAuthor().getNickname()
        );
        return new CommunityPostSummaryResponse(
                p.getId(),
                p.getTitle(),
                author,
                p.isAnonymous(),
                p.getCreatedAt()
        );
    }

    private CommunityPostDetailResponse toDetailDto(CommunityPost p) {
        var author = p.isAnonymous()
                ? new CommunityPostDetailResponse.AuthorSummary(null, "익명")
                : new CommunityPostDetailResponse.AuthorSummary(
                p.getAuthor().getId(),
                p.getAuthor().getNickname()
        );

        return new CommunityPostDetailResponse(
                p.getId(),
                p.getTitle(),
                p.getContent(),
                author,
                p.isAnonymous(),
                p.getViewCount(),
                p.getLikeCount(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

}






















