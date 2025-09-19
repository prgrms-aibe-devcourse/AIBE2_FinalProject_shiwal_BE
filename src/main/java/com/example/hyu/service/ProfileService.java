package com.example.hyu.service;

import com.example.hyu.dto.user.ProfileResponse;
import com.example.hyu.dto.user.ProfileUpdateRequest;
import com.example.hyu.dto.user.ProfileChatMessageDto;
import com.example.hyu.dto.user.ProfileChatSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProfileService {
    /**
     * Retrieve the profile for the specified user.
     *
     * @param userId the identifier of the user whose profile should be returned
     * @return a ProfileResponse containing the user's profile data
     */
    ProfileResponse getMyProfile(Long userId);

    /**
     * Create or update the profile for the specified user and return the resulting profile.
     *
     * The method applies the changes described in the provided request to the user's profile.
     *
     * @param userId the id of the user whose profile will be created or updated
     * @param req the profile update data (fields to create or modify)
     * @return the stored ProfileResponse reflecting the user's current profile after the upsert
     */
    ProfileResponse upsertMyProfile(Long userId, ProfileUpdateRequest req);

    // -------------------- Added for profile chat view --------------------


    Page<ProfileChatMessageDto> getMyRecentChatMessages(Long userId, Pageable pageable);


    List<ProfileChatSummaryDto> getMyLatestPerSession(Long userId, Pageable pageable);
}