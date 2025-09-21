package com.example.hyu.assistant;

/*
 Testing library/framework: JUnit 5 (Jupiter) with standard Assertions.
 Focus: Thorough unit tests for DummyAssistantClient.reply(...) covering happy paths, edge cases, and precedence rules.
*/

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DummyAssistantClientTest {

    private final DummyAssistantClient client = new DummyAssistantClient();

    private static final String BLANK_PROMPT = "무슨 생각이 드셨는지 한두 문장으로 적어주실래요?";
    private static final String GREETING_PROMPT = "안녕하세요\! 오늘 기분은 어떠세요? 편하게 말씀해 주세요.";
    private static final String SHORT_PROMPT = "조금만 더 자세히 들려주실 수 있을까요? 어떤 상황이었는지도 함께 알려주세요.";
    private static final String DEFAULT_PROMPT = "말해 주셔서 고마워요. 말씀하신 내용을 보니 꽤 신경 쓰이는 일이었겠어요. 그 상황에서 특히 힘들었던 점 한 가지를 꼽는다면 무엇일까요?";

    private String call(String userMessage) {
        // history and systemPrompt are ignored by DummyAssistantClient, use safe defaults
        return client.reply("ignored-system-prompt", Collections.emptyList(), userMessage);
    }

    @Test
    @DisplayName("Null or whitespace-only input -> request to write a couple of sentences")
    void reply_whenNullOrBlank_returnsBlankPrompt() {
        String[] inputs = new String[]{null, "", " ", "    ", "\n\t  "};
        for (String in : inputs) {
            assertEquals(BLANK_PROMPT, call(in), "Input should yield blank prompt: " + String.valueOf(in));
        }
    }

    @Test
    @DisplayName("Contains '안녕' anywhere -> greeting prompt (takes precedence over length check)")
    void reply_whenContainsGreeting_returnsGreetingPrompt() {
        String[] inputs = new String[]{
                "안녕",
                "안녕하세요",
                "   안녕하세요   ",
                "오늘도 친구에게 안녕이라고 인사했어요",
                "안녕?" // short but greeting has precedence
        };
        for (String in : inputs) {
            assertEquals(GREETING_PROMPT, call(in), "Greeting detection failed for: " + in);
        }
    }

    @Test
    @DisplayName("Short (< 8 chars) non-greeting -> ask for more details")
    void reply_whenShortNonGreeting_returnsAskForDetails() {
        String[] inputs = new String[]{
                "힘들어", // length 3
                "고민",   // length 2
                "short",  // length 5
                "1234567" // length 7
        };
        for (String in : inputs) {
            assertEquals(SHORT_PROMPT, call(in), "Short input should request details: " + in);
        }
    }

    @Test
    @DisplayName("Exactly 8 chars (non-greeting) -> empathetic follow-up")
    void reply_whenExactlyEightCharsNonGreeting_returnsDefault() {
        assertEquals(DEFAULT_PROMPT, call("12345678"));
        assertEquals(DEFAULT_PROMPT, call("abcdefgh"));
    }

    @Test
    @DisplayName("Trimming is applied before rule checks")
    void reply_appliesTrimBeforeRules() {
        assertEquals(SHORT_PROMPT, call("   1234567   "));  // trimmed length 7 -> short
        assertEquals(GREETING_PROMPT, call("   안녕   "));   // trimmed contains greeting
        assertEquals(BLANK_PROMPT, call("   \t  \n  "));    // blank after trim
    }

    @Test
    @DisplayName("Long non-greeting message -> empathetic follow-up")
    void reply_whenLongNonGreeting_returnsDefault() {
        String longMsg = "오늘 회사에서 일이 많아서 많이 지쳤어요. 그래도 내일은 더 나아졌으면 좋겠어요.";
        assertEquals(DEFAULT_PROMPT, call(longMsg));
    }

    @Test
    @DisplayName("Null history and systemPrompt are safely ignored")
    void reply_whenNullHistoryAndSystemPrompt_stillWorks() {
        String result = client.reply(null, null, "안녕하세요");
        assertEquals(GREETING_PROMPT, result);
    }
}