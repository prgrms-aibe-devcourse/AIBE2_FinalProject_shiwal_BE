package com.example.hyu.controller.chat;


import com.example.hyu.dto.chat.CreateSessionResponse;
import com.example.hyu.dto.chat.MessageDto;
import com.example.hyu.dto.chat.SendMessageRequest;
import com.example.hyu.dto.chat.SessionDto;
import com.example.hyu.security.AuthPrincipal;
import com.example.hyu.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sessions")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public CreateSessionResponse create(@AuthenticationPrincipal AuthPrincipal me) {
        return chatService.create(me.getUserId());
    }

    @GetMapping
    public Page<SessionDto> list(@AuthenticationPrincipal AuthPrincipal me,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return chatService.list(me.getUserId(), PageRequest.of(page, size));
    }

    @GetMapping("/{id}/messages")
    public Page<MessageDto> messages(@AuthenticationPrincipal AuthPrincipal me,
                                     @PathVariable UUID id,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "50") int size) {
        return chatService.getMessages(me.getUserId(), id, PageRequest.of(page, size));
    }

    @PostMapping("/{id}/messages")
    public MessageDto send(@AuthenticationPrincipal AuthPrincipal me,
                           @PathVariable UUID id,
                           @Valid @RequestBody SendMessageRequest req) {
        return chatService.send(me.getUserId(), id, req.content());
    }
}