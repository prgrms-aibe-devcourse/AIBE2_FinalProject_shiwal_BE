package com.example.hyu.controller.checkin;

import com.example.hyu.dto.checkin.CheckinCreateResponse;
import com.example.hyu.dto.checkin.CheckinStatsResponse;
import com.example.hyu.dto.checkin.CheckinTodayResponse;
import com.example.hyu.security.AuthPrincipal;
import com.example.hyu.service.checkin.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

@RestController
@RequiredArgsConstructor
@RequestMapping("/checkins")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class CheckinController {

    private final CheckinService checkinService;

    /** 로그인 직후: 오늘 체크 여부 + 모달 띄울지 */
    @GetMapping("/today")
    public CheckinTodayResponse today(@AuthenticationPrincipal AuthPrincipal me) {
        return checkinService.getToday(me.getUserId());
    }

    /** 출석체크(같은 날 여러번 호출해도 안전) */
    @PostMapping
    public CheckinCreateResponse create(@AuthenticationPrincipal AuthPrincipal me) {
        return checkinService.checkinToday(me.getUserId());
    }

    /** 임의 기간 통계 */
    @GetMapping("/stats")
    public CheckinStatsResponse stats(
            @AuthenticationPrincipal AuthPrincipal me,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return checkinService.getStats(me.getUserId(), from, to);
    }

    /** 월 단위 통계(프로필에서 이번 달 요약 표시용) */
    @GetMapping("/stats/month")
    public CheckinStatsResponse monthStats(
            @AuthenticationPrincipal AuthPrincipal me,
            @RequestParam(required = false) String month // "YYYY-MM", 없으면 이번 달
    ) {
        ZoneId ZONE = ZoneId.of("Asia/Seoul");
        YearMonth ym = (month == null || month.isBlank())
                ? YearMonth.from(LocalDate.now(ZONE))
                : YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();
        return checkinService.getStats(me.getUserId(), start, end);
    }
}