package com.example.hyu.dto.goal;
import com.example.hyu.entity.Notification;
import com.example.hyu.enums.NotificationType;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 📌 클라이언트로 내려보내는 "알림" 응답 DTO
 * - Entity 그대로 반환하지 않고 필요한 필드만 노출
 * - record 타입이라 불변성 유지 + 간단한 생성자 제공
 */
public record NotificationResponse(
        Long id,                 // 알림 ID (PK)
        Long goalId,             // 어떤 목표와 연관된 알림인지 (없으면 null)
        String title,            // 알림 제목 (간단한 안내 문구)
        String body,             // 알림 본문 (상세 설명)
        NotificationType type,   // 알림 종류 (MISSED_DAILY 등)
        LocalDate eventDate,     // 알림 기준 날짜 (예: 2025-09-20)
        boolean read,            // 읽음 여부 (false: 안읽음, true: 읽음)
        Instant createdAt        // 생성 시각 (BaseTimeEntity에서 상속)
){
    // 엔티티 → DTO 변환 편의 메서드
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getGoalId(),
                n.getTitle(),
                n.getBody(),
                n.getType(),
                n.getEventDate(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}