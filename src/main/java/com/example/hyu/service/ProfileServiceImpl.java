package com.example.hyu.service;

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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepo;
    private final ProfileConcernRepository concernRepo;
    private final ProfileGoalRepository goalRepo;

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
}
