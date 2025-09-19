package com.example.hyu.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingNotificationSender implements NotificationSender {
    @Override
    public void sendInApp(Long userId, String title, String body) {
        log.info("[IN-APP] userId={} | {} - {}", userId, title, body);
    }
}
