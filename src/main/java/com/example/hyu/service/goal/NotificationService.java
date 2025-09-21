package com.example.hyu.service.goal;

import com.example.hyu.dto.SliceResponse;
import com.example.hyu.dto.goal.NotificationResponse;
import com.example.hyu.entity.Notification;
import com.example.hyu.repository.goal.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository repo;

    /** 알림함 무한 스크롤 조회 */
    public SliceResponse<NotificationResponse> list(Long userId, int page, int size) {
        var slice = repo.findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(NotificationResponse::from);
        return com.example.hyu.dto.SliceResponse.from(slice);
    }

    /** 안 읽은 개수 */
    public long unreadCount(Long userId) {
        return repo.countByUserIdAndReadFalse(userId);
    }

    /** 단건 읽음 처리 */
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification n = repo.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("알림을 찾을 수 없습니다."));
        if (!n.getUserId().equals(userId)) {
            throw new AccessDeniedException("본인 알림만 읽음 처리할 수 있습니다.");
        }
        n.markAsRead();
    }

    /** 모두 읽음 처리 (최근 200건) */
    @Transactional
    public int markAllAsRead(Long userId) {
        // 간단히 최신 200건만 처리(대량 업데이트 방지)
        var slice = repo.findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 200));
        int cnt = 0;
        for (var n : slice) {
            if (!n.isRead()) { n.markAsRead(); cnt++; }
        }
        return cnt;
    }
}
