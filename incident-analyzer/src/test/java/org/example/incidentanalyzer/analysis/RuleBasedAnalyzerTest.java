package org.example.incidentanalyzer.analysis;

import org.example.incidentanalyzer.TestAlerts;
import org.example.incidentanalyzer.alert.AlertRequest;
import org.example.incidentanalyzer.alert.AlertType;
import org.example.incidentanalyzer.rule.AlertAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleBasedAnalyzerTest {

    @Test
    void selectsMatchingAnalyzer() {
        AlertAnalyzer highCpu = new AlertAnalyzer() {
            @Override
            public boolean supports(AlertType alertType) {
                return alertType == AlertType.HIGH_CPU;
            }

            @Override
            public AlertAnalysisResult analyze(AlertRequest.Alert alert) {
                return new AlertAnalysisResult("JacketHighCPU", "warning", "localhost", "ok");
            }
        };

        RuleBasedAnalyzer analyzer = new RuleBasedAnalyzer(List.of(highCpu));
        AlertAnalysisResult result = analyzer.analyze(TestAlerts.highCpu("fp-rule"));

        assertThat(result.analysis()).isEqualTo("ok");
    }

    @Test
    void rejectsUnknownAlertName() {
        RuleBasedAnalyzer analyzer = new RuleBasedAnalyzer(List.of());
        AlertRequest.Alert unknown = new AlertRequest.Alert(
                "firing",
                Map.of("alertname", "SomethingElse"),
                Map.of(),
                null,
                null,
                "fp"
        );

        assertThatThrownBy(() -> analyzer.analyze(unknown))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown alert");
    }
}
