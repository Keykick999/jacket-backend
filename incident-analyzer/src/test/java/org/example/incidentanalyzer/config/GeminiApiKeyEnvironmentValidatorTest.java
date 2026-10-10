package org.example.incidentanalyzer.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiApiKeyEnvironmentValidatorTest {

    private final GeminiApiKeyEnvironmentValidator validator =
            new GeminiApiKeyEnvironmentValidator();

    @Test
    void skipsValidationWhenLlmDisabled() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("incident.llm.enabled", "false");

        assertThatCode(() -> validator.validate(environment))
                .doesNotThrowAnyException();
    }

    @Test
    void failsWithClearMessageWhenKeyMissingAndLlmEnabled() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("incident.llm.enabled", "true");

        assertThatThrownBy(() -> validator.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GEMINI_API_KEY")
                .hasMessageNotContaining("AIza");
    }

    @Test
    void acceptsEnvironmentVariableWhenPresent() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("incident.llm.enabled", "true");
        environment.setProperty("GEMINI_API_KEY", "present-but-not-logged");

        assertThatCode(() -> validator.validate(environment))
                .doesNotThrowAnyException();
    }
}
