package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * 커뮤니티 게시글 엔티티 (실무형 Soft Delete 적용)
 * - 기본 조회 시 삭제글 제외(@Where)
 * - delete() 호출 시 실제 삭제 대신 is_deleted = true 로 마킹(@SQLDelete)
 * - 조회수/제목/내용 변경을 위한 편의 메서드 제공
 */
@Entity
@Table(
        name = "community_posts",
        indexes = {
                @Index(name = "idx_posts_created_at", columnList = "created_at"),
                @Index(name = "idx_posts_user_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_posts_isdeleted_created_at", columnList = "is_deleted, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@SQLDelete(sql = "UPDATE posts SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class CommunityPost extends BaseTimeEntity {

    /** PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 작성자: 인증/권한 체크용, 지연 로딩 권장 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users author;

    /** 제목: 1~150자 권장 */
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    /** 본문: TEXT 컬럼으로 저장 */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 조회수: 단건 조회 시 증가, 중복 방지 로직은 서비스/캐시 레이어에서 처리 */
    @Column(name = "view_count", nullable = false)
    private int viewCount;

    /** 좋아요 수: 좋아요 기능 붙일 때 사용 */
    @Column(name = "like_count", nullable = false)
    private int likeCount;

    /** Soft Delete 플래그 */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    /** 익명 여부 */
    @Column(name = "is_anonymous", nullable = false)
    private boolean isAnonymous;


    /* ========= 편의 메서드 (도메인 규칙 캡슐화) ========= */

    /** 제목/본문 수정 (검증은 서비스/Validator에서 선행 권장) */
    public void update(String newTitle, String newContent) {
        if (newTitle != null && !newTitle.isBlank()) this.title = newTitle;
        if (newContent != null && !newContent.isBlank()) this.content = newContent;
    }

    /** 조회수 +1 (중복 방지 키는 서비스/캐시에서) */
    public void increaseViewCount() {
        this.viewCount += 1;
    }

    /** 좋아요 수 증감(토글 등에서 사용) */
    public void increaseLikeCount() {
        this.likeCount += 1;
    }
    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount -= 1;
    }

    /** 소프트 삭제(명시적 호출이 필요할 때). 일반 delete()는 @SQLDelete에 의해 soft 처리됨 */
    public void softDelete() {
        this.deleted = true;
    }

    /** 작성자 검증(서비스에서 호출) */
    public boolean isAuthor(Long userId) {
        return this.author != null && this.author.getId().equals(userId);
    }

    /** 익명 여부 변경 */
    public void changeAnonymous(boolean anonymous) {
        this.isAnonymous = anonymous;
    }

}