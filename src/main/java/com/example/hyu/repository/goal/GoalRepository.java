package com.example.hyu.repository.goal;

import com.example.hyu.entity.Goal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
    Page<Goal> findAllByUserIdAndDeletedFalse(Long userId, Pageable pageable);

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

    // 전 유저 대상 : 오늘 기간 안에 있지만 체크 안 했고 알림이 켜져 있지만 오늘 날짜로 미이행 알림이 아직 안만들어진 목표도ㅠㅜ
    @Query("""
        select g from Goal g
         where g.deleted = false
           and :today between g.startDate and g.endDate
           and g.alertEnabled = true
           and not exists (
                select 1 from GoalCheckin c
                 where c.goal.id = g.id
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
                 where c.goal.id = g.id
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



    // ------------ 목표 리스트 무한 스크롤

    // 목표 리스트 무한 스크롤 지원
    // 정렬 규칙 : 0 -> 오늘 아직 체크 안 한 목표(맨 위)
    // 1 -> 오늘 체크한 목표 ( 그 아래)
    // 2 -> 기간이 종료된 목표 (맨 아래)
    @Query(
            value = """
        select g from Goal g
         where g.userId = :userId
           and g.deleted = false
         order by
           case
             when :today > g.endDate then 2  
             when exists (
               select 1 from GoalCheckin c
                where c.goal = g and c.checkinDate = :today
             ) then 1                        
             else 0                          
           end asc,
           g.endDate asc,    
           g.createdAt desc, 
           g.id desc         
      """
    )
    Slice<Goal> findSliceForList(@Param("userId") Long userId,
                                 @Param("today") LocalDate today,
                                 Pageable pageable);
}