package com.example.hyu.service.goal;

import com.example.hyu.dto.SliceResponse;
import com.example.hyu.dto.goal.GoalRequest;
import com.example.hyu.dto.goal.GoalResponse;
import com.example.hyu.dto.goal.GoalCheckinResponse;
import com.example.hyu.entity.Goal;
import com.example.hyu.entity.GoalCheckin;
import com.example.hyu.repository.goal.GoalCheckinRepository;
import com.example.hyu.repository.goal.GoalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalService {

    private final GoalRepository goalRepository;
    private final GoalCheckinRepository checkinRepository;

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    // ===============================
    // 목표 CRUD
    // ===============================

    /** 목표 생성 */
    @Transactional
    public GoalResponse createGoal(Long userId, GoalRequest dto) {
        Goal goal = Goal.builder()
                .userId(userId)
                .title(dto.title())
                .description(dto.description())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .alertEnabled(Boolean.TRUE.equals(dto.alertEnabled()))
                .deleted(false)
                .build();

        return GoalResponse.fromEntity(goalRepository.save(goal));
    }

    /** 목표 단건 조회 */
    public GoalResponse getGoal(Long goalId, Long userId) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedFalse(goalId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found"));
        return GoalResponse.fromEntity(goal);
    }

    /** 내 목표 전체 조회 */
    public SliceResponse<GoalResponse> getGoals(Long userId, int page, int size) {
        LocalDate today = LocalDate.now(ZONE_SEOUL);
        Pageable pageable = PageRequest.of(page, size);
        var slice = goalRepository
                .findSliceForList(userId, today, pageable) // 상태 기반 정렬 쿼리 호출
                .map(GoalResponse::fromEntity);
        return SliceResponse.from(slice);
    }

    /** 목표 수정 */
    @Transactional
    public GoalResponse updateGoal(Long goalId, Long userId, GoalRequest dto) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedFalse(goalId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found"));

        goal.updateBasic(dto.title(), dto.description(), dto.startDate(), dto.endDate(), dto.alertEnabled());
        return GoalResponse.fromEntity(goal);
    }

    /** 목표 소프트삭제 */
    @Transactional
    public void deleteGoal(Long goalId, Long userId) {
        int updated = goalRepository.softDeleteByIdAndUser(goalId, userId);
        if (updated == 0) {
            throw new EntityNotFoundException("Goal not found or already deleted");
        }
    }

    /** 알림 토글 */
    @Transactional
    public GoalResponse toggleAlert(Long goalId, Long userId, boolean enabled) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedFalse(goalId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found"));
        goal.updateBasic(null, null, null, null, enabled);
        return GoalResponse.fromEntity(goal);
    }

    // ===============================
    // 체크인
    // ===============================

    /** 오늘 체크 */
    @Transactional
    public void checkToday(Long goalId, Long userId) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedFalse(goalId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found"));

        LocalDate today = LocalDate.now(ZONE_SEOUL);
        if (today.isBefore(goal.getStartDate()) || today.isAfter(goal.getEndDate())) {
            throw new IllegalStateException("기간 외에는 체크 불가");
        }

        boolean exists = checkinRepository.existsByGoal_IdAndCheckinDate(goalId, today);
        if (!exists) {
            checkinRepository.saveAndFlush(
                    GoalCheckin.builder()
                            .goal(goal)
                            .checkinDate(today)
                            .build()
            );
        }
    }

    /** 오늘 체크 취소 */
    @Transactional
    public void uncheckToday(Long goalId, Long userId) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedFalse(goalId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found"));

        LocalDate today = LocalDate.now(ZONE_SEOUL);
        checkinRepository.deleteByGoal_IdAndCheckinDate(goalId, today);
    }

    /** 주간/월간 기록 조회 */
    public List<GoalCheckinResponse> getCheckins(Long goalId, Long userId, LocalDate from, LocalDate to) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedFalse(goalId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found"));

        return checkinRepository.findAllByGoalIdAndCheckinDateBetween(goal.getId(), from, to).stream()
                .map(GoalCheckinResponse::fromEntity)
                .toList();
    }
}