package com.example.hyu.repository;

import com.example.hyu.entity.CmsContent;
import com.example.hyu.entity.CmsContent.Category;
import com.example.hyu.entity.CmsContent.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CmsContentRepository extends JpaRepository<CmsContent, Long> {

    // 기본 카테고리 페이징 (삭제여부=false 만 자동으로 조회됨: @Where 덕분)
    Page<CmsContent> findByCategory(Category category, Pageable pageable);

    /**
     * 관리자 기본 검색(삭제되지 않은 것만)
     * - 제목/문구 LIKE
     * - 카테고리/공개범위/그룹키 필터
     * - @Where(clause="삭제여부=false")가 엔티티에 걸려 있으므로 soft-deleted는 자동 제외
     */
    @Query("""
        SELECT c
          FROM CmsContent c
         WHERE (:q IS NULL OR :q = ''
                OR LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.text)  LIKE LOWER(CONCAT('%', :q, '%')))
           AND (:category   IS NULL OR c.category   = :category)
           AND (:visibility IS NULL OR c.visibility = :visibility)
           AND (:groupKey   IS NULL OR :groupKey = '' OR c.groupKey = :groupKey)
        """)
    Page<CmsContent> search(
            @Param("q") String q,
            @Param("category") Category category,
            @Param("visibility") Visibility visibility,
            @Param("groupKey") String groupKey,
            Pageable pageable
    );

    /**
     * 관리자 "삭제 포함 조회" (soft-deleted 포함)
     * - 엔티티의 @Where가 적용되지 않도록 NativeQuery 사용
     * - includeDeleted=true 이면 모두, false면 삭제되지 않은 것만
     */
    @Query(value = """
        SELECT *
          FROM cms_contents c
         WHERE ( :q IS NULL OR :q = ''
                 OR LOWER(c.`제목`) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(c.`문구`) LIKE LOWER(CONCAT('%', :q, '%')) )
           AND ( :category   IS NULL OR c.`카테고리` = :category )
           AND ( :visibility IS NULL OR c.`공개범위` = :visibility )
           AND ( :groupKey   IS NULL OR :groupKey = '' OR c.`그룹` = :groupKey )
           AND ( :includeDeleted = TRUE OR c.`삭제여부` = FALSE )
        ORDER BY c.`생성` DESC, c.`콘텐츠 ID` DESC
        """,
            countQuery = """
        SELECT COUNT(*)
          FROM cms_contents c
         WHERE ( :q IS NULL OR :q = ''
                 OR LOWER(c.`제목`) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(c.`문구`) LIKE LOWER(CONCAT('%', :q, '%')) )
           AND ( :category   IS NULL OR c.`카테고리` = :category )
           AND ( :visibility IS NULL OR c.`공개범위` = :visibility )
           AND ( :groupKey   IS NULL OR :groupKey = '' OR c.`그룹` = :groupKey )
           AND ( :includeDeleted = TRUE OR c.`삭제여부` = FALSE )
        """,
            nativeQuery = true)
    Page<CmsContent> searchIncludingDeleted(
            @Param("q") String q,
            @Param("category") String category,     // Native에 enum 문자열 전달 (e.g. "MEDITATION")
            @Param("visibility") String visibility, // Native에 enum 문자열 전달 (e.g. "PUBLIC")
            @Param("groupKey") String groupKey,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable
    );

    /**
     * 단건 조회(삭제 포함). 복구/감사 등 관리용.
     */
    @Query(value = "SELECT * FROM cms_contents WHERE `콘텐츠 ID` = :id", nativeQuery = true)
    Optional<CmsContent> findByIdIncludingDeleted(@Param("id") Long id);
}