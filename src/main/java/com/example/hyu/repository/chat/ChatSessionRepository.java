package com.example.hyu.repository.chat;

import com.example.hyu.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    Page<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    // 세션 소유권 확인용 (권한 체크 경로에서 사용)
    boolean existsByIdAndUserId(UUID id, Long userId);
    Optional<ChatSession> findByIdAndUserId(UUID id, Long userId);
}