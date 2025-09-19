package com.example.hyu.service.kpi;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;

@Service
@RequiredArgsConstructor
public class MetricsJob {

    private final JdbcTemplate jdbc;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /* =========================
       매일 00:15 KST 실행
       ========================= */
    @Scheduled(cron = "0 15 0 * * *", zone = "Asia/Seoul")
    public void runDailyJobs() {
        LocalDate todayKst = LocalDate.now(KST);

        // 지연 반영 위해 최근 3일 재집계
        for (int i = 1; i <= 3; i++) {
            LocalDate day = todayKst.minusDays(i);
            computeDaily(day);
            computeRetentionForTargetDay(day);
        }

        // 월/연 재집계 (당월/전월, 당해/전해)
        computeMonthly(todayKst.withDayOfMonth(1));
        computeMonthly(todayKst.minusMonths(1).withDayOfMonth(1));
        computeYearly(todayKst.getYear());
        computeYearly(todayKst.getYear() - 1);
    }

    /* =========================
       DAILY (하루 한 줄 업데이트)
       ========================= */
    public void computeDaily(LocalDate kstDay) {
        UtcRange r = dayRangeToUtc(kstDay);

        long newSignups = ql("""
          SELECT COUNT(*) FROM users u
           WHERE u.created_at >= ? AND u.created_at < ?
        """, r.s, r.e);

        long dau = ql("""
          SELECT COUNT(DISTINCT e.user_id) FROM events e
           WHERE e.status='ok'
             AND e.event_time >= ? AND e.event_time < ?
        """, r.s, r.e);

        long aiActive = ql("""
          SELECT COUNT(DISTINCT e.user_id) FROM events e
           WHERE e.status='ok' AND e.event_name='ai_chat_user_message'
             AND e.event_time >= ? AND e.event_time < ?
        """, r.s, r.e);

        long mild = riskCount("mild", r);
        long moderate = riskCount("moderate", r);
        long risk = riskCount("risk", r);
        long highRisk = riskCount("high_risk", r);

        long checkins = ql("""
          SELECT COUNT(*) FROM events e
           WHERE e.status='ok' AND e.event_name='self_assessment_completed'
             AND e.event_time >= ? AND e.event_time < ?
        """, r.s, r.e);

        // 평균 세션 길이는 준비되면 계산, 지금은 null
        Long avgSessionLen = null;

        upsertDailyWide(kstDay, dau, newSignups, aiActive,
                mild, moderate, risk, highRisk,
                checkins, avgSessionLen);
    }

    private long riskCount(String level, UtcRange r) {
        return ql("""
          SELECT COUNT(*) FROM events e
           WHERE e.status='ok'
             AND e.event_name='risk_detected'
             AND e.level=?
             AND e.event_time >= ? AND e.event_time < ?
        """, level, r.s, r.e);
    }

    private void upsertDailyWide(
            LocalDate day,
            long dau, long newSignups, long aiActive,
            long mild, long moderate, long risk, long highRisk,
            long checkins, Long avgSessionLenSeconds
    ) {
        jdbc.update("""
          INSERT INTO metrics_daily(
            day, daily_active_users, new_signups, ai_active_users,
            mild_event_count, moderate_event_count, risk_event_count, high_risk_event_count,
            checkin_count, avg_session_length_seconds, computed_at
          ) VALUES (?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)
          ON DUPLICATE KEY UPDATE
            daily_active_users=VALUES(daily_active_users),
            new_signups=VALUES(new_signups),
            ai_active_users=VALUES(ai_active_users),
            mild_event_count=VALUES(mild_event_count),
            moderate_event_count=VALUES(moderate_event_count),
            risk_event_count=VALUES(risk_event_count),
            high_risk_event_count=VALUES(high_risk_event_count),
            checkin_count=VALUES(checkin_count),
            avg_session_length_seconds=VALUES(avg_session_length_seconds),
            computed_at=CURRENT_TIMESTAMP
        """, day, dau, newSignups, aiActive,
                mild, moderate, risk, highRisk,
                checkins, avgSessionLenSeconds);
    }

