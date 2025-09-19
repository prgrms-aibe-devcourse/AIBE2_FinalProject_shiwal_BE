package com.example.hyu.repository.chat;

import com.example.hyu.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    Page<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
}