package com.example.hyu.notification;

public interface NotificationSender {
    void sendInApp(Long userId, String title, String body);
    default void sendEmail(Long userId, String subject, String body) { /* no-op */ }
}