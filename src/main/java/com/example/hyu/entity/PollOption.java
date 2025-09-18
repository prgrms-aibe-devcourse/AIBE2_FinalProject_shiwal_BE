package com.example.hyu.entity;

// 투표 보기
// Poll 에 속하는 개별 선택지
// BINARY 의 경우 서비스에서 자동으로 YES, NO 2개를 생성 권장

import jakarta.persistence.*;
import lombok.*;

/**
 * PollOption (투표 보기)
 * - Poll에 속하는 개별 선택지
 * - BINARY의 경우 서비스에서 자동으로 ["Yes","No"] 등 2개를 생성 권장
 */
@Entity
@Table(
        name = "poll_options",
        indexes = { @Index(name = "idx_poll_options_poll", columnList = "poll_id") }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PollOption extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 투표의 보기인지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    // 보기 내용
    @Column(name = "content", nullable = false, length = 200)
    private String content;

    // 양방향 보조
    void setPoll(Poll poll) {
        this.poll = poll;
    }

}
