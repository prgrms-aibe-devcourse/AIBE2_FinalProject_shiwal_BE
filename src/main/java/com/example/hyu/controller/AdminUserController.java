package com.example.hyu.controller;

import com.example.hyu.dto.admin.user.ChangeStateRequest;
import com.example.hyu.dto.admin.user.PasswordResetIssueResponse;
import com.example.hyu.dto.admin.user.UserSearchCond;
import com.example.hyu.dto.admin.user.UserSummaryResponse;
import com.example.hyu.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 사용자 목록 조회 (관리자/유저 필터 포함)
     * 예:
     *  GET /api/admin/users?q=kim&role=ADMIN&page=0&size=20&sort=createdAt,desc
     *  GET /api/admin/users?joinedFrom=2025-01-01&joinedTo=2025-12-31
     */
    @GetMapping
    public Page<UserSummaryResponse> list(
            UserSearchCond cond,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return adminUserService.list(cond, pageable);
    }

    @PatchMapping("/{id}/state")
    public UserSummaryResponse changeState(
            @PathVariable Long id,
            @RequestBody ChangeStateRequest req
    ) {
        return adminUserService.changeState(id, req);
    }

    @PostMapping("/{id}/password-reset")
    public PasswordResetIssueResponse issuePasswordReset(@PathVariable Long id) {
        return adminUserService.issuePasswordReset(id);
    }
}