package com.example.hyu.assistant;

import com.example.hyu.dto.chat.MessageDto;

import java.util.List;

public interface AssistantClient {
    /**
     * @param systemPrompt 세션의 시스템 프롬프트(프로필 기반)
     * @param history      최근 대화(필요 시 일부만 전달)
     * @param userMessage  사용자의 최신 입력
     * @return             어시스턴트의 응답 텍스트
     */
    String reply(String systemPrompt, List<MessageDto> history, String userMessage);
}
