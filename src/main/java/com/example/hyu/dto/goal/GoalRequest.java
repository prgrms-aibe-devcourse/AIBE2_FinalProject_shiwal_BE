package com.example.hyu.dto.goal;

import java.time.LocalDate;

// 목표 생성/수정 요청 DTO
public record GoalRequest (
        String title,  // 제목
        String description,  // 설명
        LocalDate startDate,  // 기간 시작
        LocalDate endDate,  // 기간 종료
        Boolean alertEnabled  // 알림 설정 여부
){}
