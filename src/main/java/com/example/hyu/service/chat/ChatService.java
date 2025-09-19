package com.example.hyu.service.chat;

import com.example.hyu.dto.chat.CreateSessionResponse;
import com.example.hyu.dto.chat.MessageDto;
import com.example.hyu.dto.chat.SessionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChatService {
    CreateSessionResponse create(Long userId);
    Page<SessionDto> list(Long userId, Pageable pageable);
    Page<MessageDto> getMessages(Long userId, UUID sessionId, Pageable pageable);
    MessageDto send(Long userId, UUID sessionId, String userContent);
}