    /* =========================
       MONTHLY (월 한 줄 업데이트)
       ========================= */
    public void computeMonthly(LocalDate monthStartKst) {
        LocalDate next = monthStartKst.plusMonths(1);
        Instant s = monthStartKst.atStartOfDay(KST).toInstant();
        Instant e = next.atStartOfDay(KST).toInstant();

        long mau = ql("""
          SELECT COUNT(DISTINCT e.user_id) FROM events e
           WHERE e.status='ok'
             AND e.event_time >= ? AND e.event_time < ?
        """, s, e);

        long monthlyNew = ql("""
          SELECT COUNT(*) FROM users u
           WHERE u.created_at >= ? AND u.created_at < ?
        """, s, e);

        long aiActiveUsers = ql("""
          SELECT COUNT(DISTINCT e.user_id) FROM events e
           WHERE e.status='ok' AND e.event_name='ai_chat_user_message'
             AND e.event_time >= ? AND e.event_time < ?
        """, s, e);

        long mild = qlRisk("mild", s, e);
        long moderate = qlRisk("moderate", s, e);
        long risk = qlRisk("risk", s, e);
        long highRisk = qlRisk("high_risk", s, e);

        long monthlyCheckins = ql("""
          SELECT COUNT(*) FROM events e
           WHERE e.status='ok' AND e.event_name='self_assessment_completed'
             AND e.event_time >= ? AND e.event_time < ?
        """, s, e);

        BigDecimal avgSec = null;

        upsertMonthlyWide(monthStartKst, mau, monthlyNew, aiActiveUsers,
                mild, moderate, risk, highRisk, monthlyCheckins, avgSec);
    }

    private long qlRisk(String level, Instant s, Instant e) {
        return ql("""
          SELECT COUNT(*) FROM events e
           WHERE e.status='ok'
             AND e.event_name='risk_detected'
             AND e.level=?
             AND e.event_time >= ? AND e.event_time < ?
        """, level, s, e);
    }

    private void upsertMonthlyWide(
            LocalDate monthStart,
            long mau, long newSignups, long aiActiveUsers,
            long mild, long moderate, long risk, long highRisk,
            long monthlyCheckins, BigDecimal avgSessionLenSeconds
    ) {
        jdbc.update("""
          INSERT INTO metrics_monthly(
            month, monthly_active_users, monthly_new_signups, monthly_ai_active_users,
            mild_event_count, moderate_event_count, risk_event_count, high_risk_event_count,
            monthly_checkin_count, avg_session_length_seconds, computed_at
          ) VALUES (?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)
          ON DUPLICATE KEY UPDATE
            monthly_active_users=VALUES(monthly_active_users),
            monthly_new_signups=VALUES(monthly_new_signups),
            monthly_ai_active_users=VALUES(monthly_ai_active_users),
            mild_event_count=VALUES(mild_event_count),
            moderate_event_count=VALUES(moderate_event_count),
            risk_event_count=VALUES(risk_event_count),
            high_risk_event_count=VALUES(high_risk_event_count),
            monthly_checkin_count=VALUES(monthly_checkin_count),
            avg_session_length_seconds=VALUES(avg_session_length_seconds),
            computed_at=CURRENT_TIMESTAMP
        """, monthStart, mau, newSignups, aiActiveUsers,
                mild, moderate, risk, highRisk,
                monthlyCheckins, avgSessionLenSeconds);
    }

    /* =========================
       YEARLY (연 한 줄 업데이트)
       ========================= */
    public void computeYearly(int year) {
        Instant s = LocalDate.of(year,1,1).atStartOfDay(KST).toInstant();
        Instant e = LocalDate.of(year+1,1,1).atStartOfDay(KST).toInstant();

        long yau = ql("""
          SELECT COUNT(DISTINCT e.user_id) FROM events e
           WHERE e.status='ok'
             AND e.event_time >= ? AND e.event_time < ?
        """, s, e);

        long yearlyNew = ql("""
          SELECT COUNT(*) FROM users u
           WHERE u.created_at >= ? AND u.created_at < ?
        """, s, e);

        long aiActiveUsers = ql("""
          SELECT COUNT(DISTINCT e.user_id) FROM events e
           WHERE e.status='ok' AND e.event_name='ai_chat_user_message'
             AND e.event_time >= ? AND e.event_time < ?
        """, s, e);

        long mild = qlRisk("mild", s, e);
        long moderate = qlRisk("moderate", s, e);
        long risk = qlRisk("risk", s, e);
        long highRisk = qlRisk("high_risk", s, e);

        long yearlyCheckins = ql("""
          SELECT COUNT(*) FROM events e
           WHERE e.status='ok' AND e.event_name='self_assessment_completed'
             AND e.event_time >= ? AND e.event_time < ?
        """, s, e);

        BigDecimal avgSec = null;

        upsertYearlyWide(year, yau, yearlyNew, aiActiveUsers,
                mild, moderate, risk, highRisk,
                yearlyCheckins, avgSec);
    }

