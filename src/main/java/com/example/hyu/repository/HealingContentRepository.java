package com.example.hyu.repository;

import com.example.hyu.entity.CmsContent;
import com.example.hyu.entity.CmsContent.Category;
import com.example.hyu.entity.CmsContent.Visibility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface HealingContentRepository extends JpaRepository<CmsContent, Long> {

    /**
                                     * Retrieves a page of CmsContent matching visibility and publication constraints with optional filters.
                                     *
                                     * Returns content where visibility equals {@code vis}, publishedAt is null or <= {@code now}, and — when provided —
                                     * category and groupKey match. If {@code q} is provided and non-empty, title or text are matched case-insensitively
                                     * for a partial substring. Results are ordered by COALESCE(publishedAt, createdAt) DESC then id DESC and are
                                     * constrained by the supplied {@code pageable}.
                                     *
                                     * @param vis the required content visibility
                                     * @param now upper bound for publishedAt; content with publishedAt in the future is excluded
                                     * @param category optional category filter; pass {@code null} to ignore
                                     * @param groupKey optional group key filter; pass {@code null} or empty string to ignore
                                     * @param q optional search term for title or text; pass {@code null} or empty string to disable text search
                                     * @param pageable pagination information (page size, sort is ignored by the query)
                                     * @return a list of CmsContent matching the filters for the requested page
                                     */
                                    @Query("""
      SELECT c FROM CmsContent c
       WHERE c.visibility = :vis
         AND (c.publishedAt IS NULL OR c.publishedAt <= :now)
         AND (:category IS NULL OR c.category = :category)
         AND (:groupKey IS NULL OR :groupKey = '' OR c.groupKey = :groupKey)
         AND (:q IS NULL OR :q = '' OR
              LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%')) OR
              LOWER(c.text)  LIKE LOWER(CONCAT('%', :q, '%')))
       ORDER BY COALESCE(c.publishedAt, c.createdAt) DESC, c.id DESC
    """)
    List<CmsContent> fetchFirstPage(@Param("vis") Visibility vis,
                                    @Param("now") Instant now,
                                    @Param("category") Category category,
                                    @Param("groupKey") String groupKey,
                                    @Param("q") String q,
                                    Pageable pageable);

    /**
                                       * Retrieves a page of CmsContent items after a given cursor, applying visibility, publish-time,
                                       * optional category/groupKey filters, and an optional text search.
                                       *
                                       * The query treats a null `publishedAt` as `createdAt` for ordering and cursor comparisons,
                                       * excludes items scheduled for the future (publishedAt > now), and orders results by
                                       * COALESCE(publishedAt, createdAt) DESC then id DESC. Pagination is controlled via the provided
                                       * Pageable and the cursor parameters.
                                       *
                                       * @param vis                required visibility to filter content
                                       * @param now                upper bound Instant for publishedAt; items with publishedAt in the future are excluded
                                       * @param category           optional category filter; if null, category is not filtered
                                       * @param groupKey           optional group key filter; if null or empty, groupKey is not filtered
                                       * @param q                  optional case-insensitive substring search applied to title or text; if null or empty, no text filtering
                                       * @param cursorPublishedAt  cursor timestamp used for "after" pagination (compared against COALESCE(publishedAt, createdAt))
                                       * @param cursorId           cursor id used to break ties when the cursor timestamp is equal
                                       * @param pageable           pagination information (page size, sort is ignored in favor of the query's ordering)
                                       * @return                   list of CmsContent items matching the criteria, ordered for cursor-based pagination
                                       */
                                      @Query("""
      SELECT c FROM CmsContent c
       WHERE c.visibility = :vis
         AND (c.publishedAt IS NULL OR c.publishedAt <= :now)
         AND (:category IS NULL OR c.category = :category)
         AND (:groupKey IS NULL OR :groupKey = '' OR c.groupKey = :groupKey)
         AND (:q IS NULL OR :q = '' OR
              LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%')) OR
              LOWER(c.text)  LIKE LOWER(CONCAT('%', :q, '%')))
         AND (
              COALESCE(c.publishedAt, c.createdAt) < :cursorPublishedAt
              OR (COALESCE(c.publishedAt, c.createdAt) = :cursorPublishedAt AND c.id < :cursorId)
         )
       ORDER BY COALESCE(c.publishedAt, c.createdAt) DESC, c.id DESC
    """)
    List<CmsContent> fetchAfterCursor(@Param("vis") Visibility vis,
                                      @Param("now") Instant now,
                                      @Param("category") Category category,
                                      @Param("groupKey") String groupKey,
                                      @Param("q") String q,
                                      @Param("cursorPublishedAt") Instant cursorPublishedAt,
                                      @Param("cursorId") Long cursorId,
                                      Pageable pageable);
}