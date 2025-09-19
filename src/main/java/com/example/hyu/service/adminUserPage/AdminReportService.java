// src/main/java/com/example/hyu/service/AdminReportService.java
package com.example.hyu.service.adminUserPage;

import com.example.hyu.dto.adminUserPage.ReportDetailResponse;
import com.example.hyu.dto.adminUserPage.ReportSearchCond;
import com.example.hyu.dto.adminUserPage.ReportSummaryResponse;
import com.example.hyu.dto.adminUserPage.ReportUpdateRequest;
import com.example.hyu.entity.Report;
import com.example.hyu.repository.adminUserPage.ReportQueryRepository;
import com.example.hyu.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // 검토 / 조치 업데이트
    @Transactional
    public ReportDetailResponse update(Long id, ReportUpdateRequest req, Long adminId) {
        Report r = reportRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("REPORT_NOT_FOUND"));

        // 상태 처리
        if (req.status() != null && !req.status().isBlank()) {
            String normalized = req.status().trim().toUpperCase();
            switch (normalized) {
                case "REVIEWED" -> r.markReviewed(adminId, req.note());
                case "ACTION_TAKEN" -> r.markActionTaken(adminId, req.note());
                case "PENDING" -> r.setStatus(Report.Status.PENDING); // 되돌리기
                default -> throw new IllegalArgumentException("INVALID_STATUS");
            }
        } else if (req.note() != null) {
            // 상태 변경 없이 메모만 수정 가능
            r.setAdminNote(req.note().trim());
        }
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
                r.getLastReviewedAt()
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