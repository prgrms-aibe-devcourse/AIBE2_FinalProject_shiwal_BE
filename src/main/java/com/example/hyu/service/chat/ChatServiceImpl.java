package com.example.hyu.service.chat;

import com.example.hyu.assistant.AssistantClient;
import com.example.hyu.dto.chat.CreateSessionResponse;
import com.example.hyu.dto.chat.MessageDto;
import com.example.hyu.dto.chat.SessionDto;
import com.example.hyu.entity.ChatMessage;
import com.example.hyu.entity.ChatSession;
import com.example.hyu.entity.Profile;
import com.example.hyu.repository.chat.ChatMessageRepository;
import com.example.hyu.repository.chat.ChatSessionRepository;
import com.example.hyu.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final AssistantClient assistant;
    private final ProfileRepository profiles;

    @Override
    @Transactional
    public CreateSessionResponse create(Long userId) {
        // 1) 세션 생성
        ChatSession s = ChatSession.builder()
                .userId(userId)
                .status(ChatSession.Status.OPEN)
                .build();
        sessions.save(s); // @PrePersist 로 createdAt/updatedAt 세팅

        // 2) 초기 SYSTEM 메시지(프로필 기반 프롬프트) 저장
        ChatMessage sys = ChatMessage.builder()
                .session(s)
                .userId(userId)
                .role(ChatMessage.Role.SYSTEM)
                .content(buildSystemPrompt(userId))
                .build();
        messages.save(sys);

        return new CreateSessionResponse(s.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SessionDto> list(Long userId, Pageable pageable) {
        return sessions.findByUserIdOrderByUpdatedAtDesc(userId, pageable)
                .map(s -> new SessionDto(s.getId(), s.getStatus().name(), s.getUpdatedAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageDto> getMessages(Long userId, UUID sessionId, Pageable pageable) {
        ensureOwnership(userId, sessionId);
        return messages
                .findBySession_IdAndUserIdOrderByCreatedAtAsc(sessionId, userId, pageable)
                .map(m -> new MessageDto(
                        m.getId(),
                        m.getRole().name(),
                        m.getContent(),
                        m.getCreatedAt()
                ));
    }

    @Override
    @Transactional
    public MessageDto send(Long userId, UUID sessionId, String userContent) {
        ChatSession s = ensureOwnership(userId, sessionId);

        // 1) 사용자 메시지 저장
        ChatMessage userMsg = messages.save(ChatMessage.builder()
                .session(s)
                .userId(userId)
                .role(ChatMessage.Role.USER)
                .content(userContent)
                .build());

        // 2) (선택) Safety 훅 자리
        // SafetyResult sr = safetyService.check(userContent, profile.getCrisisResourcesRegion());
        // if (sr.level()==CRITICAL) { ... }

        // 3) 어시스턴트 응답 생성 (더미 어댑터 사용)
        List<MessageDto> recent = messages
                .findBySession_IdAndUserIdOrderByCreatedAtAsc(sessionId, userId, PageRequest.of(0, 50))
                .map(m -> new MessageDto(m.getId(), m.getRole().name(), m.getContent(), m.getCreatedAt()))
                .getContent();

        String systemPrompt = loadSystemPrompt(sessionId, userId);
        String replyText = assistant.reply(systemPrompt, recent, userContent);

        ChatMessage botMsg = messages.save(ChatMessage.builder()
                .session(s)
                .userId(userId)
                .role(ChatMessage.Role.ASSISTANT)
                .content(replyText)
                .build());

        // 4) 세션 갱신 (updatedAt 갱신)
        s.setUpdatedAt(Instant.now());
        // sessions.save(s); // JPA 영속 상태면 생략 가능

        return new MessageDto(
                botMsg.getId(),
                botMsg.getRole().name(),
                botMsg.getContent(),
                botMsg.getCreatedAt()
        );
    }

    // -------------------- 내부 유틸 --------------------

    private ChatSession ensureOwnership(Long userId, UUID sessionId) {
        ChatSession s = sessions.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!s.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return s;
    }

    /** 프로필 기반 시스템 프롬프트 생성 */
    private String buildSystemPrompt(Long userId) {
        Profile p = profiles.findById(userId).orElse(null);
        String nick = (p != null && p.getNickname() != null) ? p.getNickname() : "사용자";
        String tone = (p != null && p.getPreferredTone() != null) ? p.getPreferredTone().name() : "NEUTRAL";
        String sens = (p != null && p.getContentSensitivity() != null) ? p.getContentSensitivity().name() : "MEDIUM";

        return "당신은 공감적인 AI 상담사입니다.\n"
                + "- 닉네임: " + nick + "\n"
                + "- 톤: " + tone + "\n"
                + "- 민감도: " + sens + "\n"
                + "대화는 부드럽고 안전하게 이끌어 주세요.";
    }

    /** 세션의 최초 SYSTEM 메시지 내용을 읽어오거나, 없으면 새로 빌드 */
    private String loadSystemPrompt(UUID sessionId, Long userId) {
        var page = messages.findBySession_IdAndUserIdOrderByCreatedAtAsc(
                sessionId, userId, PageRequest.of(0, 1)
        );
        return page.getContent().stream()
                .filter(m -> m.getRole() == ChatMessage.Role.SYSTEM) // 엔티티 enum으로 비교
                .map(ChatMessage::getContent)                        // 엔티티 getter
                .findFirst()
                .orElse(buildSystemPrompt(userId));
    }
}
