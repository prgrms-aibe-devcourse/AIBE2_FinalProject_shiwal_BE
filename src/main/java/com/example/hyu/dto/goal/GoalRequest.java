package com.example.hyu.dto.goal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// 목표 생성/수정 요청 DTO
public record GoalRequest (
        @NotBlank String title,  // 제목
        @Size(max = 1000)String description,  // 설명
        @NotNull LocalDate startDate,  // 기간 시작
        @NotNull LocalDate endDate,  // 기간 종료
        Boolean alertEnabled  // 알림 설정 여부
){}
