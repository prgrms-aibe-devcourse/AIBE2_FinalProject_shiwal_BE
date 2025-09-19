package com.example.hyu.repository.Assessment;

import com.example.hyu.entity.Assessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long>, JpaSpecificationExecutor<Assessment> {

    // --- 사용자/일반 조회 (삭제 제외) ---
    Optional<Assessment> findByCodeAndStatus(String code, Assessment.Status status);
    List<Assessment> findByStatus(Assessment.Status status);
    Page<Assessment> findByStatus(Assessment.Status status, Pageable pageable);
    Page<Assessment> findByStatusAndNameContainingIgnoreCase(Assessment.Status status, String name, Pageable pageable);
    Page<Assessment> findByStatusAndCategoryContainingAndNameContainingIgnoreCase(Assessment.Status status, String category, String name, Pageable pageable);

    // --- 코드 중복/단건 조회 (삭제 포함) ---
    @Query(value = "SELECT COUNT(*) FROM assessments WHERE code = :code", nativeQuery = true)
    long countAnyByCode(@Param("code") String code);

    @Query(value = "SELECT * FROM assessments WHERE code = :code LIMIT 1", nativeQuery = true)
    Optional<Assessment> findAnyByCode(@Param("code") String code);

    // --- 관리자 전용: 삭제 포함 조회 ---
    @Query(value = "SELECT * FROM assessments WHERE id = :id", nativeQuery = true)
    Optional<Assessment> findAnyById(@Param("id") Long id);

    @Query(
            value = """
            SELECT * 
              FROM assessments a
             ORDER BY a.created_at DESC, a.id DESC
            """,
            countQuery = "SELECT COUNT(*) FROM assessments",
            nativeQuery = true
    )
    Page<Assessment> findAllIncludingDeleted(Pageable pageable);

    // (수정용) 자기 자신 제외 + '미삭제'만 대상으로 코드 중복 검사
    @Query(value = """
            SELECT COUNT(*)
              FROM assessments
             WHERE code = :code
               AND id <> :id
               AND is_deleted = false
            """, nativeQuery = true)
    long countActiveByCodeExcludingId(@Param("code") String code, @Param("id") Long id);

}