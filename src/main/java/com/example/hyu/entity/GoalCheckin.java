package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "goal_checkins",
        uniqueConstraints = {
                // 하나의 목표는 하루에 한 번만 체크 가능
                @UniqueConstraint(name = "uq_goal_date", columnNames = {"goal_id", "checkin_date"})
        },
        indexes = {
                // 목표별 체크 기록 빠르게 조회
                @Index(name = "idx_checkins_goal", columnList = "goal_id")
        }
)
public class GoalCheckin extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "checkin_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal; // 체크 대상 목표

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate; //체크한 날짜(오늘 체크 여부 판단용)


    // 편의 메서드

    // 오늘 체크인지 확인
    public boolean isToday(LocalDate today) {
        return this.checkinDate.equals(today);
    }
}