    private void upsertYearlyWide(
            int year,
            long yau, long newSignups, long aiActiveUsers,
            long mild, long moderate, long risk, long highRisk,
            long yearlyCheckins, BigDecimal avgSessionLenSeconds
    ) {
        jdbc.update("""
          INSERT INTO metrics_yearly(
            year, yearly_active_users, yearly_new_signups, yearly_ai_active_users,
            mild_event_count, moderate_event_count, risk_event_count, high_risk_event_count,
            yearly_checkin_count, avg_session_length_seconds, computed_at
          ) VALUES (?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)
          ON DUPLICATE KEY UPDATE
            yearly_active_users=VALUES(yearly_active_users),
            yearly_new_signups=VALUES(yearly_new_signups),
            yearly_ai_active_users=VALUES(yearly_ai_active_users),
            mild_event_count=VALUES(mild_event_count),
            moderate_event_count=VALUES(moderate_event_count),
            risk_event_count=VALUES(risk_event_count),
            high_risk_event_count=VALUES(high_risk_event_count),
            yearly_checkin_count=VALUES(yearly_checkin_count),
            avg_session_length_seconds=VALUES(avg_session_length_seconds),
            computed_at=CURRENT_TIMESTAMP
        """, year, yau, newSignups, aiActiveUsers,
                mild, moderate, risk, highRisk,
                yearlyCheckins, avgSessionLenSeconds);
    }

    /* =========================
       RETENTION (D1/D7/D30)
       ========================= */
    public void computeRetentionForTargetDay(LocalDate targetKstDay) {
        retentionWindow(targetKstDay.minusDays(1),  targetKstDay, "D1");
        retentionWindow(targetKstDay.minusDays(7),  targetKstDay, "D7");
        retentionWindow(targetKstDay.minusDays(30), targetKstDay, "D30");
    }

    private void retentionWindow(LocalDate cohortDayKst, LocalDate returnedDayKst, String window) {
        UtcRange cohortR = dayRangeToUtc(cohortDayKst);
        long total = ql("""
          SELECT COUNT(*) FROM users u
           WHERE u.created_at >= ? AND u.created_at < ?
        """, cohortR.s, cohortR.e);

        if (total == 0) {
            upsertRetention(cohortDayKst, window, 0, 0);
            return;
        }

        UtcRange retR = dayRangeToUtc(returnedDayKst);
        long returned = ql("""
          SELECT COUNT(DISTINCT e.user_id) FROM events e
           WHERE e.status='ok'
             AND e.event_time >= ? AND e.event_time < ?
             AND e.user_id IN (
                SELECT u.id FROM users u
                 WHERE u.created_at >= ? AND u.created_at < ?
             )
        """, retR.s, retR.e, cohortR.s, cohortR.e);

        upsertRetention(cohortDayKst, window, total, returned);
    }

    private void upsertRetention(LocalDate cohortDay, String window, long total, long returned) {
        BigDecimal rate = (total == 0)
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(returned * 100.0 / total).setScale(2, RoundingMode.HALF_UP);

        jdbc.update("""
          INSERT INTO metrics_retention(
            cohort_day, ret_window, users_total, users_returned, rate, computed_at
          ) VALUES (?,?,?,?,?,CURRENT_TIMESTAMP)
          ON DUPLICATE KEY UPDATE
            users_total=VALUES(users_total),
            users_returned=VALUES(users_returned),
            rate=VALUES(rate),
            computed_at=CURRENT_TIMESTAMP
        """, cohortDay, window, total, returned, rate);
    }

    /* =========================
       공통 헬퍼
       ========================= */
    private record UtcRange(Instant s, Instant e) {}

    private UtcRange dayRangeToUtc(LocalDate kstDay) {
        Instant s = kstDay.atStartOfDay(KST).toInstant();
        Instant e = kstDay.plusDays(1).atStartOfDay(KST).toInstant();
        return new UtcRange(s, e);
        // WHERE event_time >= s AND event_time < e  (반닫힌 구간)
    }

    private long ql(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, args, Long.class);
        return v == null ? 0L : v;
    }
}