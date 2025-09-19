package com.example.hyu.repository.goal;

import com.example.hyu.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    // ------- 기본 조회 -------

    // 내 목표 전부(소프트 삭제 제외) 조회
    List<Goal> findAllByUserIdAndDeletedFalse(Long userId);

    // 소유자 검증을 포함한 단건 조회
    Optional<Goal> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    // 빠른 가드 체크 용도
    boolean existsByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    // ------- 소프트 삭제 -------

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Goal g
           set g.deleted = true
         where g.id = :id
           and g.userId = :userId
           and g.deleted = false
    """)
    int softDeleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    // ------- 21시 미이행 알림 대상 조회 -------

    // 전 유저 대상
    @Query("""
        select g from Goal g
         where g.deleted = false
           and :today between g.startDate and g.endDate
           and g.alertEnabled = true
           and not exists (
                select 1 from GoalCheckin c
                 where c.goal = g
                   and c.checkinDate = :today
           )
           and not exists (
                select 1 from Notification n
                 where n.userId = g.userId
                   and n.goalId = g.id
                   and n.type = com.example.hyu.enums.NotificationType.MISSED_DAILY
                   and n.eventDate = :today
           )
    """)
    List<Goal> findMissedTodayForAll(@Param("today") LocalDate today);

    // 특정 유저 대상
    @Query("""
        select g from Goal g
         where g.deleted = false
           and g.userId = :userId
           and :today between g.startDate and g.endDate
           and g.alertEnabled = true
           and not exists (
                select 1 from GoalCheckin c
                 where c.goal = g
                   and c.checkinDate = :today
           )
           and not exists (
                select 1 from Notification n
                 where n.userId = g.userId
                   and n.goalId = g.id
                   and n.type = com.example.hyu.enums.NotificationType.MISSED_DAILY
                   and n.eventDate = :today
           )
    """)
    List<Goal> findMissedTodayForUser(@Param("userId") Long userId,
                                      @Param("today") LocalDate today);
}