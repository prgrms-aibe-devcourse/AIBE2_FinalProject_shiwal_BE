package com.example.hyu.controller;

import com.example.hyu.dto.admin.*;
import com.example.hyu.entity.CmsContent.Category;
import com.example.hyu.entity.CmsContent.Visibility;
import com.example.hyu.service.CmsContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

// 🔹 테스트 단계에선 PreAuthorize 잠시 막자 (로그인/권한 미구현이면 401/403 나옴)
// @PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/cms-contents")
@RequiredArgsConstructor
@Validated
public class CmsContentController {

    private final CmsContentService service;

    /**
     * Creates a new CMS content.
     *
     * Accepts a validated CMS content payload and the calling admin's identifier, delegates creation to the service,
     * and returns the created content representation.
     *
     * @param req     validated request body containing the content data
     * @param adminId admin identifier provided via the "X-ADMIN-ID" request header
     * @return the created CmsContentResponse
     */
    @PostMapping
    public CmsContentResponse create(@Valid @RequestBody CmsContentRequest req,
                                     @RequestHeader("X-ADMIN-ID") Long adminId) {
        return service.create(req, adminId);
    }

    /**
     * Retrieve a CMS content by its ID.
     *
     * @param id the content's unique identifier
     * @return the found CmsContentResponse
     */
    @GetMapping("/{id}")
    public CmsContentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    /**
     * Searches and returns a paginated list of CMS contents.
     *
     * Supports optional filtering by full-text query, category, visibility, and group key,
     * and paging/sorting via page, size, and sort parameters.
     *
     * The `sort` parameter expects the form `"field,dir"` (e.g. `"createdAt,asc"`).
     * If `dir` is omitted or not `"asc"` (case-insensitive), descending order is used.
     *
     * @param q         optional full-text query to match content
     * @param category  optional category filter
     * @param visibility optional visibility filter
     * @param groupKey  optional group key to filter contents belonging to a group
     * @param page      zero-based page index (default 0)
     * @param size      page size (default 12)
     * @param sort      sort specification in the form `field,dir` (default "createdAt,desc")
     * @return          a page of matching CmsContentResponse objects
     */
    @GetMapping
    public Page<CmsContentResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Visibility visibility,
            @RequestParam(required = false) String groupKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        String[] s = sort.split(",");
        Sort.Direction dir = (s.length > 1 && "asc".equalsIgnoreCase(s[1]))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, s[0]));
        return service.search(q, category, visibility, groupKey, pageable);
    }

    /**
     * Update an existing CMS content.
     *
     * Validates the request body and delegates to the service to perform the update.
     *
     * @param id the identifier of the CMS content to update
     * @param req the update payload (validated)
     * @param adminId the admin's ID taken from the `X-ADMIN-ID` request header
     * @return the updated CmsContentResponse
     */
    @PutMapping("/{id}")
    public CmsContentResponse update(@PathVariable Long id,
                                     @Valid @RequestBody CmsContentRequest req,
                                     @RequestHeader("X-ADMIN-ID") Long adminId) {
        return service.update(id, req, adminId);
    }

    /**
     * Set the visibility state for a CMS content item.
     *
     * Updates the visibility of the content identified by {@code id} to the provided {@code value}.
     *
     * @param id the ID of the CMS content to update
     * @param value the target visibility state
     * @param adminId the admin's ID from the "X-ADMIN-ID" request header authorizing the change
     */
    @PatchMapping("/{id}/visibility")
    public void setVisibility(@PathVariable Long id,
                              @RequestParam("value") Visibility value,
                              @RequestHeader("X-ADMIN-ID") Long adminId) {
        service.toggleVisibility(id, value, adminId);
    }

    /**
     * Delete the CMS content with the given id.
     *
     * Delegates deletion to the CmsContentService; no content is returned.
     *
     * @param id the identifier of the CMS content to delete
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}