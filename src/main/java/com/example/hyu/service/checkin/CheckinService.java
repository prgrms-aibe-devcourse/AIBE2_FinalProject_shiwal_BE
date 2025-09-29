package com.example.hyu.service.checkin;

import com.example.hyu.dto.checkin.CheckinCreateRequest;
import com.example.hyu.dto.checkin.CheckinCreateResponse;
import com.example.hyu.dto.checkin.CheckinStatsResponse;
import com.example.hyu.dto.checkin.CheckinTodayResponse;

import java.time.LocalDate;

public interface CheckinService {
    CheckinTodayResponse getToday(Long userId);

    // ✅ 바디(JSON) 받는 시그니처로 변경
    CheckinCreateResponse checkinToday(Long userId, CheckinCreateRequest body);

    CheckinStatsResponse getStats(Long userId, LocalDate from, LocalDate to);

    // (선택) 하위호환이 필요하면 기본 메서드로 유지 가능
    default CheckinCreateResponse checkinToday(Long userId) {
        return checkinToday(userId, new CheckinCreateRequest(
                null, null, null, null, null, null, null, null,
                null, null, null, null
        ));
    }
}