package com.example.hyu.service;

import com.example.hyu.dto.user.ProfileResponse;
import com.example.hyu.dto.user.ProfileUpdateRequest;

public interface ProfileService {
    ProfileResponse getMyProfile(Long userId);
    ProfileResponse upsertMyProfile(Long userId, ProfileUpdateRequest req);
}