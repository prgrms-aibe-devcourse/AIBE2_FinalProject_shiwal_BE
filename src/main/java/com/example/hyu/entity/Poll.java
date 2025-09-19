package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Poll (투표 본문)
 * - 특정 게시글에 1개 붙는 투표
 * - 이진형(BINARY) / 단일선택(SINGLE)만 지원 (multi-select 미지원)
 * - 마감(deadline)은 Instant(UTC). null이면 생성 시 +24h로 기본 설정.
 * - Soft delete 적용.
 */
@Entity
@Table(
        name = "polls",
        uniqueConstraints = {
            // 같은 게시글에는 하나의 투표만 허용
                @UniqueConstraint(name = "uq_post_poll", columnNames = {"post_id"})
        },
        indexes = {
                @Index(name = "idx_polls_post", columnList = "post_id"),
                @Index(name = "idx_polls_deadline", columnList = "deadline")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@SQLDelete(sql = "UPDATE polls SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Poll extends BaseTimeEntity {

    /** 투표 유형: 이진(BINARY) / 단일선택(SINGLE) */
    public enum PollType { BINARY, SINGLE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 투표가 달린 게시글 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    /** 투표 유형 (이진/단일) */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PollType type;

    /** 질문 문구 */
    @Column(name = "question", nullable = false, length = 255)
    private String question;

    /** 마감 시각(UTC). null 입력 시 +24h로 설정해 사용할 것(서비스에서 resolve). */
    @Column(name = "deadline", nullable = false)
    private Instant deadline;

    /** Soft delete 플래그 */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /** 옵션 목록 (보기들) */
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PollOption> options = new ArrayList<>();

    /* ====== 편의 메서드 ====== */

    /** 마감 여부 */
    public boolean isExpired() {
        return Instant.now().isAfter(this.deadline);
    }

    /** deadline 기본값 처리: null이면 생성 시점 기준 +24시간 */
    public static Instant resolveDeadline(Instant input) {
        return (input != null) ? input : Instant.now().plusSeconds(24 * 3600);
    }

    /** 양방향 연관관계 동기화 */
    public void addOption(PollOption option) {
        if (this.options == null) {
            this.options = new ArrayList<>();
        }
        this.options.add(option);
        option.setPoll(this);
    }
}