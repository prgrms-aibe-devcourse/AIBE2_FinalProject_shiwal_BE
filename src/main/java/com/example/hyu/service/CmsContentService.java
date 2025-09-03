package com.example.hyu.service;

import com.example.hyu.dto.admin.*;
import com.example.hyu.entity.CmsContent.Category;
import com.example.hyu.entity.CmsContent.Visibility;
import org.springframework.data.domain.*;

public interface CmsContentService {
    /**
 * Creates a new CMS content entry from the provided request and records the acting administrator.
 *
 * The request supplies content fields such as title, body, metadata, category, visibility, and groupKey.
 * Returns a CmsContentResponse representing the newly created content, including its generated identifier and any server-populated fields (e.g., timestamps).
 *
 * @param req the content creation request containing content fields
 * @param adminId identifier of the administrator performing the creation
 * @return the created CmsContentResponse
 */
CmsContentResponse create(CmsContentRequest req, Long adminId);
    /**
 * Retrieve a CMS content entry by its identifier.
 *
 * @param id the identifier of the content to retrieve
 * @return the content represented as a {@code CmsContentResponse}
 */
CmsContentResponse get(Long id);
    /**
 * Searches CMS content with optional filters and returns a paginated result.
 *
 * The search supports a free-text query and optional filters for content category,
 * visibility state, and a logical grouping key. Results are returned according to
 * the supplied pagination and sorting in {@code pageable}.
 *
 * @param q         free-text search query; when null or empty, no text filtering is applied
 * @param category  optional content category filter; when null, all categories are included
 * @param visibility optional visibility filter; when null, all visibility states are included
 * @param groupKey  optional group identifier used to restrict results to a specific group/context
 * @param pageable  pagination and sorting information for the result page
 * @return          a Page of {@code CmsContentResponse} matching the provided criteria
 */
Page<CmsContentResponse> search(String q, Category category, Visibility visibility, String groupKey, Pageable pageable);
    /**
 * Updates an existing CMS content entry.
 *
 * @param id the identifier of the content to update
 * @param req the update payload containing new content fields
 * @param adminId the administrator's identifier performing the update (used for audit/context)
 * @return the updated CmsContentResponse representing the saved content
 */
CmsContentResponse update(Long id, CmsContentRequest req, Long adminId);
    /**
 * Set the visibility of a CMS content entry.
 *
 * Updates the persisted visibility of the content identified by `id` to the provided `value`.
 *
 * @param id the identifier of the CMS content to update
 * @param value the new visibility value to apply
 * @param adminId identifier of the administrator performing the change (used for auditing/authorization)
 */
void toggleVisibility(Long id, Visibility value, Long adminId);
    /**
 * Delete the CMS content identified by the given id.
 *
 * Removes the content record corresponding to the provided identifier.
 *
 * @param id the identifier of the content to delete
 */
void delete(Long id);
}