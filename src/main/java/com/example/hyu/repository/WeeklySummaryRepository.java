package com.example.hyu.repository;

import com.example.hyu.entity.WeeklySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface WeeklySummaryRepository extends JpaRepository<WeeklySummary, Long> {
    boolean existsByUserIdAndWeekStart(Long userId, LocalDate weekStart);
}
