package com.example.hyu.repository.healingContent;

import com.example.hyu.entity.CmsContent;
import com.example.hyu.entity.CmsContent.Category;
import com.example.hyu.entity.CmsContent.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CmsContentRepository extends JpaRepository<CmsContent, Long> {

    // 삭제 제외 JPQL (@Where로 deleted=false 자동 적용)
    @Query("""
        SELECT c
          FROM CmsContent c
         WHERE (:q IS NULL OR :q = ''
                OR LOWER(COALESCE(c.title, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(c.text,  '')) LIKE LOWER(CONCAT('%', :q, '%')))
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

    // 삭제 포함 네이티브 (@Where 우회)
    @Query(value = """
        SELECT *
          FROM cms_contents c
         WHERE ( :q IS NULL OR :q = ''
                 OR LOWER(c.title, '') LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(c.text, '')  LIKE LOWER(CONCAT('%', :q, '%')) )
           AND ( :category   IS NULL OR c.category   = :category )
           AND ( :visibility IS NULL OR c.visibility = :visibility )
           AND ( :groupKey   IS NULL OR :groupKey = '' OR c.group_key = :groupKey )
           AND ( :includeDeleted = TRUE OR c.is_deleted = FALSE )
        ORDER BY c.created_at DESC, c.id DESC
        """,
            countQuery = """
        SELECT COUNT(*)
          FROM cms_contents c
         WHERE ( :q IS NULL OR :q = ''
                 OR LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(c.text)  LIKE LOWER(CONCAT('%', :q, '%')) )
           AND ( :category   IS NULL OR c.category   = :category )
           AND ( :visibility IS NULL OR c.visibility = :visibility )
           AND ( :groupKey   IS NULL OR :groupKey = '' OR c.group_key = :groupKey )
           AND ( :includeDeleted = TRUE OR c.is_deleted = FALSE )
        """,
            nativeQuery = true)
    Page<CmsContent> searchIncludingDeleted(
            @Param("q") String q,
            @Param("category") String category,       // enum 문자열
            @Param("visibility") String visibility,   // enum 문자열
            @Param("groupKey") String groupKey,
            @Param("includeDeleted") Boolean includeDeleted,
            Pageable pageable
    );

    // 삭제 포함 단건 조회
    @Query(value = "SELECT * FROM cms_contents WHERE id = :id", nativeQuery = true)
    Optional<CmsContent> findAnyById(@Param("id") Long id);
}

