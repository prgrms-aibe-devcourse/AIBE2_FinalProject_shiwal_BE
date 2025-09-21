package com.example.hyu.controller;

import com.example.hyu.dto.SliceResponse;
import com.example.hyu.dto.goal.NotificationResponse;
import com.example.hyu.security.AuthPrincipal;
import com.example.hyu.service.goal.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    /** 알림함 무한 스크롤 */
    @GetMapping
    public SliceResponse<NotificationResponse> list(
            @AuthenticationPrincipal AuthPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        return service.list(user.getUserId(), page, size);
    }

    /** 안 읽은 개수 */
    @GetMapping("/unread-count")
    public long unread(@AuthenticationPrincipal AuthPrincipal user) {
        return service.unreadCount(user.getUserId());
    }

    /** 단건 읽음 처리 */
    @PatchMapping("/{notificationId}/read")
    public void markAsRead(@AuthenticationPrincipal AuthPrincipal user,
                           @PathVariable Long notificationId) {
        service.markAsRead(user.getUserId(), notificationId);
    }

    /** 모두 읽음 처리 */
    @PatchMapping("/read-all")
    public int markAllAsRead(@AuthenticationPrincipal AuthPrincipal user) {
        return service.markAllAsRead(user.getUserId());
    }
}
