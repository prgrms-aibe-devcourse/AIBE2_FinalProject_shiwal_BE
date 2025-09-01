package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "daily_checkins")
public class DailyCheckinEmotion {

    @EmbeddedId
    private Id id;

    @Column(name = "count", nullable = false)
    @Builder.Default
    private Long count = 0L;

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Id implements Serializable {

        @Column(name = "list_id", nullable = false)
        private Long listId;

        @Column(name = "checkin_id", nullable = false)
        private Long checkinId; // FK → daily_checkin.checkin_id

        @Column(name = "emotion_id", nullable = false)
        private Long emotionId; // FK → emotion.emotion_id
    }
}