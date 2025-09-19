package com.example.hyu.repository.goal;

import com.example.hyu.entity.GoalCheckin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface GoalCheckinRepository extends JpaRepository<GoalCheckin, Long> {

    // ---------오늘 체크 여부 판정에 쓰는 핵심 메서드---------

    //해당 목표가 특정 날짜에 체크되었는지 여부 (오늘 체크 판단용)
    boolean existsByGoalIdAndCheckinDate(Long goalId, LocalDate checkinDate);

    // 체크 취소 (실수 복구) - 특정 날짜의 체크인  레코드 삭제
    void deleteByGoalIdAndCheckinDate(Long goalId, LocalDate checkinDate);


    // --------- 조회/ 집계에 편한 보조 메서드

    // 주간/월간 진행도 표시용 : 날짜 범위의 체크인 전부 조회
    List<GoalCheckin> findAllByGoalIdAndCheckinDateBetween(Long goalId, LocalDate from, LocalDate to);

    // 리스트 최적화용 : 오늘 체크된 목표들을 한 번에 가져오기
    @Query("""
            select c.goal.id
            from GoalCheckin c
            where c.goal.userId = :userId
            and c.goal.deleted = false
            and c.checkinDate = :today
            """)
    List<Long> findCheckedGoalIdsOfUserOnDate(Long userId, LocalDate today);

    // 여러 goalId에 대해 특정 날짜 체크 여부를 한 번에 조회
    @Query("""
            select c.goal.id
            from GoalCheckin c
            where c.goal.id in :goalIds
            and c.checkinDate = :date
            """)
    List<Long> findCheckedGoalIdsIn(Collection<Long> goalIds, LocalDate date);
}
