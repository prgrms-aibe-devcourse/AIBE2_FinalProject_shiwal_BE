package com.example.hyu.dto.goal;

import com.example.hyu.entity.GoalCheckin;

import java.time.LocalDate;

// 체크인 조회 응답
public record GoalCheckinResponse (
        Long checkinId,
        Long goalId,  // 어떤 goal에 대한 체크인인지 연결하기 위한 id
        LocalDate checkinDate  // 체크한 날짜
){
    public static GoalCheckinResponse fromEntity(GoalCheckin checkin) {
        return new GoalCheckinResponse(
                checkin.getId(),
                checkin.getGoal().getId(),
                checkin.getCheckinDate()
        );
    }
}
