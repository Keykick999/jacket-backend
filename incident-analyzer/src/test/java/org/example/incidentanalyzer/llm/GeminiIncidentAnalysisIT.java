package org.example.incidentanalyzer.llm;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("gemini")
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
@TestPropertySource(properties = {
        "incident.llm.enabled=true",
        "spring.ai.model.chat=google-genai",
        "spring.ai.google.genai.api-key=${GEMINI_API_KEY}"
})
class GeminiIncidentAnalysisIT {

    @Autowired
    private ChatModel chatModel;

    @Test
    void canCallGemini() {
        ChatResponse response = chatModel.call(new Prompt(new UserMessage("Reply with the word ok.")));
        assertThat(response.getResult().getOutput().getText()).isNotBlank();
    }

    @Test
    void canCallGeminiWithJsonMimeAndNoNativeTools() {
        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model("gemini-3.8-flash")
                .temperature(0.2)
                .responseMimeType("application/json")
                .thinkingLevel(GoogleGenAiThinkingLevel.LOW)
                .includeServerSideToolInvocations(false)
                .build();
        ChatResponse response = chatModel.call(new Prompt(
                List.of(new UserMessage("Return JSON {\"status\":\"ok\"} and nothing else.")),
                options
        ));
        assertThat(response.getResult().getOutput().getText()).contains("ok");
    }
}
