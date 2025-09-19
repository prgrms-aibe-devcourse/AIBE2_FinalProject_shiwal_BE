package com.example.hyu.dto.checkin;

import java.time.LocalDate;
import java.util.Map;

public record CheckinStatsResponse(
        int streak,
        int totalDays,
        int checkedDays,
        Map<LocalDate, Boolean> days // 날짜별 체크 여부(연속 달력 만들기 쉬움)
) {}
