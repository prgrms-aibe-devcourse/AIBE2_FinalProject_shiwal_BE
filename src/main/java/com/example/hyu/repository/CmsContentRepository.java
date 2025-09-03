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

    /**
 * Retrieve a paginated list of CmsContent entries for the given category.
 *
 * Results respect the entity-level soft-delete filter (rows with 삭제여부 = true are excluded).
 *
 * @param category the CmsContent.Category to filter by
 * @param pageable pagination and sorting information
 * @return a page of CmsContent matching the category (empty if none)
 */
    Page<CmsContent> findByCategory(Category category, Pageable pageable);

    /**
     * Admin search for non-deleted CmsContent with optional text and field filters, returning a paginated result.
     *
     * Performs a case-insensitive partial match of `q` against title and text, and optionally filters by
     * category, visibility, and exact groupKey. The entity-level soft-delete filter (@Where clause) excludes
     * deleted rows automatically.
     *
     * @param q         free-text search applied to title and text (partial, case-insensitive); ignored when null or empty
     * @param category  category to filter by; ignored when null
     * @param visibility visibility to filter by; ignored when null
     * @param groupKey  exact group key to filter by; ignored when null or empty
     * @param pageable  pagination and sorting information
     * @return a page of CmsContent matching the provided criteria (empty if none)
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
     * Admin search across CMS contents that can optionally include soft-deleted rows.
     *
     * <p>Performs a case-insensitive substring search on title and text when {@code q} is provided,
     * and can filter by category, visibility, and group key. This method uses a native SQL query
     * (bypassing the entity-level {@code @Where} soft-delete filter) — when {@code includeDeleted}
     * is true results include deleted rows; when false only rows with 삭제여부 = FALSE are returned.
     *
     * @param q            optional full-text query applied to title and text (case-insensitive substring)
     * @param category     optional category filter — pass the enum name as a String (e.g. "MEDITATION")
     * @param visibility   optional visibility filter — pass the enum name as a String (e.g. "PUBLIC")
     * @param groupKey     optional group key; empty or null means no group filtering
     * @param includeDeleted if true include soft-deleted rows; if false exclude them
     * @param pageable     pagination information
     * @return a page of CmsContent matching the provided filters, ordered by created DESC then content id DESC
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
     * Find a CmsContent by its content ID, including soft-deleted rows.
     *
     * This query bypasses the entity-level soft-delete filter and returns a content record regardless of its 삭제여부 value — intended for admin operations such as recovery or auditing.
     *
     * @param id the 콘텐츠 ID of the content to retrieve
     * @return an Optional containing the matching CmsContent, or empty if not found
     */
    @Query(value = "SELECT * FROM cms_contents WHERE `콘텐츠 ID` = :id", nativeQuery = true)
    Optional<CmsContent> findByIdIncludingDeleted(@Param("id") Long id);
}