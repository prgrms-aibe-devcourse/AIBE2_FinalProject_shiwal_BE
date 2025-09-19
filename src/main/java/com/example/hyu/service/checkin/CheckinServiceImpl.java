package com.example.hyu.service.checkin;

import com.example.hyu.dto.checkin.CheckinCreateResponse;
import com.example.hyu.dto.checkin.CheckinStatsResponse;
import com.example.hyu.dto.checkin.CheckinTodayResponse;
import com.example.hyu.entity.Checkin;
import com.example.hyu.repository.CheckinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {

    private final CheckinRepository checkinRepo;

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private LocalDate today() { return LocalDate.now(ZONE); }
    private Instant now() { return Instant.now(); }

    @Override
    @Transactional(readOnly = true)
    public CheckinTodayResponse getToday(Long userId) {
        LocalDate today = today();
        boolean checked = checkinRepo.existsByUserIdAndDate(userId, today);
        int streak = computeStreak(userId, today);
        boolean shouldPrompt = !checked; // 필요 시: 리마인더 정책 추가 가능
        return new CheckinTodayResponse(checked, shouldPrompt, today, streak);
    }

    @Override
    @Transactional
    public CheckinCreateResponse checkinToday(Long userId) {
        LocalDate today = today();
        boolean exists = checkinRepo.existsByUserIdAndDate(userId, today);
        if (!exists) {
            checkinRepo.save(Checkin.builder()
                    .userId(userId)
                    .date(today)
                    .createdAt(now())
                    .build());
        }
        int streak = computeStreak(userId, today);
        return new CheckinCreateResponse(!exists, today, streak);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckinStatsResponse getStats(Long userId, LocalDate from, LocalDate to) {
        List<Checkin> list = checkinRepo.findAllByUserIdAndDateBetweenOrderByDateAsc(userId, from, to);

        Map<LocalDate, Boolean> map = new LinkedHashMap<>();
        LocalDate d = from;
        while (!d.isAfter(to)) {
            map.put(d, Boolean.FALSE);
            d = d.plusDays(1);
        }
        for (Checkin c : list) {
            map.put(c.getDate(), Boolean.TRUE);
        }

        int checkedDays = (int) map.values().stream().filter(Boolean::booleanValue).count();
        int streak = computeStreak(userId, today());
        return new CheckinStatsResponse(streak, map.size(), checkedDays, map);
    }

    /** 오늘을 끝점으로 뒤로 가며 연속 출석 계산 */
    private int computeStreak(Long userId, LocalDate anchor) {
        int s = 0;
        LocalDate cur = anchor;
        while (true) {
            boolean exists = checkinRepo.existsByUserIdAndDate(userId, cur);
            if (!exists) break;
            s++;
            cur = cur.minusDays(1);
        }
        return s;
    }
}
