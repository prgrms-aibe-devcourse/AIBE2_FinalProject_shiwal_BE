package com.example.hyu.dto.user;

import com.example.hyu.enums.ContentSensitivity;
import com.example.hyu.enums.ProfileConcernTag;
import com.example.hyu.enums.ProfileTone;
import jakarta.validation.constraints.*;

import java.util.List;

public record ProfileUpdateRequest(
        @NotBlank @Size(min = 2, max = 16)
        String nickname,

        @Size(max = 255)
        String avatarUrl,

        @Size(max = 200)
        String bio,

        @Size(max = 10)
        List<@NotBlank @Size(min = 2, max = 50) String> goals,

        @Size(max = 7)
        List<ProfileConcernTag> concernTags,

        ProfileTone preferredTone,
        ContentSensitivity contentSensitivity,

        // ISO 639-1 언어 코드
        @Pattern(regexp = "^[a-z]{2}$")
        String language,

        // 24시간 "HH:mm"
        @Pattern(regexp = "^(?:[01]\\d|2[0-3]):[0-5]\\d$")
        String checkinReminder,

        Boolean weeklySummary,
        Boolean safetyConsent,

        @Size(max = 8)
        String crisisResourcesRegion, // 예: "KR"

        Boolean anonymity
) {}