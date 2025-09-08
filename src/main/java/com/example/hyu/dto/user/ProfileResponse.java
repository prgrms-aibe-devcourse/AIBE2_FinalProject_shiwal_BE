package com.example.hyu.dto.user;

import com.example.hyu.enums.ContentSensitivity;
import com.example.hyu.enums.ProfileConcernTag;
import com.example.hyu.enums.ProfileTone;

import java.time.Instant;
import java.util.List;

/** 프로필 조회 응답 DTO */
public record ProfileResponse(
        String nickname,
        String avatarUrl,
        String bio,
        List<String> goals,                       // 목표 텍스트만 간단히 전달
        List<ProfileConcernTag> concernTags,      // 관심 이슈 태그
        ProfileTone preferredTone,                // 응답 톤
        ContentSensitivity contentSensitivity,    // 민감도
        String language,                          // "ko"
        String checkinReminder,                   // "HH:mm"
        boolean weeklySummary,                    // 주간 요약 수신 여부
        boolean safetyConsent,                    // 안전 안내 동의
        String crisisResourcesRegion,             // "KR"
        boolean anonymity,                        // 익명 표시
        Instant updatedAt                         // 업데이트 시각
) {}