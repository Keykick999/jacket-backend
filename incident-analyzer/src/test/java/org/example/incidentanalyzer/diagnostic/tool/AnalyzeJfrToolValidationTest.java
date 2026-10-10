package org.example.incidentanalyzer.diagnostic.tool;

import org.example.incidentanalyzer.diagnostic.JfrCpuAnalyzer;
import org.example.incidentanalyzer.diagnostic.JfrFileResolver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AnalyzeJfrToolValidationTest {

    private final AnalyzeJfrTool tool = new AnalyzeJfrTool(
            mock(JfrCpuAnalyzer.class),
            mock(JfrFileResolver.class)
    );

    @Test
    void rejectsTopNOutOfRange() {
        assertThatThrownBy(() -> tool.validate(new AnalyzeJfrRequest(11)))
                .isInstanceOf(InvalidToolParameterException.class)
                .hasMessageContaining("topN");
    }

    @Test
    void acceptsNullTopN() {
        tool.validate(new AnalyzeJfrRequest(null));
        tool.validate(new AnalyzeJfrRequest(5));
    }
}
