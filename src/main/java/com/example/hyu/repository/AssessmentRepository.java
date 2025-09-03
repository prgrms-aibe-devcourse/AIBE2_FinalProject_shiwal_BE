package com.example.hyu.repository;

import com.example.hyu.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 검사 메타
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    Optional<Assessment> findByCodeAndStatus(String code, Assessment.Status status);
}
