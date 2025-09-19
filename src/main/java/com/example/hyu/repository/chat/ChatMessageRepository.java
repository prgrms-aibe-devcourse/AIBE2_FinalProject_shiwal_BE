package com.example.hyu.repository.chat;

import com.example.hyu.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findBySession_IdAndUserIdOrderByCreatedAtAsc(UUID sessionId, Long userId, Pageable pageable);

    // ✅ 프로필용: 사용자 전체 최근 메시지 피드 (최신순)
    Page<ChatMessage> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<ChatMessage> findBySession_IdAndUserId(UUID sessionId, Long userId, Pageable pageable);

    boolean existsBySession_IdAndUserId(UUID sessionId, Long userId);
}