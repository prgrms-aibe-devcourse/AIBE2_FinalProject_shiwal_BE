package com.example.hyu.entity;


import jakarta.persistence.*;
import lombok.*;

/**
 * PollVote (투표 응답)
 * - 내부적으로만 사용 (중복 방지 + 집계용)
 * - API에서는 노출하지 않음
 */
@Entity
@Table(
        name = "poll_votes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_vote_user_poll", columnNames = {"user_id", "poll_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PollVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 누가 투표했는지 (집계 외부 노출 X) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /** 어떤 투표에 대한 응답인지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    /** 어떤 보기를 골랐는지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private PollOption option;
}
