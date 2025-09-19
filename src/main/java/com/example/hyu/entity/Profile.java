package com.example.hyu.entity;

import com.example.hyu.enums.ContentSensitivity;
import com.example.hyu.enums.ProfileTone;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "profiles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Profile {

    @Id
    private Long userId;   // users.id (1:1 매핑, FK로 잡아도 되고 단순 키만 관리해도 OK)

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 255)
    private String avatarUrl;

    @Column(length = 400)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProfileTone preferredTone = ProfileTone.NEUTRAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentSensitivity contentSensitivity = ContentSensitivity.MEDIUM;

    @Column(nullable = false, length = 8)
    private String language = "ko";

    @Column(length = 5) // "HH:mm"
    private String checkinReminder;

    @Column(nullable = false)
    private boolean weeklySummary = false;

    @Column(nullable = false)
    private boolean safetyConsent = false;

    @Column(length = 8)
    private String crisisResourcesRegion = "KR";

    @Column(nullable = false)
    private boolean anonymity = true;

    private Instant updatedAt;
}