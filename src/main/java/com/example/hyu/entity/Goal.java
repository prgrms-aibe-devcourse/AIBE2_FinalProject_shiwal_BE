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
        name = "goals",
        indexes = {
                @Index(name = "idx_goals_user_del_end", columnList = "user_id,is_deleted,end_date")
        }
)
public class Goal extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 목표 소유자 ID

    @Column(nullable = false, length = 100)
    private String title; // 제목

    @Column(columnDefinition = "TEXT")
    private String description; // 설명

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // 기간 시작

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate; // 기간 종료

    @Column(name = "alert_enabled", nullable = false)
    private boolean alertEnabled; // 알림 설정 여부

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;  // 소프트 삭제

    // 편의 메서드

    // 오늘이 endDate 이후인지 체크 -> 종료 여부 계산
    public boolean isEnded(LocalDate today) {
        return today.isAfter(endDate);
    }

    // 제목/설명/기간/알림 여부만 수정
    public void updateBasic(String title, String description, LocalDate startDate, LocalDate endDate, Boolean alertEnabled) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }
        if (alertEnabled != null) {
            this.alertEnabled = alertEnabled;
        }
    }

    // 소프트 삭제 처리
    public void softDelete() { this.deleted = true;}
}