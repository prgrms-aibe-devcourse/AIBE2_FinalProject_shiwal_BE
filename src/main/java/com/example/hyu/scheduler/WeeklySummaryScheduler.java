package com.example.hyu.scheduler;

import com.example.hyu.entity.Profile;
import com.example.hyu.entity.WeeklySummary;
import com.example.hyu.notification.NotificationSender;
import com.example.hyu.repository.CheckinRepository;
import com.example.hyu.repository.ProfileRepository;
import com.example.hyu.repository.WeeklySummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklySummaryScheduler {

    private final ProfileRepository profileRepo;
    private final CheckinRepository checkinRepo;
    private final WeeklySummaryRepository weeklyRepo;
    private final NotificationSender notifier;

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    /** 매주 월요일 09:00 KST에 지난 주 요약 생성/전송 */
    @Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Seoul")
    public void run() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate weekStart = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<Profile> targets = profileRepo.findAll().stream()
                .filter(Profile::isWeeklySummary) // 주간 요약 수신 동의
                .toList();

        for (Profile p : targets) {
            Long userId = p.getUserId();
            try {
                if (weeklyRepo.existsByUserIdAndWeekStart(userId, weekStart)) {
                    continue; // 이미 발송됨
                }
                int checkedDays = checkinRepo
                        .findAllByUserIdAndDateBetweenOrderByDateAsc(userId, weekStart, weekEnd)
                        .size();

                String content = String.format(
                        "지난주 출석: %d/7일. 계속 화이팅이에요! (%s ~ %s)",
                        checkedDays, weekStart, weekEnd
                );

                weeklyRepo.save(WeeklySummary.builder()
                        .userId(userId)
                        .weekStart(weekStart)
                        .weekEnd(weekEnd)
                        .content(content)
                        .createdAt(Instant.now())
                        .build());

                notifier.sendInApp(userId, "주간 요약", content);
            } catch (Exception e) {
                log.warn("WeeklySummary fail userId={} weekStart={}: {}", userId, weekStart, e.getMessage());
            }
        }
    }
}
