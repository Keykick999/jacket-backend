package org.example.incidentanalyzer.analysis;

import org.example.incidentanalyzer.TestAlerts;
import org.example.incidentanalyzer.alert.AlertRequest;
import org.example.incidentanalyzer.config.IncidentProperties;
import org.example.incidentanalyzer.diagnostic.PrometheusQueryClient;
import org.example.incidentanalyzer.llm.LlmAnalysisException;
import org.example.incidentanalyzer.llm.LlmIncidentAnalyzer;
import org.example.incidentanalyzer.slack.SlackNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncidentAnalysisServiceTest {

    private RuleBasedAnalyzer ruleBasedAnalyzer;
    private LlmIncidentAnalyzer llmIncidentAnalyzer;
    private SlackNotificationService slackNotificationService;
    private IncidentProperties properties;
    private IncidentAnalysisService service;
    private AlertAnalysisResult ruleResult;

    @BeforeEach
    void setUp() {
        ruleBasedAnalyzer = mock(RuleBasedAnalyzer.class);
        llmIncidentAnalyzer = mock(LlmIncidentAnalyzer.class);
        slackNotificationService = mock(SlackNotificationService.class);
        PrometheusQueryClient prometheusQueryClient = mock(PrometheusQueryClient.class);
        properties = new IncidentProperties();
        properties.getLlm().setEnabled(true);

        service = new IncidentAnalysisService(
                ruleBasedAnalyzer,
                llmIncidentAnalyzer,
                prometheusQueryClient,
                slackNotificationService,
                properties,
                Runnable::run
        );

        ruleResult = new AlertAnalysisResult(
                "JacketHighCPU",
                "warning",
                "host.docker.internal:8080",
                "JFR CPU 분석 결과"
        );
        when(ruleBasedAnalyzer.analyze(any())).thenReturn(ruleResult);
    }

    @Test
    void fallsBackToRuleResultWhenLlmFails() {
        when(llmIncidentAnalyzer.analyze(any(), any()))
                .thenThrow(new LlmAnalysisException("timeout"));

        IncidentReport report = service.analyze(TestAlerts.highCpu("fp-fail"));

        assertThat(report.llmStatus()).isEqualTo(LlmStatus.FAILED);
        assertThat(report.ruleResult()).isEqualTo(ruleResult);
        assertThat(report.llmReport()).isNull();
        assertThat(report.llmFailureReason()).contains("timeout");
    }

    @Test
    void slackStillReceivesRuleResultWhenLlmFails() {
        when(llmIncidentAnalyzer.analyze(any(), any()))
                .thenThrow(new LlmAnalysisException("timeout"));

        service.analyzeAsync(new AlertRequest(
                "incident-analyzer",
                "firing",
                java.util.List.of(TestAlerts.highCpu("fp-slack-fail"))
        ));

        verify(slackNotificationService).sendIncidentReport(any(IncidentReport.class));

        SlackNotificationService realSlack = new SlackNotificationService("http://localhost/dummy");
        String message = realSlack.formatIncidentReport(new IncidentReport(
                ruleResult,
                null,
                LlmStatus.FAILED,
                "timeout"
        ));
        assertThat(message).contains("[Rule 기반 분석 (정량)]");
        assertThat(message).contains("JFR CPU 분석 결과");
        assertThat(message).contains("LLM 분석");
        assertThat(message).contains("timeout");
    }

    @Test
    void skipsLlmWhenDisabled() {
        properties.getLlm().setEnabled(false);

        IncidentReport report = service.analyze(TestAlerts.highCpu("fp-disabled"));

        assertThat(report.llmStatus()).isEqualTo(LlmStatus.DISABLED);
        verify(llmIncidentAnalyzer, never()).analyze(any(), any());
    }

    @Test
    void skipsLlmForResolvedAlert() {
        AlertRequest.Alert resolved = new AlertRequest.Alert(
                "resolved",
                TestAlerts.highCpu("fp-resolved").labels(),
                TestAlerts.highCpu("fp-resolved").annotations(),
                Instant.parse("2026-10-10T01:00:00Z"),
                Instant.parse("2026-10-10T01:05:00Z"),
                "fp-resolved"
        );

        IncidentReport report = service.analyze(resolved);

        assertThat(report.llmStatus()).isEqualTo(LlmStatus.SKIPPED);
        verify(llmIncidentAnalyzer, never()).analyze(any(), any());
    }
}
