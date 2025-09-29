// backend/src/main/java/com/example/hyu/repository/CheckinRepository.java
package com.example.hyu.repository;

import com.example.hyu.entity.Checkin;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CheckinRepository extends JpaRepository<Checkin, Long> {

    // ── 기본 존재/조회 ──
    boolean existsByUserIdAndDate(Long userId, LocalDate date);

    Optional<Checkin> findByUserIdAndDate(Long userId, LocalDate date);

    List<Checkin> findAllByUserIdAndDateBetweenOrderByDateAsc(Long userId, LocalDate from, LocalDate to);

    // ── 통계용(빠른 카운트) ──
    long countByUserIdAndDateBetween(Long userId, LocalDate from, LocalDate to);

    // ── 스트릭 최적화: 앵커 날짜부터 과거로 내림차순 날짜만 뽑기 ──
    @Query("""
           select c.date
             from Checkin c
            where c.userId = :userId
              and c.date <= :anchor
            order by c.date desc
           """)
    List<LocalDate> findDatesDescFromAnchor(Long userId, LocalDate anchor, Pageable pageable);

    // ── 월간/기간 요약 시 날짜만 필요한 경우(네트워크 전송량↓) ──
    @Query("""
           select c.date
             from Checkin c
            where c.userId = :userId
              and c.date between :from and :to
            order by c.date asc
           """)
    List<LocalDate> findDatesBetweenAsc(Long userId, LocalDate from, LocalDate to);
}