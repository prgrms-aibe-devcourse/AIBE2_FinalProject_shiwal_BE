// src/main/java/com/example/hyu/service/AdminReportService.java
package com.example.hyu.service;

import com.example.hyu.dto.admin.ReportDetailResponse;
import com.example.hyu.dto.admin.ReportSearchCond;
import com.example.hyu.dto.admin.ReportSummaryResponse;
import com.example.hyu.dto.admin.ReportUpdateRequest;
import com.example.hyu.entity.Report;
import com.example.hyu.repository.ReportQueryRepository;
import com.example.hyu.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final ReportQueryRepository queryRepository;
    private final ReportRepository reportRepository;

    /* 리스트 */
    @Transactional(readOnly = true)
    public Page<ReportSummaryResponse> list(ReportSearchCond cond, Pageable pageable) {
        return queryRepository.search(cond, pageable)
                .map(this::toSummary);
    }

    /* 상세 */
    @Transactional(readOnly = true)
    public ReportDetailResponse get(Long id) {
        Report r = reportRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("REPORT_NOT_FOUND"));
        return toDetail(r);
    }


    @Transactional
    public ReportDetailResponse update(Long id, ReportUpdateRequest req, Long adminId) {
        Report r = reportRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("REPORT_NOT_FOUND"));

        if (req.status() != null && !req.status().isBlank()) {
            String normalized = req.status().trim().toUpperCase();
            r.setStatus(Report.Status.valueOf(normalized));
        }
        if (req.note() != null) {                 // ✅ 관리자 메모 반영
            r.setAdminNote(req.note().trim());
        }
        r.setLastReviewedAt(Instant.now());

        return toDetail(r);
    }


    /* ===== 매핑 ===== */
    private ReportSummaryResponse toSummary(Report r) {
        return new ReportSummaryResponse(
                r.getId(),
                r.getReportedAt(),
                r.getReporterId(),
                r.getTargetType().name(),
                r.getTargetId(),
                r.getReason().name(),
                r.getStatus().name(),
                r.getLastReviewedAt()             // ✅ 변경
        );
    }

    private ReportDetailResponse toDetail(Report r) {
        return new ReportDetailResponse(
                r.getId(),
                r.getReportedAt(),
                r.getReporterId(),
                r.getTargetType().name(),
                r.getTargetId(),
                r.getReason().name(),
                r.getStatus().name(),
                r.getDescription(),
                r.getAttachmentUrl(),             // ✅ 첨부 포함
                r.getLastReviewedAt(),            // ✅ 변경
                r.getAdminNote()                  // ✅ 메모 포함
        );
    }
}