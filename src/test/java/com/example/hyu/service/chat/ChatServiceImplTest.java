/*
 Testing library/framework note:
 - Using JUnit 5 (org.junit.jupiter.api) with Mockito (org.mockito.junit.jupiter.MockitoExtension).
 - Assertions: primarily JUnit's Assertions, consistent with most existing tests.
 - Plain unit tests (no Spring context). External dependencies are mocked.
*/
package com.example.hyu.service.chat;

import com.example.hyu.assistant.AssistantClient;
import com.example.hyu.dto.chat.CreateSessionResponse;
import com.example.hyu.dto.chat.MessageDto;
import com.example.hyu.dto.chat.SessionDto;
import com.example.hyu.entity.ChatMessage;
import com.example.hyu.entity.ChatSession;
import com.example.hyu.entity.Profile;
import com.example.hyu.repository.ProfileRepository;
import com.example.hyu.repository.chat.ChatMessageRepository;
import com.example.hyu.repository.chat.ChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock ChatSessionRepository sessions;
    @Mock ChatMessageRepository messages;
    @Mock AssistantClient assistant;
    @Mock ProfileRepository profiles;

    @InjectMocks ChatServiceImpl service;

    private final Long userId = 101L;
    private final UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void init() {
        // @InjectMocks wires fields
    }

    private ChatSession newSession(Long uid) {
        ChatSession s = ChatSession.builder()
                .userId(uid)
                .status(ChatSession.Status.OPEN)
                .build();
        s.setUpdatedAt(Instant.now());
        return s;
    }

    private ChatMessage msg(Long id, ChatMessage.Role role, String content, Instant createdAt) {
        ChatMessage m = ChatMessage.builder()
                .role(role)
                .content(content)
                .build();
        try {
            java.lang.reflect.Field fId = ChatMessage.class.getDeclaredField("id");
            fId.setAccessible(true);
            fId.set(m, id);
        } catch (Exception ignored) {}
        try {
            java.lang.reflect.Field fTs = ChatMessage.class.getDeclaredField("createdAt");
            fTs.setAccessible(true);
            fTs.set(m, createdAt);
        } catch (Exception ignored) {}
        return m;
    }

    @Nested
    @DisplayName("create(userId)")
    class CreateTests {
        @Test
        @DisplayName("creates session and saves initial SYSTEM message with default prompt when profile missing")
        void create_savesSystemMessage_withDefaultNickname() {
            when(profiles.findById(userId)).thenReturn(Optional.empty());

            ArgumentCaptor<ChatSession> sessionCap = ArgumentCaptor.forClass(ChatSession.class);
            doAnswer(inv -> inv.getArgument(0)).when(sessions).save(sessionCap.capture());

            ArgumentCaptor<ChatMessage> msgCap = ArgumentCaptor.forClass(ChatMessage.class);
            doAnswer(inv -> inv.getArgument(0)).when(messages).save(msgCap.capture());

            CreateSessionResponse res = service.create(userId);
            assertNotNull(res);
            verify(sessions, times(1)).save(any(ChatSession.class));
            ChatSession saved = sessionCap.getValue();
            assertEquals(userId, saved.getUserId());
            assertEquals(ChatSession.Status.OPEN, saved.getStatus());

            verify(messages, times(1)).save(any(ChatMessage.class));
            ChatMessage sys = msgCap.getValue();
            assertEquals(ChatMessage.Role.SYSTEM, sys.getRole());
            assertNotNull(sys.getContent());
            assertTrue(sys.getContent().contains("- 닉네임: 사용자"));
            assertTrue(sys.getContent().contains("- 톤: NEUTRAL"));
            assertTrue(sys.getContent().contains("- 민감도: MEDIUM"));
        }

        @Test
        @DisplayName("includes profile nickname when available; tone/sensitivity default if null")
        void create_usesProfileNickname_whenAvailable() {
            Profile p = mock(Profile.class);
            when(p.getNickname()).thenReturn("Alex");
            when(p.getPreferredTone()).thenReturn(null);
            when(p.getContentSensitivity()).thenReturn(null);
            when(profiles.findById(userId)).thenReturn(Optional.of(p));

            ArgumentCaptor<ChatMessage> msgCap = ArgumentCaptor.forClass(ChatMessage.class);
            when(messages.save(msgCap.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(sessions.save(any(ChatSession.class))).thenAnswer(inv -> inv.getArgument(0));

            service.create(userId);

            ChatMessage sys = msgCap.getValue();
            String prompt = sys.getContent();
            assertTrue(prompt.contains("- 닉네임: Alex"));
            assertTrue(prompt.contains("- 톤: NEUTRAL"));
            assertTrue(prompt.contains("- 민감도: MEDIUM"));
        }

        @Test
        @DisplayName("returns non-null session id when repository assigns UUID")
        void create_returnsResponseIdNotNull_whenRepoAssignsId() {
            when(profiles.findById(userId)).thenReturn(Optional.empty());
            when(messages.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));
            doAnswer(inv -> {
                ChatSession s = inv.getArgument(0);
                try {
                    java.lang.reflect.Field f = ChatSession.class.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(s, java.util.UUID.randomUUID());
                } catch (Exception ignored) {}
                return s;
            }).when(sessions).save(any(ChatSession.class));

            CreateSessionResponse res = service.create(userId);
            assertNotNull(res);
            assertNotNull(res.id());
        }
    }

    @Nested
    @DisplayName("list(userId, pageable)")
    class ListTests {
        @Test
        @DisplayName("returns mapped session DTOs ordered by updatedAt desc")
        void list_mapsSessions() {
            ChatSession s1 = newSession(userId);
            ChatSession s2 = newSession(userId);
            s1.setUpdatedAt(Instant.now().minusSeconds(10));
            s2.setUpdatedAt(Instant.now());

            Page<ChatSession> page = new PageImpl<>(List.of(s2, s1), PageRequest.of(0, 10), 2);
            when(sessions.findByUserIdOrderByUpdatedAtDesc(eq(userId), any(Pageable.class)))
                    .thenReturn(page);

            Page<SessionDto> result = service.list(userId, PageRequest.of(0, 10));
            assertEquals(2, result.getTotalElements());
            List<SessionDto> content = result.getContent();
            assertEquals("OPEN", content.get(0).status());
            assertTrue(content.get(0).updatedAt().isAfter(content.get(1).updatedAt()));
        }

        @Test
        @DisplayName("returns empty page when none exist")
        void list_empty() {
            when(sessions.findByUserIdOrderByUpdatedAtDesc(eq(userId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
            Page<SessionDto> result = service.list(userId, PageRequest.of(0, 10));
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getMessages(userId, sessionId, pageable)")
    class GetMessagesTests {
        @Test
        @DisplayName("throws 404 when session not found")
        void getMessages_sessionMissing_404() {
            when(sessions.findById(sessionId)).thenReturn(Optional.empty());
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.getMessages(userId, sessionId, PageRequest.of(0, 20)));
            assertEquals(404, ex.getStatusCode().value());
        }

        @Test
        @DisplayName("throws 404 when session belongs to a different user")
        void getMessages_userMismatch_404() {
            ChatSession other = newSession(999L);
            when(sessions.findById(sessionId)).thenReturn(Optional.of(other));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.getMessages(userId, sessionId, PageRequest.of(0, 20)));
            assertEquals(404, ex.getStatusCode().value());
        }

        @Test
        @DisplayName("returns mapped MessageDto page on success")
        void getMessages_success() {
            ChatSession s = newSession(userId);
            when(sessions.findById(sessionId)).thenReturn(Optional.of(s));

            ChatMessage m1 = msg(1L, ChatMessage.Role.USER, "hi", Instant.now().minusSeconds(5));
            ChatMessage m2 = msg(2L, ChatMessage.Role.ASSISTANT, "hello", Instant.now());
            Page<ChatMessage> page = new PageImpl<>(List.of(m1, m2), PageRequest.of(0, 20), 2);
            when(messages.findBySession_IdAndUserIdOrderByCreatedAtAsc(eq(sessionId), eq(userId), any(Pageable.class)))
                    .thenReturn(page);

            Page<MessageDto> result = service.getMessages(userId, sessionId, PageRequest.of(0, 20));
            assertEquals(2, result.getTotalElements());
            assertEquals("USER", result.getContent().get(0).role());
            assertEquals("ASSISTANT", result.getContent().get(1).role());
            assertEquals("hello", result.getContent().get(1).content());
        }

        @Test
        @DisplayName("returns empty page when no messages")
        void getMessages_empty() {
            ChatSession s = newSession(userId);
            when(sessions.findById(sessionId)).thenReturn(Optional.of(s));
            when(messages.findBySession_IdAndUserIdOrderByCreatedAtAsc(eq(sessionId), eq(userId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
            Page<MessageDto> result = service.getMessages(userId, sessionId, PageRequest.of(0, 20));
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("send(userId, sessionId, userContent)")
    class SendTests {
        @Test
        @DisplayName("persists user message, calls assistant with existing system prompt, saves bot reply, updates session timestamp, and preserves call order")
        void send_happyPath_withExistingSystemPrompt() {
            ChatSession s = newSession(userId);
            when(sessions.findById(sessionId)).thenReturn(Optional.of(s));

            Page<ChatMessage> recent = new PageImpl<>(List.of(
                    msg(10L, ChatMessage.Role.USER, "earlier", Instant.now().minusSeconds(60))
            ));
            Page<ChatMessage> sysPage = new PageImpl<>(List.of(
                    msg(11L, ChatMessage.Role.SYSTEM, "SYS_PROMPT", Instant.now().minusSeconds(120))
            ));

            when(messages.findBySession_IdAndUserIdOrderByCreatedAtAsc(eq(sessionId), eq(userId), any(Pageable.class)))
                    .thenAnswer(inv -> {
                        Pageable p = inv.getArgument(2);
                        return p.getPageSize() == 50 ? recent : sysPage;
                    });

            when(messages.save(any(ChatMessage.class))).thenAnswer(inv -> {
                ChatMessage m = inv.getArgument(0);
                try {
                    java.lang.reflect.Field f = ChatMessage.class.getDeclaredField("createdAt");
                    f.setAccessible(true);
                    if (f.get(m) == null) f.set(m, Instant.now());
                } catch (Exception ignored) {}
                return m;
            });

            when(assistant.reply(eq("SYS_PROMPT"), anyList(), eq("hello there"))).thenReturn("BOT_REPLY");

            MessageDto dto = service.send(userId, sessionId, "hello there");
            assertNotNull(dto);
            assertEquals("ASSISTANT", dto.role());
            assertEquals("BOT_REPLY", dto.content());
            assertNotNull(dto.createdAt());
            assertNotNull(s.getUpdatedAt());

            // Verify order: save(USER) -> find(recent) -> find(sys) -> assistant.reply -> save(ASSISTANT)
            InOrder inOrder = inOrder(messages, assistant);
            inOrder.verify(messages).save(argThat(m -> m.getRole() == ChatMessage.Role.USER));
            inOrder.verify(messages, times(1)).findBySession_IdAndUserIdOrderByCreatedAtAsc(eq(sessionId), eq(userId), any(Pageable.class));
            inOrder.verify(messages, times(1)).findBySession_IdAndUserIdOrderByCreatedAtAsc(eq(sessionId), eq(userId), any(Pageable.class));
            inOrder.verify(assistant).reply(eq("SYS_PROMPT"), anyList(), eq("hello there"));
            inOrder.verify(messages).save(argThat(m -> m.getRole() == ChatMessage.Role.ASSISTANT));
        }

        @Test
        @DisplayName("uses PageRequest sizes 50 (history) and 1 (system prompt)")
        void send_usesExpectedPageSizes() {
            ChatSession s = newSession(userId);
            when(sessions.findById(sessionId)).thenReturn(Optional.of(s));

            when(messages.findBySession_IdAndUserIdOrderByCreatedAtAsc(eq(sessionId), eq(userId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            when(messages.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));
            when(assistant.reply(anyString(), anyList(), anyString())).thenReturn("ok");

            service.send(userId, sessionId, "hi");

            ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
            verify(messages, times(2)).findBySession_IdAndUserIdOrderByCreatedAtAsc(eq(sessionId), eq(userId), cap.capture());
            List<Integer> sizes = cap.getAllValues().stream().map(Pageable::getPageSize).sorted().toList();
            assertEquals(List.of(1, 50), sizes);
        }

        @Test
        @DisplayName("builds system prompt from profile when no SYSTEM message present")
        void send_buildsSystemPrompt_whenNoSystemMessage() {
            ChatSession s = newSession(userId);
            when(sessions.findById(sessionId)).thenReturn(Optional.of(s));

            Page<ChatMessage> recent = new PageImpl<>(Collections.emptyList());
            Page<ChatMessage> emptySys = new PageImpl<>(Collections.emptyList());

            when(messages.findBySession_IdAndUserIdOrderByCreatedAtAsc(eq(sessionId), eq(userId), any(Pageable.class)))
                    .thenAnswer(inv -> {
                        Pageable p = inv.getArgument(2);
                        return p.getPageSize() == 50 ? recent : emptySys;
                    });

            when(messages.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

            when(profiles.findById(userId)).thenReturn(Optional.empty());

            ArgumentCaptor<String> systemPromptCap = ArgumentCaptor.forClass(String.class);
            when(assistant.reply(systemPromptCap.capture(), anyList(), eq("hey"))).thenReturn("ok");

            service.send(userId, sessionId, "hey");

            String prompt = systemPromptCap.getValue();
            assertNotNull(prompt);
            assertTrue(prompt.contains("- 닉네임: 사용자"));
            assertTrue(prompt.contains("대화는 부드럽고 안전하게 이끌어 주세요."));
        }

        @Test
        @DisplayName("returns empty assistant content as-is")
        void send_emptyAssistantReply_returnsEmptyContent() {
            ChatSession s = newSession(userId);
            when(sessions.findById(sessionId)).thenReturn(Optional.of(s));

            when(messages.findBySession_IdAndUserIdOrderByCreatedAtAsc(eq(sessionId), eq(userId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()), new PageImpl<>(List.of())); // history, system
            when(messages.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));
            when(assistant.reply(anyString(), anyList(), eq("ping"))).thenReturn("");

            MessageDto dto = service.send(userId, sessionId, "ping");
            assertNotNull(dto);
            assertEquals("ASSISTANT", dto.role());
            assertEquals("", dto.content());
        }

        @Test
        @DisplayName("throws 404 when sending to a session owned by a different user")
        void send_userMismatch_404() {
            ChatSession other = newSession(999L);
            when(sessions.findById(sessionId)).thenReturn(Optional.of(other));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.send(userId, sessionId, "msg"));
            assertEquals(404, ex.getStatusCode().value());
        }
    }
}