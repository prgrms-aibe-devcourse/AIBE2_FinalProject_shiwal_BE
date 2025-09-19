package com.example.hyu.entity;

import com.example.hyu.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

//웹 알림함
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "notifications",
        uniqueConstraints = {
                // 같은 날, 같은 목표, 같은 타입 알림은 1회만 생성
                @UniqueConstraint(name = "uq_notif_daily", columnNames = {"user_id", "goal_id", "type", "event_date"})
        },
        indexes = {
                @Index(name = "idx_notif_user_read", columnList = "user_id, is_read, created_at")
        }
)
public class Notification extends BaseTimeEntity{

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "notification_id")
        private Long id;

        @Column(name = "user_id", nullable = false)
        private Long userId;  // 알림을 받을 사용자

        @Column(name = "goal_id")
        private Long goalId;  // 어떤 목표에 대한 알림인지

        @Enumerated(EnumType.STRING)
        @Column(length = 40, nullable = false)
        private NotificationType type;  // 알림 종류(미이행 알림, 일반 알림)

        @Column(nullable = false, length = 120)
        private String title;  // 알림 제목(간단한 안내 문구)

        @Column(nullable = false, length = 500)
        private String body;  // 알림 본문( 상세 메시지)

        @Column(name = "event_date")
        private LocalDate eventDate;  // 알림 기준일 (중복 방지 키에 사용)

        @Column(name = "is_read", nullable = false)
        private boolean read ; // 읽음 여부 false = 안 읽음, true = 읽음

        // 알림을 읽음 처리할 떄 호출
        public void markAsRead() {
                this.read = true;
        }

}
