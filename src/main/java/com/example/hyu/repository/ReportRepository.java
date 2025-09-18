package com.example.hyu.repository;

import com.example.hyu.entity.Report;
import org.hibernate.tool.schema.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    // 최근 특정 시점 이후 동일 유저가 같은 대상에 신고한 기록이 있는지 확인
    Optional<Report> findTop1ByReporterIdAndTargetTypeAndTargetIdAndReportedAtAfterOrderByReportedAtDesc(
            Long reporterId, Report.TargetType targetType, Long targetId, Instant since);
}