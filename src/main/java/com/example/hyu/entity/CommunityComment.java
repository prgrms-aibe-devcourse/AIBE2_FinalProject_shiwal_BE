package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(
        name = "community_comments",
        indexes = {
                @Index(name = "idx_cc_post_created", columnList = "post_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@SQLDelete(sql = "UPDATE community_comments SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class CommunityComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 게시글 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    /** 작성자 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users author;

    /** 댓글 내용 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 익명 여부 */
    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous;

    /** 좋아요 수: 좋아요 기능 붙일 때 사용 */
    @Column(name = "like_count", nullable = false)
    private int likeCount;

    /** soft delete 플래그 */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    // === 편의 메서드 ===
    // 댓글 수정/삭제 시 권한 체크
    public boolean isAuthor(Long userId) {
        return author != null && author.getId().equals(userId);
    }

    // 댓글 수정 로직 캡슐화
    public void update(String newContent, Boolean newAnonymous) {
        if (newContent != null && !newContent.isBlank()) this.content = newContent;
        if (newAnonymous != null) this.anonymous = newAnonymous;
    }

    // 소프트 삭제 명시적 호출
    public void softDelete() {
        this.deleted = true;
    }

    /** 좋아요 수 증감(토글 등에서 사용) */
    public void increaseLikeCount() {
        this.likeCount += 1;
    }
    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount -= 1;
    }
}