package com.example.hyu.repository;

import com.example.hyu.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    Optional<Assessment> findByCodeAndStatus(String code, Assessment.Status status);

    // 사용자 목록/검색용 (서비스에서 씀)
    List<Assessment> findByStatus(Assessment.Status status);

    List<Assessment> findByStatusAndCategoryContaining(
            Assessment.Status status, String category);

    List<Assessment> findByStatusAndNameContainingIgnoreCase(
            Assessment.Status status, String name);

    List<Assessment> findByStatusAndCategoryContainingAndNameContainingIgnoreCase(
            Assessment.Status status, String category, String name);

}