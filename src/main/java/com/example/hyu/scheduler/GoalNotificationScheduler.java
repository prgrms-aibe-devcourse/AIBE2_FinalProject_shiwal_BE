package com.example.hyu.scheduler;

import com.example.hyu.entity.Goal;
import com.example.hyu.entity.Notification;
import com.example.hyu.enums.NotificationType;
import com.example.hyu.repository.goal.GoalRepository;
import com.example.hyu.repository.goal.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoalNotificationScheduler {

    private final GoalRepository goalRepository;
    private final NotificationRepository notificationRepository;

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    // 매일 밤 9시 실행
    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Seoul")
    public void runDaily() {
        LocalDate today = LocalDate.now(ZONE_SEOUL);

        // 오늘 기간 내 목표인데 체크 안 하고 알림 아직 안 된 목표 조회
        List<Goal> missedGoals = goalRepository.findMissedTodayForAll(today);
        int created  = 0;

        for(Goal g : missedGoals) {
            Notification n = Notification.builder()
                    .userId(g.getUserId())
                    .goalId(g.getId())
                    .type(NotificationType.MISSED_DAILY)
                    .title("오늘 목표를 체크하지 않았어요")
                    .body("목표 \"" + g.getTitle() + "\"가 오늘 체크되지 않았습니다 !")
                    .eventDate(today)
                    .read(false)
                    .build();

            try {
                notificationRepository.save(n);  // 유니크 제약으로 중복 방지
                created++;
            } catch(DataIntegrityViolationException ignore) {
                // 동시성으로 중복 시 조용히 무시
                log.debug("Duplicate notification ignoredL user={}, goal={}, date={}", g.getUserId(), g.getId(), today);
            }
        }
        log.info("[GoalNotificationScheduler] {} notifications created for {}", created, today);
    }

}
