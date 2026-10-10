package org.example.incidentanalyzer.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * LLM이 켜져 있을 때 GEMINI_API_KEY가 없으면 원인을 바로 알 수 있게 실패시킨다.
 * 키 값은 예외 메시지에 넣지 않는다.
 */
public class GeminiApiKeyEnvironmentValidator
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    static final String MISSING_KEY_MESSAGE =
            "GEMINI_API_KEY 환경 변수가 없거나 비어 있습니다. "
                    + "Windows 사용자 환경 변수에 GEMINI_API_KEY를 설정한 뒤 "
                    + "IDE와 터미널을 재시작하고 다시 실행하세요. "
                    + "API 키를 application.properties나 소스 코드에 넣지 마세요.";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        validate(event.getEnvironment());
    }

    void validate(Environment environment) {
        boolean llmEnabled = environment.getProperty(
                "incident.llm.enabled",
                Boolean.class,
                true
        );
        if (!llmEnabled) {
            return;
        }

        String key = firstNonBlank(
                environment.getProperty("GEMINI_API_KEY"),
                environment.getProperty("spring.ai.google.genai.api-key")
        );
        if (key == null || key.isBlank() || isPlaceholder(key)) {
            throw new IllegalStateException(MISSING_KEY_MESSAGE);
        }
    }

    private boolean isPlaceholder(String key) {
        return "dummy-not-set".equalsIgnoreCase(key)
                || "unset".equalsIgnoreCase(key)
                || "${GEMINI_API_KEY}".equals(key)
                || "${GEMINI_API_KEY:}".equals(key);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
