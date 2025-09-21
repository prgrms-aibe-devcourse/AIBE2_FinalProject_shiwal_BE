package com.example.hyu.controller;

import com.example.hyu.dto.SliceResponse;
import com.example.hyu.dto.goal.GoalRequest;
import com.example.hyu.dto.goal.GoalResponse;
import com.example.hyu.dto.goal.GoalCheckinResponse;
import com.example.hyu.service.goal.GoalService;
import com.example.hyu.security.AuthPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    // ===============================
    // 목표 CRUD
    // ===============================

    /** 목표 생성 */
    @PostMapping
    public GoalResponse createGoal(@AuthenticationPrincipal AuthPrincipal user,
                                   @RequestBody @Valid GoalRequest dto) {
        return goalService.createGoal(user.getUserId(), dto);
    }

    /** 내 목표 전체 조회 */
    @GetMapping
    public SliceResponse<GoalResponse> getGoals(@AuthenticationPrincipal AuthPrincipal user,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return goalService.getGoals(user.getUserId(), page, size);
    }

    /** 목표 단건 상세 조회 */
    @GetMapping("/{goalId}")
    public GoalResponse getGoalDetail(@AuthenticationPrincipal AuthPrincipal user,
                                      @PathVariable Long goalId) {
        return goalService.getGoal(goalId, user.getUserId());
    }

    /** 목표 수정 */
    @PutMapping("/{goalId}")
    public GoalResponse updateGoal(@AuthenticationPrincipal AuthPrincipal user,
                                   @PathVariable Long goalId,
                                   @RequestBody @Valid GoalRequest dto) {
        return goalService.updateGoal(goalId, user.getUserId(), dto);
    }

    /** 목표 삭제 (소프트 삭제) */
    @DeleteMapping("/{goalId}")
    public void deleteGoal(@AuthenticationPrincipal AuthPrincipal user,
                           @PathVariable Long goalId) {
        goalService.deleteGoal(goalId, user.getUserId());
    }

    /** 알림 토글 */
    @PatchMapping("/{goalId}/alert")
    public GoalResponse toggleAlert(@AuthenticationPrincipal AuthPrincipal user,
                                    @PathVariable Long goalId,
                                    @RequestParam boolean enabled) {
        return goalService.toggleAlert(goalId, user.getUserId(), enabled);
    }

    // ===============================
    // 체크인
    // ===============================

    /** 오늘 체크 */
    @PostMapping("/{goalId}/checkin")
    public void checkToday(@AuthenticationPrincipal AuthPrincipal user,
                           @PathVariable Long goalId) {
        goalService.checkToday(goalId, user.getUserId());
    }

    /** 오늘 체크 취소 */
    @DeleteMapping("/{goalId}/checkin")
    public void uncheckToday(@AuthenticationPrincipal AuthPrincipal user,
                             @PathVariable Long goalId) {
        goalService.uncheckToday(goalId, user.getUserId());
    }

    /** 주간/월간 기록 조회 */
    @GetMapping("/{goalId}/checkins")
    public List<GoalCheckinResponse> getCheckins(@AuthenticationPrincipal AuthPrincipal user,
                                                 @PathVariable Long goalId,
                                                 @RequestParam LocalDate from,
                                                 @RequestParam LocalDate to) {
        return goalService.getCheckins(goalId, user.getUserId(), from, to);
    }
}