package com.example.hyu.controller.chat;

import com.example.hyu.dto.user.ProfileChatMessageDto;
import com.example.hyu.dto.user.ProfileChatSummaryDto;
import com.example.hyu.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 프로젝트에 맞게 조정
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profiles/me/chats")
public class ProfileChatController {

    private final ProfileService profileService;

    // 최근 메시지 피드 (세션 섞어서 최신순)
    @GetMapping("/recent")
    public Page<ProfileChatMessageDto> recent(
            @AuthenticationPrincipal(expression = "userId") Long userId, // 프로젝트에 맞게 principal 추출
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return profileService.getMyRecentChatMessages(userId, PageRequest.of(page, size));
    }

    // 세션별 최신 1건 요약 (세션 카드/목록용)
    @GetMapping("/summaries")
    public List<ProfileChatSummaryDto> summaries(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return profileService.getMyLatestPerSession(userId, PageRequest.of(page, size));
    }
}
