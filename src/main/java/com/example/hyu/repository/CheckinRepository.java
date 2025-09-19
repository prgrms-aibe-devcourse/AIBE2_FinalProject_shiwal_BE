package com.example.hyu.repository;

import com.example.hyu.entity.Checkin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CheckinRepository extends JpaRepository<Checkin, Long> {
    boolean existsByUserIdAndDate(Long userId, LocalDate date);
    Optional<Checkin> findByUserIdAndDate(Long userId, LocalDate date);
    List<Checkin> findAllByUserIdAndDateBetweenOrderByDateAsc(Long userId, LocalDate from, LocalDate to);
}
