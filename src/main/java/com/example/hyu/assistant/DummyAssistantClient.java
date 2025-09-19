package com.example.hyu.assistant;

import com.example.hyu.dto.chat.MessageDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DummyAssistantClient implements AssistantClient {

    @Override
    public String reply(String systemPrompt, List<MessageDto> history, String userMessage) {
        // 아주 단순한 룰/템플릿 기반 응답 (향후 LLM으로 교체)
        String msg = userMessage == null ? "" : userMessage.trim();

        if (msg.isBlank()) return "무슨 생각이 드셨는지 한두 문장으로 적어주실래요?";
        if (msg.contains("안녕") || msg.contains("안녕하세요"))
            return "안녕하세요! 오늘 기분은 어떠세요? 편하게 말씀해 주세요.";

        if (msg.length() < 8)
            return "조금만 더 자세히 들려주실 수 있을까요? 어떤 상황이었는지도 함께 알려주세요.";

        return "말해 주셔서 고마워요. 말씀하신 내용을 보니 꽤 신경 쓰이는 일이었겠어요. "
                + "그 상황에서 특히 힘들었던 점 한 가지를 꼽는다면 무엇일까요?";
    }
}
