package com.example.hyu.service.checkin;

import com.example.hyu.dto.checkin.CheckinCreateRequest;
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
import java.util.Optional;

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
        boolean shouldPrompt = !checked;
        return new CheckinTodayResponse(checked, shouldPrompt, today, streak);
    }

    /** 신규면 생성, 있으면 보강 업데이트(멱등) */
    @Override
    @Transactional
    public CheckinCreateResponse checkinToday(Long userId, CheckinCreateRequest body) {
        LocalDate today = today();

        Optional<Checkin> existingOpt = checkinRepo.findByUserIdAndDate(userId, today);
        boolean created;

        if (existingOpt.isEmpty()) {
            Checkin c = Checkin.builder()
                    .userId(userId)
                    .date(today)
                    .createdAt(now())
                    .build();
            applyBody(c, body);
            checkinRepo.save(c);
            created = true;
        } else {
            Checkin c = existingOpt.get();
            applyBody(c, body);
            created = false;
        }

        int streak = computeStreak(userId, today);
        return new CheckinCreateResponse(created, today, streak);
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

    /** 최소 필드만 반영 (null은 무시) */
    private void applyBody(Checkin c, CheckinCreateRequest body) {
        if (body == null) return;

        if (body.mood() != null)   c.setMood(clamp1to5(body.mood()));
        if (body.energy() != null) c.setEnergy(clamp1to5(body.energy()));
        if (body.stress() != null) c.setStress(clamp1to5(body.stress()));

        if (body.note() != null) {
            String note = body.note();
            if (note.length() > 200) note = note.substring(0, 200);
            c.setNote(note);
        }
    }

    private static int clamp1to5(int v) {
        return Math.max(1, Math.min(5, v));
    }
}