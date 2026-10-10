package org.example.incidentanalyzer.diagnostic.tool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    private final SensitiveDataMasker masker = new SensitiveDataMasker();

    @Test
    void masksSecretsAndEmails() {
        String masked = masker.mask(
                "Authorization Bearer abcdef.token value password=super-secret user=dev@example.com"
        );

        assertThat(masked).doesNotContain("abcdef.token");
        assertThat(masked).doesNotContain("super-secret");
        assertThat(masked).doesNotContain("dev@example.com");
        assertThat(masked).contains("Bearer ***");
        assertThat(masked).contains("password=***");
    }

    @Test
    void masksGoogleApiKeyMaterial() {
        String masked = masker.mask("request failed for AIzaSyDummyTestKeyValue1234567890");

        assertThat(masked).doesNotContain("AIzaSyDummyTestKeyValue1234567890");
        assertThat(masked).contains("***");
    }
}
