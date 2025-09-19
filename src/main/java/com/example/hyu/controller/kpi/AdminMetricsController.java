package com.example.hyu.controller.kpi;
import com.example.hyu.dto.kpi.*;
import com.example.hyu.service.kpi.MetricsJob;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * 관리자 대시보드용 조회 API 모음
 * - 데이터 출처: metrics_* 집계 테이블 (MetricsJob이 채움)
 * - 설계 목표: 프론트 차트/카드가 바로 쓸 수 있는 납작한 형태로 반환
 */
@RestController
@RequestMapping("/api/admin/metrics")
@RequiredArgsConstructor
public class AdminMetricsController {

    private final JdbcTemplate jdbc;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final MetricsJob metricsJob;

    /* -------------------------------------------------
     * 1) 일별 KPI (기간 조회)
     *   - 대시보드 하단 '일별/타임라인' 그래프 데이터
     * ------------------------------------------------- */
    @GetMapping("/daily")
    public ResponseEntity<List<DailyMetricsRes>> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        // metrics_daily에서 지정 기간의 일별 KPI 반환
        String sql = """
            SELECT day, daily_active_users, new_signups, ai_active_users,
                   mild_event_count, moderate_event_count, risk_event_count, high_risk_event_count,
                   checkin_count
              FROM metrics_daily
             WHERE day BETWEEN ? AND ?
             ORDER BY day
        """;
        var rows = jdbc.query(sql, (rs, n) -> new DailyMetricsRes(
                rs.getObject("day", LocalDate.class),
                rs.getLong("daily_active_users"),
                rs.getLong("new_signups"),
                rs.getLong("ai_active_users"),
                rs.getLong("mild_event_count"),
                rs.getLong("moderate_event_count"),
                rs.getLong("risk_event_count"),
                rs.getLong("high_risk_event_count"),
                rs.getLong("checkin_count")
        ), from, to);
        return ResponseEntity.ok(rows);
    }

    /* -------------------------------------------------
     * 2) 월별 KPI (기간 조회)
     *   - 월별 방문자/가입자 차트 (막대/선)
     *   - fromMonth/toMonth는 각 월의 1일로 요청하는 걸 권장
     * ------------------------------------------------- */
    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlyMetricsRes>> monthly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromMonth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toMonth
    ) {
        String sql = """
            SELECT month, monthly_active_users, monthly_new_signups, monthly_ai_active_users,
                   mild_event_count, moderate_event_count, risk_event_count, high_risk_event_count,
                   monthly_checkin_count
              FROM metrics_monthly
             WHERE month BETWEEN ? AND ?
             ORDER BY month
        """;
        var rows = jdbc.query(sql, (rs, n) -> new MonthlyMetricsRes(
                rs.getObject("month", LocalDate.class),
                rs.getLong("monthly_active_users"),
                rs.getLong("monthly_new_signups"),
                rs.getLong("monthly_ai_active_users"),
                rs.getLong("mild_event_count"),
                rs.getLong("moderate_event_count"),
                rs.getLong("risk_event_count"),
                rs.getLong("high_risk_event_count"),
                rs.getLong("monthly_checkin_count")
        ), fromMonth, toMonth);
        return ResponseEntity.ok(rows);
    }

    /* -------------------------------------------------
     * 3) 연도별 KPI (범위 조회)
     *   - 연간 요약 차트/표
     * ------------------------------------------------- */
    @GetMapping("/yearly")
    public ResponseEntity<List<YearlyMetricsRes>> yearly(
            @RequestParam int fromYear,
            @RequestParam int toYear
    ) {
        String sql = """
            SELECT year, yearly_active_users, yearly_new_signups, yearly_ai_active_users,
                   mild_event_count, moderate_event_count, risk_event_count, high_risk_event_count,
                   yearly_checkin_count
              FROM metrics_yearly
             WHERE year BETWEEN ? AND ?
             ORDER BY year
        """;
        var rows = jdbc.query(sql, (rs, n) -> new YearlyMetricsRes(
                rs.getInt("year"),
                rs.getLong("yearly_active_users"),
                rs.getLong("yearly_new_signups"),
                rs.getLong("yearly_ai_active_users"),
                rs.getLong("mild_event_count"),
                rs.getLong("moderate_event_count"),
                rs.getLong("risk_event_count"),
                rs.getLong("high_risk_event_count"),
                rs.getLong("yearly_checkin_count")
        ), fromYear, toYear);
        return ResponseEntity.ok(rows);
    }

    /* -------------------------------------------------
     * 4) 위험 타임라인 (high_risk 전용)
     *   - 상단 '위험 감지 타임라인' 막대 그래프 데이터
     * ------------------------------------------------- */
    @GetMapping("/risk-timeline")
    public ResponseEntity<List<RiskTimelinePoint>> riskTimeline(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String sql = """
            SELECT day, high_risk_event_count
              FROM metrics_daily
             WHERE day BETWEEN ? AND ?
             ORDER BY day
        """;
        var rows = jdbc.query(sql, (rs, n) -> new RiskTimelinePoint(
                rs.getObject("day", LocalDate.class),
                rs.getLong("high_risk_event_count")
        ), from, to);
        return ResponseEntity.ok(rows);
    }

    /* -------------------------------------------------
     * 5) 리텐션 (리스트형)
     *   - 코호트(가입일) x 윈도우(D1/D7/D30) 3행 구조
     *   - 그대로 테이블로 뿌리거나, 클라이언트에서 피봇 가능
     * ------------------------------------------------- */
    @GetMapping("/retention")
    public ResponseEntity<List<RetentionRow>> retention(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cohortFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cohortTo
    ) {
        String sql = """
    SELECT cohort_day, ret_window, users_total, users_returned, rate
      FROM metrics_retention
     WHERE cohort_day BETWEEN ? AND ?
     ORDER BY cohort_day
""";
        var rows = jdbc.query(sql, (rs, n) -> new RetentionRow(
                rs.getObject("cohort_day", LocalDate.class),
                rs.getString("ret_window"),
                rs.getLong("users_total"),
                rs.getLong("users_returned"),
                rs.getBigDecimal("rate")
        ), cohortFrom, cohortTo);

        List<String> order = List.of("D1","D7","D30");
        rows.sort(
                Comparator
                        .comparing(RetentionRow::cohortDay)
                        .thenComparing(r -> order.indexOf(r.window()))
        );


        return ResponseEntity.ok(rows);
    }

    /* -------------------------------------------------
     * 6) 리텐션 매트릭스 (피봇된 한 줄 요약)
     *   - 코호트별로 D1/D7/D30이 한 레코드에 모여있는 형태
     *   - 대시보드 표/히트맵에 바로 적합
     * ------------------------------------------------- */
    @GetMapping("/retention/matrix")
    public ResponseEntity<List<RetentionMatrixRow>> retentionMatrix(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cohortFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cohortTo
    ) {
        String sql = """
    SELECT cohort_day, ret_window, users_total, users_returned, rate
      FROM metrics_retention
     WHERE cohort_day BETWEEN ? AND ?
     ORDER BY cohort_day
""";
        // 1) 전 행을 가져온 뒤
        var all = jdbc.query(sql, (rs, n) -> Map.of(
                "cohort",  rs.getObject("cohort_day", LocalDate.class),
                "window",  rs.getString("ret_window"),
                "total",   rs.getLong("users_total"),
                "ret",     rs.getLong("users_returned"),
                "rate",    rs.getBigDecimal("rate")
        ), cohortFrom, cohortTo);

        // 2) cohort_day 기준으로 윈도우별 값 합치기
        Map<LocalDate, RetCell> acc = new TreeMap<>();
        for (var row : all) {
            LocalDate c = (LocalDate) row.get("cohort");
            String w    = (String) row.get("window");
            long total  = ((Number) row.get("total")).longValue();
            long ret    = ((Number) row.get("ret")).longValue();
            BigDecimal rate = (BigDecimal) row.get("rate");

            var cell = acc.computeIfAbsent(c, k -> new RetCell());
            cell.total = total; // 동일 코호트면 동일 total
            switch (w) {
                case "D1"  -> { cell.d1Ret = ret;  cell.d1Rate = rate; }
                case "D7"  -> { cell.d7Ret = ret;  cell.d7Rate = rate; }
                case "D30" -> { cell.d30Ret = ret; cell.d30Rate = rate; }
            }
        }

        // 3) DTO로 변환
        List<RetentionMatrixRow> out = new ArrayList<>();
        acc.forEach((cohort, cell) -> out.add(
                new RetentionMatrixRow(
                        cohort,
                        cell.total,
                        cell.d1Ret,  nz(cell.d1Rate),
                        cell.d7Ret,  nz(cell.d7Rate),
                        cell.d30Ret, nz(cell.d30Rate)
                )
        ));
        return ResponseEntity.ok(out);
    }

    /* -------------------------------------------------
     * 7) 상단 요약 카드
     *   - 기간 내 high_risk 총합
     *   - 소스별 분해(chat / assessment) → meta.source 사용 (없으면 0)
     *   - 기간 내 고유 AI 상담 사용자수 / 자가진단 사용자수
     * ------------------------------------------------- */
    @GetMapping("/summary")
    public ResponseEntity<SummaryRes> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        // KST 00:00 ~ 익일 00:00 을 UTC로 변환
        var s = from.atStartOfDay(KST).toInstant();
        var e = to.plusDays(1).atStartOfDay(KST).toInstant();

        // (1) 기간 전체 high_risk
        long highRiskTotal = jdbc.queryForObject("""
            SELECT COUNT(*) FROM events
             WHERE status='ok'
               AND event_name='risk_detected'
               AND level='high_risk'
               AND event_time>=? AND event_time<?""",
                Long.class, s, e);

        // (2) 소스별 high_risk (meta.source = "chat" / "assessment")
        //  - meta를 아직 안 넣었다면 0으로만 나올 수 있음 (차후 meta.source 넣으면 자동 분해됨)
        long fromChat = jdbc.queryForObject("""
            SELECT COUNT(*) FROM events
             WHERE status='ok'
               AND event_name='risk_detected'
               AND level='high_risk'
               AND JSON_EXTRACT(meta,'$.source')='\"chat\"'
               AND event_time>=? AND event_time<?""",
                Long.class, s, e);

        long fromAssessment = jdbc.queryForObject("""
            SELECT COUNT(*) FROM events
             WHERE status='ok'
               AND event_name='risk_detected'
               AND level='high_risk'
               AND JSON_EXTRACT(meta,'$.source')='\"assessment\"'
               AND event_time>=? AND event_time<?""",
                Long.class, s, e);

        // (3) 기간 내 AI 상담 사용자수(고유)
        long aiActiveUsers = jdbc.queryForObject("""
            SELECT COUNT(DISTINCT user_id) FROM events
             WHERE status='ok'
               AND event_name='ai_chat_user_message'
               AND event_time>=? AND event_time<?""",
                Long.class, s, e);

        // (4) 기간 내 자가진단 완료 사용자수(고유)
        long selfAssessmentUsers = jdbc.queryForObject("""
            SELECT COUNT(DISTINCT user_id) FROM events
             WHERE status='ok'
               AND event_name='self_assessment_completed'
               AND event_time>=? AND event_time<?""",
                Long.class, s, e);

        return ResponseEntity.ok(new SummaryRes(
                nvl(highRiskTotal),
                nvl(fromChat),
                nvl(fromAssessment),
                nvl(aiActiveUsers),
                nvl(selfAssessmentUsers)
        ));
    }

    // 재계산
    // AdminMetricsController.java

    @PostMapping("/recompute/daily")
    public ResponseEntity<String> recomputeDaily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day
    ) {
        metricsJob.computeDaily(day);
        metricsJob.computeRetentionForTargetDay(day);
        return ResponseEntity.ok("Daily recompute done for " + day);
    }

    @PostMapping("/recompute/monthly")
    public ResponseEntity<String> recomputeMonthly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate monthStart
    ) {
        metricsJob.computeMonthly(monthStart);
        return ResponseEntity.ok("Monthly recompute done for " + monthStart);
    }

    @PostMapping("/recompute/yearly")
    public ResponseEntity<String> recomputeYearly(
            @RequestParam int year
    ) {
        metricsJob.computeYearly(year);
        return ResponseEntity.ok("Yearly recompute done for " + year);
    }

    /* ====================== 내부 유틸 ====================== */

    // 리텐션 합치기에 쓰는 임시 컨테이너
    private static final class RetCell {
        long total;
        long d1Ret;  BigDecimal d1Rate;
        long d7Ret;  BigDecimal d7Rate;
        long d30Ret; BigDecimal d30Rate;
    }

    private static long nvl(Long v) { return v == null ? 0L : v; }
    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
