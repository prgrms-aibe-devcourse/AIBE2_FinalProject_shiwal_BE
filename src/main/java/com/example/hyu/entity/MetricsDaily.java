package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity
@Table(name = "metrics_daily")
public class MetricsDaily {

    @Id
    @Column(name = "day", nullable = false)
    private LocalDate day;
    // PK: 해당 날짜 (yyyy-MM-dd), 하루 단위 집계 기준일

    @Column(name = "daily_active_users")
    private Long dailyActiveUsers;
    // 하루 동안 로그인/활동한 고유 사용자 수

    @Column(name = "new_signups")
    private Long newSignups;
    // 하루 동안 새로 가입한 사용자 수

    @Column(name = "ai_active_users")
    private Long aiActiveUsers;
    // 하루 동안 AI 상담을 이용한 고유 사용자 수

    @Column(name = "avg_session_length_seconds")
    private Long avgSessionLengthSeconds;
    // 하루 동안 평균 세션 길이(초), 없으면 null

    // 위험 이벤트 단계별 발생 건수
    @Column(name = "mild_event_count")
    private Long mildEventCount;

    @Column(name = "moderate_event_count")
    private Long moderateEventCount;

    @Column(name = "risk_event_count")
    private Long riskEventCount;

    @Column(name = "high_risk_event_count")
    private Long highRiskEventCount;

    @Column(name = "checkin_count")
    private Long checkinCount;
    // 하루 동안 자가진단 완료 건수 등 체크인 수

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
    // 집계가 생성된 시각 (배치가 돌 때 기록)
}