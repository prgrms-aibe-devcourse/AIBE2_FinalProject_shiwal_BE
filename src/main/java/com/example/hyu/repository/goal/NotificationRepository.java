package com.example.hyu.repository.goal;

import com.example.hyu.entity.Notification;
import com.example.hyu.enums.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 알림함 무한 스크롤 (최신순)
    Slice<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 안 읽은 개수
    long countByUserIdAndReadFalse(Long userId);

    // 중복 생성 체크 (유니크키와 함꼐 2중 안전장치)
    boolean existsByUserIdAndGoalIdAndTypeAndEventDate(Long userId, Long goalI,
                                                       NotificationType type, LocalDate eventDate);
}
