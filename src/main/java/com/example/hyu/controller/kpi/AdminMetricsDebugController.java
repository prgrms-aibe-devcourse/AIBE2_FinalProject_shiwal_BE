package com.example.hyu.controller.kpi;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminMetricsDebugController {
    private final com.example.hyu.service.kpi.MetricsJob job;

    @GetMapping("/api/admin/metrics/debug/recompute")
    public String recompute() {
        var KST = java.time.ZoneId.of("Asia/Seoul");
        var today = java.time.LocalDate.now(KST);
        // 어제 기준 재집계 (오늘 이벤트면 today도 추가로 돌려)
        job.computeDaily(today);
        job.computeRetentionForTargetDay(today);
        job.computeMonthly(today.withDayOfMonth(1));
        job.computeYearly(today.getYear());
        return "ok";
    }
}
