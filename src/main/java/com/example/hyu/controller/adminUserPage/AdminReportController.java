package com.example.hyu.controller.adminUserPage;

import com.example.hyu.dto.adminUserPage.ReportDetailResponse;
import com.example.hyu.dto.adminUserPage.ReportSearchCond;
import com.example.hyu.dto.adminUserPage.ReportSummaryResponse;
import com.example.hyu.dto.adminUserPage.ReportUpdateRequest;
import com.example.hyu.security.AuthPrincipal;
import com.example.hyu.service.adminUserPage.AdminReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminReportService service;

    // 리스트 (필터: status, reason, targetType, from~to, q)
    @GetMapping
    public Page<ReportSummaryResponse> list(
            ReportSearchCond cond,
            @PageableDefault(size = 20, sort = "reportedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return service.list(cond, pageable);
    }

    // 상세
    @GetMapping("/{id}")
    public ReportDetailResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    // 상태/검토일 업데이트 (B안: 자동정지 없음)
    @PatchMapping("/{id}")
    public ReportDetailResponse update(@PathVariable Long id,
                                       @Valid @RequestBody ReportUpdateRequest req,
                                       @AuthenticationPrincipal AuthPrincipal me) {
        return service.update(id, req, me.getUserId());
    }
}
