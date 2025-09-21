package com.example.hyu.dto.goal;

import com.example.hyu.entity.Goal;

import java.time.LocalDate;

// 목표 조회 응답
public record GoalResponse (
        Long id,
        String title,  // 제목
        String description,  // 설명
        LocalDate startDate,  // 기간 시작
        LocalDate endDate, // 기간 종료
        boolean alertEnabled, // 알림 설정 여부
        boolean deleted
){
    public static GoalResponse fromEntity(Goal goal) {
        return new GoalResponse (
                goal.getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getStartDate(),
                goal.getEndDate(),
                goal.isAlertEnabled(),
                goal.isDeleted()
        );
    }
}
