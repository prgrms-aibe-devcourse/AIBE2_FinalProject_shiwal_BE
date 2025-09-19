package com.example.hyu.controller.chat;

import com.example.hyu.dto.chat.CreateSessionResponse;
import com.example.hyu.dto.chat.MessageDto;
import com.example.hyu.dto.chat.SendMessageRequest;
import com.example.hyu.dto.chat.SessionDto;
import com.example.hyu.security.AuthPrincipal;
import com.example.hyu.service.chat.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testing stack: JUnit 5 (Jupiter), Spring Boot @WebMvcTest with MockMvc, Mockito via @MockBean,
 * and spring-security-test for authentication mocking.
 *
 * Focus is on ChatController endpoints and especially parameters introduced/changed in the PR diff.
 * We validate happy paths, pagination defaults/overrides, validation errors, and that AuthPrincipal's userId
 * is propagated to ChatService methods.
 */
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    private static SecurityMockMvcRequestPostProcessors.RequestPostProcessor auth(AuthPrincipal principal, String... roles) {
        // Wrap custom principal in a Spring Authentication for @AuthenticationPrincipal resolution.
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(principal, null, roles.length == 0 ? new String[]{"ROLE_USER"} : roles);
        authentication.setAuthenticated(true);
        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    private static AuthPrincipal principal(UUID userId) {
        // Construct a minimal AuthPrincipal for tests. If AuthPrincipal has a different constructor,
        // adapt here accordingly; tests rely only on getUserId().
        return new AuthPrincipal(userId, "user@example.com", "User", List.of("ROLE_USER"));
    }

    @Nested
    @DisplayName("POST /sessions - create session")
    class CreateSession {

        @Test
        @DisplayName("should create a new session for the authenticated user and return response payload")
        void createSession_ok() throws Exception {
            UUID userId = UUID.randomUUID();
            CreateSessionResponse stub = new CreateSessionResponse(UUID.randomUUID());
            when(chatService.create(eq(userId))).thenReturn(stub);

            mockMvc.perform(post("/sessions")
                            .with(auth(principal(userId)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(stub.id().toString())));

            verify(chatService, times(1)).create(eq(userId));
        }
    }

    @Nested
    @DisplayName("GET /sessions - list sessions")
    class ListSessions {

        @Test
        @DisplayName("should use default pagination (page=0,size=20) and return a page of sessions")
        void list_defaultPaging() throws Exception {
            UUID userId = UUID.randomUUID();
            List<SessionDto> content = List.of(
                    new SessionDto(UUID.randomUUID(), "Session A", Instant.now()),
                    new SessionDto(UUID.randomUUID(), "Session B", Instant.now())
            );
            Page<SessionDto> page = new PageImpl<>(content, PageRequest.of(0, 20), 2);
            when(chatService.list(eq(userId), eq(PageRequest.of(0, 20)))).thenReturn(page);

            mockMvc.perform(get("/sessions")
                            .with(auth(principal(userId)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id", is(content.get(0).id().toString())))
                    .andExpect(jsonPath("$.content[0].title", is(content.get(0).title())))
                    .andExpect(jsonPath("$.size", is(20)))
                    .andExpect(jsonPath("$.number", is(0)))
                    .andExpect(jsonPath("$.totalElements", is(2)));

            verify(chatService).list(eq(userId), eq(PageRequest.of(0, 20)));
        }

        @Test
        @DisplayName("should honor explicit page and size query params")
        void list_customPaging() throws Exception {
            UUID userId = UUID.randomUUID();
            int pageNum = 3, size = 7;
            List<SessionDto> content = List.of(new SessionDto(UUID.randomUUID(), "OnlyOne", Instant.now()));
            Page<SessionDto> page = new PageImpl<>(content, PageRequest.of(pageNum, size), 15);
            when(chatService.list(eq(userId), eq(PageRequest.of(pageNum, size)))).thenReturn(page);

            mockMvc.perform(get("/sessions")
                            .queryParam("page", String.valueOf(pageNum))
                            .queryParam("size", String.valueOf(size))
                            .with(auth(principal(userId)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.number", is(pageNum)))
                    .andExpect(jsonPath("$.size", is(size)));

            verify(chatService).list(eq(userId), eq(PageRequest.of(pageNum, size)));
        }
    }

    @Nested
    @DisplayName("GET /sessions/{id}/messages - list messages")
    class ListMessages {

        @Test
        @DisplayName("should return messages with default paging (page=0,size=50)")
        void messages_defaultPaging() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            List<MessageDto> messages = List.of(
                    new MessageDto(UUID.randomUUID(), sessionId, "user", "Hello", Instant.now()),
                    new MessageDto(UUID.randomUUID(), sessionId, "assistant", "Hi\!", Instant.now())
            );
            Page<MessageDto> page = new PageImpl<>(messages, PageRequest.of(0, 50), messages.size());
            when(chatService.getMessages(eq(userId), eq(sessionId), eq(PageRequest.of(0, 50)))).thenReturn(page);

            mockMvc.perform(get("/sessions/{id}/messages", sessionId)
                            .with(auth(principal(userId)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].content", is("Hello")))
                    .andExpect(jsonPath("$.size", is(50)))
                    .andExpect(jsonPath("$.number", is(0)));

            verify(chatService).getMessages(eq(userId), eq(sessionId), eq(PageRequest.of(0, 50)));
        }

        @Test
        @DisplayName("should honor explicit page and size")
        void messages_customPaging() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            int pageNum = 2, size = 5;
            List<MessageDto> messages = List.of(new MessageDto(UUID.randomUUID(), sessionId, "user", "Only", Instant.now()));
            Page<MessageDto> page = new PageImpl<>(messages, PageRequest.of(pageNum, size), 11);
            when(chatService.getMessages(eq(userId), eq(sessionId), eq(PageRequest.of(pageNum, size)))).thenReturn(page);

            mockMvc.perform(get("/sessions/{id}/messages", sessionId)
                            .queryParam("page", String.valueOf(pageNum))
                            .queryParam("size", String.valueOf(size))
                            .with(auth(principal(userId)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.number", is(pageNum)))
                    .andExpect(jsonPath("$.size", is(size)));

            verify(chatService).getMessages(eq(userId), eq(sessionId), eq(PageRequest.of(pageNum, size)));
        }
    }

    @Nested
    @DisplayName("POST /sessions/{id}/messages - send message")
    class SendMessage {

        @Test
        @DisplayName("should send message and return created message dto")
        void send_ok() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            String content = "How are you?";
            MessageDto returned = new MessageDto(UUID.randomUUID(), sessionId, "user", content, Instant.now());
            when(chatService.send(eq(userId), eq(sessionId), eq(content))).thenReturn(returned);

            String body = "{\"content\":\"" + content + "\"}";

            mockMvc.perform(post("/sessions/{id}/messages", sessionId)
                            .with(auth(principal(userId)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body.getBytes(StandardCharsets.UTF_8))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId", is(sessionId.toString())))
                    .andExpect(jsonPath("$.content", is(content)));

            verify(chatService).send(eq(userId), eq(sessionId), eq(content));
        }

        @Test
        @DisplayName("should return 400 Bad Request when content is blank (validation failure)")
        void send_validationError_blank() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();

            // Assuming @Valid enforces non-blank content on SendMessageRequest.content().
            String body = "{\"content\":\"   \"}";

            mockMvc.perform(post("/sessions/{id}/messages", sessionId)
                            .with(auth(principal(userId)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verify(chatService, never()).send(any(), any(), any());
        }

        @Test
        @DisplayName("should propagate exact userId and message content to ChatService")
        void send_verifiesArguments() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            String content = "Ping";
            MessageDto returned = new MessageDto(UUID.randomUUID(), sessionId, "user", content, Instant.now());
            when(chatService.send(any(UUID.class), any(UUID.class), any(String.class))).thenReturn(returned);

            mockMvc.perform(post("/sessions/{id}/messages", sessionId)
                            .with(auth(principal(userId)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"" + content + "\"}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<UUID> userCaptor = ArgumentCaptor.forClass(UUID.class);
            ArgumentCaptor<UUID> sessionCaptor = ArgumentCaptor.forClass(UUID.class);
            ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
            verify(chatService).send(userCaptor.capture(), sessionCaptor.capture(), contentCaptor.capture());

            assert userCaptor.getValue().equals(userId) : "Expected userId to be propagated from AuthPrincipal";
            assert sessionCaptor.getValue().equals(sessionId) : "Expected path variable session id to be propagated";
            assert contentCaptor.getValue().equals(content) : "Expected request content to be propagated";
        }
    }
}