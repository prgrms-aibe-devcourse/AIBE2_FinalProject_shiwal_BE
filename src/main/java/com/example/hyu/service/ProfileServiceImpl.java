package com.example.hyu.service;

import com.example.hyu.dto.user.ProfileChatMessageDto;
import com.example.hyu.dto.user.ProfileChatSummaryDto;
import com.example.hyu.dto.user.ProfileResponse;
import com.example.hyu.dto.user.ProfileUpdateRequest;
import com.example.hyu.entity.Profile;
import com.example.hyu.entity.ProfileConcern;
import com.example.hyu.entity.ProfileGoal;
import com.example.hyu.enums.ContentSensitivity;
import com.example.hyu.enums.ProfileTone;
import com.example.hyu.repository.ProfileConcernRepository;
import com.example.hyu.repository.ProfileGoalRepository;
import com.example.hyu.repository.ProfileRepository;
import com.example.hyu.repository.chat.ChatMessageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepo;
    private final ProfileConcernRepository concernRepo;
    private final ProfileGoalRepository goalRepo;
    // ⬇️ 추가: 프로필 화면에서 채팅을 보여주기 위한 메시지 조회
    private final ChatMessageRepository messages;

    /**
     * Retrieves the profile for the given user ID, returning a ProfileResponse that includes
     * profile fields, associated concern tags, and goals.
     *
     * If no Profile exists for the user, a default Profile is created and persisted with
     * sensible defaults (e.g., nickname "사용자{userId}", preferredTone NEUTRAL, contentSensitivity MEDIUM,
     * language "ko", anonymity true, weeklySummary false, safetyConsent false, crisisResourcesRegion "KR")
     * and the current timestamp.
     *
     * @param userId the ID of the user whose profile to fetch
     * @return a ProfileResponse containing the user's profile data, tags, goals, and last updated timestamp
     */
    @Override
    public ProfileResponse getMyProfile(Long userId) {
        Profile p = profileRepo.findById(userId).orElseGet(() -> {
            Profile np = Profile.builder()
                    .userId(userId)
                    .nickname("사용자" + userId)
                    .preferredTone(ProfileTone.NEUTRAL)
                    .contentSensitivity(ContentSensitivity.MEDIUM)
                    .language("ko")
                    .anonymity(true)
                    .weeklySummary(false)
                    .safetyConsent(false)
                    .crisisResourcesRegion("KR")
                    .updatedAt(Instant.now())
                    .build();
            return profileRepo.save(np);
        });

        var tags = concernRepo.findByUserId(userId).stream().map(ProfileConcern::getTag).toList();
        var goals = goalRepo.findByUserIdOrderByOrderNoAscIdAsc(userId).stream().map(ProfileGoal::getText).toList();

        return new ProfileResponse(
                p.getNickname(), p.getAvatarUrl(), p.getBio(),
                goals, tags, p.getPreferredTone(), p.getContentSensitivity(),
                p.getLanguage(), p.getCheckinReminder(), p.isWeeklySummary(),
                p.isSafetyConsent(), p.getCrisisResourcesRegion(), p.isAnonymity(),
                p.getUpdatedAt()
        );
    }

    /**
     * Create or update the caller's Profile and associated tags and goals, then return the updated ProfileResponse.
     *
     * <p>This method is transactional: it inserts a new Profile if none exists for the given userId, applies updates
     * from the provided ProfileUpdateRequest, and replaces concern tags and goals when those lists are present
     * in the request (deletes existing entries and inserts the new set). Fields in the Profile are only changed
     * when the corresponding request values are non-null (except avatarUrl, bio, and checkinReminder which are set
     * unconditionally). The nickname and goal texts are trimmed before saving. The Profile's updatedAt is set to
     * the current instant.</p>
     *
     * @param userId the id of the user whose profile will be created or updated
     * @param req the update payload containing profile fields, concernTags, and goals; when concernTags or goals are
     *            null, the corresponding stored entries are left unchanged; when provided they fully replace existing ones
     * @return the latest ProfileResponse for the user after the upsert
     */
    @Override
    @Transactional
    public ProfileResponse upsertMyProfile(Long userId, ProfileUpdateRequest req) {
        Profile p = profileRepo.findById(userId).orElseGet(() -> {
            Profile np = new Profile();
            np.setUserId(userId);
            return np;
        });

        // 필드 업데이트
        if (req.nickname() != null) p.setNickname(req.nickname().trim());
        p.setAvatarUrl(req.avatarUrl());
        p.setBio(req.bio());
        if (req.preferredTone() != null) p.setPreferredTone(req.preferredTone());
        if (req.contentSensitivity() != null) p.setContentSensitivity(req.contentSensitivity());
        if (req.language() != null) p.setLanguage(req.language());
        p.setCheckinReminder(req.checkinReminder());
        if (req.weeklySummary() != null) p.setWeeklySummary(req.weeklySummary());
        if (req.safetyConsent() != null) p.setSafetyConsent(req.safetyConsent());
        if (req.crisisResourcesRegion() != null) p.setCrisisResourcesRegion(req.crisisResourcesRegion());
        if (req.anonymity() != null) p.setAnonymity(req.anonymity());
        p.setUpdatedAt(Instant.now());
        profileRepo.save(p);

        // 태그 교체
        if (req.concernTags() != null) {
            concernRepo.deleteByUserId(userId);
            if (!req.concernTags().isEmpty()) {
                List<ProfileConcern> list = new ArrayList<>();
                for (var t : req.concernTags()) {
                    list.add(new ProfileConcern(userId, t));
                }
                concernRepo.saveAll(list);
            }
        }

        // 목표 교체
        if (req.goals() != null) {
            goalRepo.deleteByUserId(userId);
            if (!req.goals().isEmpty()) {
                List<ProfileGoal> list = new ArrayList<>();
                int i = 1;
                for (var g : req.goals()) {
                    list.add(ProfileGoal.builder()
                            .userId(userId)
                            .text(g.trim())
                            .orderNo(i++)
                            .createdAt(Instant.now())
                            .build());
                }
                goalRepo.saveAll(list);
            }
        }

        return getMyProfile(userId);
    }

    // -------------------- Added for profile chat view --------------------

    @Override
    public Page<ProfileChatMessageDto> getMyRecentChatMessages(Long userId, Pageable pageable) {
        return messages.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(m -> new ProfileChatMessageDto(
                        m.getId(),
                        m.getSession().getId(),
                        m.getRole().name(),
                        m.getContent(),
                        m.getCreatedAt()
                ));
    }

    @Override
    public List<ProfileChatSummaryDto> getMyLatestPerSession(Long userId, Pageable pageable) {
        // 간단 구현: 최신 메시지 피드에서 세션별 첫 등장(=그 세션의 최신 메시지)만 추림
        var page = messages.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return page.getContent().stream()
                .collect(Collectors.toMap(
                        m -> m.getSession().getId(),
                        m -> m,
                        (a, b) -> a, // 이미 최신인 a 유지
                        LinkedHashMap::new
                ))
                .values().stream()
                .map(m -> new ProfileChatSummaryDto(
                        m.getSession().getId(),
                        m.getRole().name(),
                        m.getContent(),
                        m.getCreatedAt()
                ))
                .toList();
    }
}