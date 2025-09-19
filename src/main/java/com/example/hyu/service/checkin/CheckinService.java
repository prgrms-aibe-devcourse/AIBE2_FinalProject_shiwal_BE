package com.example.hyu.service.checkin;

import com.example.hyu.dto.checkin.CheckinCreateResponse;
import com.example.hyu.dto.checkin.CheckinStatsResponse;
import com.example.hyu.dto.checkin.CheckinTodayResponse;

import java.time.LocalDate;

public interface CheckinService {
    CheckinTodayResponse getToday(Long userId);
    CheckinCreateResponse checkinToday(Long userId);
    CheckinStatsResponse getStats(Long userId, LocalDate from, LocalDate to);
}