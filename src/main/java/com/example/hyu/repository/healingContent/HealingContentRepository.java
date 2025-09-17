package com.example.hyu.repository.healingContent;

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

    @Query("""
      SELECT c FROM CmsContent c
       WHERE c.visibility = :vis
         AND (c.publishedAt IS NULL OR c.publishedAt <= :now)
         AND (:category IS NULL OR c.category = :category)
         AND (:groupKey IS NULL OR :groupKey = '' OR c.groupKey = :groupKey)
         AND (:q IS NULL OR :q = '' OR
              LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%')) OR
              LOWER(c.text)  LIKE LOWER(CONCAT('%', :q, '%')))
         AND c.deleted = false
       ORDER BY COALESCE(c.publishedAt, c.createdAt) DESC, c.id DESC
    """)
    List<CmsContent> fetchFirstPage(@Param("vis") Visibility vis,
                                    @Param("now") Instant now,
                                    @Param("category") Category category,
                                    @Param("groupKey") String groupKey,
                                    @Param("q") String q,
                                    Pageable pageable);

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
         AND c.deleted = false
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