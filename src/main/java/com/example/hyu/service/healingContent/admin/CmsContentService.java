package com.example.hyu.service.healingContent.admin;

import com.example.hyu.dto.healingContent.CmsContentRequest;
import com.example.hyu.dto.healingContent.CmsContentResponse;
import com.example.hyu.entity.CmsContent.Category;
import com.example.hyu.entity.CmsContent.Visibility;
import org.springframework.data.domain.*;

public interface CmsContentService {
    CmsContentResponse create(CmsContentRequest req, Long adminId);
    CmsContentResponse get(Long id);
    Page<CmsContentResponse> search(String q, Category category, Visibility visibility, Boolean includeDeleted, String groupKey, Pageable pageable);
    CmsContentResponse update(Long id, CmsContentRequest req, Long adminId);
    void toggleVisibility(Long id, Visibility value, Long adminId);
    void delete(Long id);
}