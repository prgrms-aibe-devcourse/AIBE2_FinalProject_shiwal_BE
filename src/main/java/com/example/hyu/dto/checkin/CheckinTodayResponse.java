package com.example.hyu.dto.checkin;

import java.time.LocalDate;

public record CheckinTodayResponse(
        boolean checked,       // 오늘 체크인 여부
        boolean shouldPrompt,  // 로그인 직후 모달/페이지 안내 여부
        LocalDate date,        // 오늘 날짜(KST)
        int streak             // 현재 연속 출석(오늘 포함)
) {}
