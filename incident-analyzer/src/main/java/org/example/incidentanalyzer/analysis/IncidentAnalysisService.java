package org.example.incidentanalyzer.analysis;

import org.example.incidentanalyzer.alert.AlertRequest;
import org.example.incidentanalyzer.config.IncidentProperties;
import org.example.incidentanalyzer.diagnostic.PrometheusQueryClient;
import org.example.incidentanalyzer.diagnostic.tool.AnalysisContext;
import org.example.incidentanalyzer.diagnostic.tool.DiagnosticEvidence;
import org.example.incidentanalyzer.diagnostic.tool.EvidenceStatus;
import org.example.incidentanalyzer.llm.LlmAnalysisException;
import org.example.incidentanalyzer.llm.LlmIncidentAnalyzer;
import org.example.incidentanalyzer.llm.LlmIncidentReport;
import org.example.incidentanalyzer.slack.SlackNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
public class IncidentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(IncidentAnalysisService.class);

    private final RuleBasedAnalyzer ruleBasedAnalyzer;
    private final LlmIncidentAnalyzer llmIncidentAnalyzer;
    private final PrometheusQueryClient prometheusQueryClient;
    private final SlackNotificationService slackNotificationService;
    private final IncidentProperties properties;
    private final Executor executor;
    private final ConcurrentHashMap<String, Instant> recentAnalyses = new ConcurrentHashMap<>();

    public IncidentAnalysisService(
            RuleBasedAnalyzer ruleBasedAnalyzer,
            LlmIncidentAnalyzer llmIncidentAnalyzer,
            PrometheusQueryClient prometheusQueryClient,
            SlackNotificationService slackNotificationService,
            IncidentProperties properties,
            @Qualifier("incidentAnalysisExecutor") Executor executor
    ) {
        this.ruleBasedAnalyzer = ruleBasedAnalyzer;
        this.llmIncidentAnalyzer = llmIncidentAnalyzer;
        this.prometheusQueryClient = prometheusQueryClient;
        this.slackNotificationService = slackNotificationService;
        this.properties = properties;
        this.executor = executor;
    }

    public void analyzeAsync(AlertRequest request) {
        if (request == null || request.alerts() == null) {
            return;
        }
        for (AlertRequest.Alert alert : request.alerts()) {
            executor.execute(() -> {
                try {
                    IncidentReport report = analyze(alert);
                    if (report != null) {
                        slackNotificationService.sendIncidentReport(report);
                    }
                } catch (Exception ex) {
                    log.error("장애 분석에 실패했습니다.", ex);
                }
            });
        }
    }

    public IncidentReport analyze(AlertRequest.Alert alert) {
        String cacheKey = cacheKey(alert);
        evictExpired();
        Instant previous = recentAnalyses.putIfAbsent(cacheKey, Instant.now());
        if (previous != null && !isExpired(previous)) {
            log.info("중복 Alert를 건너뜁니다. key={}", cacheKey);
            return null;
        }

        AlertAnalysisResult ruleResult = ruleBasedAnalyzer.analyze(alert);

        if ("resolved".equalsIgnoreCase(alert.status())) {
            return new IncidentReport(ruleResult, null, LlmStatus.SKIPPED, "resolved alert");
        }
        if (!properties.getLlm().isEnabled()) {
            return new IncidentReport(ruleResult, null, LlmStatus.DISABLED, "LLM 분석이 비활성화되어 있습니다.");
        }

        Instant incidentTime = alert.startsAt() == null ? Instant.now() : alert.startsAt();
        AnalysisContext context = new AnalysisContext(
                cacheKey,
                alert,
                incidentTime,
                Instant.now().plus(properties.getLlm().getTimeout()),
                properties.getLlm().getMaxToolCalls()
        );
        collectInitialMetrics(context);

        try {
            LlmIncidentReport llmReport = llmIncidentAnalyzer.analyze(ruleResult, context);
            log.info("LLM 분석을 완료했습니다. summary={}", llmReport.summary());
            return new IncidentReport(ruleResult, llmReport, LlmStatus.SUCCESS, null);
        } catch (LlmAnalysisException ex) {
            log.warn("LLM 분석에 실패하여 Rule 결과만 사용합니다.", ex);
            return new IncidentReport(ruleResult, null, LlmStatus.FAILED, ex.getMessage());
        } catch (Exception ex) {
            log.warn("LLM 분석 중 예외가 발생했습니다.", ex);
            String reason = ex.getMessage();
            if (reason == null || reason.isBlank()) {
                reason = ex.getClass().getSimpleName();
            }
            return new IncidentReport(ruleResult, null, LlmStatus.FAILED, reason);
        }
    }

    void collectInitialMetrics(AnalysisContext context) {
        String metric = "process_cpu_usage";
        try {
            Instant start = context.incidentTime().minusSeconds(5 * 60);
            Instant end = context.incidentTime().plusSeconds(5 * 60);
            String query = context.instance() == null || context.instance().isBlank()
                    ? metric
                    : "%s{instance=\"%s\"}".formatted(metric, context.instance().replace("\"", ""));
            var series = prometheusQueryClient.queryRange(query, start, end, 15);
            context.addEvidence(new DiagnosticEvidence(
                    context.nextEvidenceId("initial-metrics"),
                    "query_metrics",
                    "prometheus",
                    context.instance(),
                    Instant.now(),
                    EvidenceStatus.SUCCESS,
                    null,
                    Map.of(
                            "metric", metric,
                            "query", query,
                            "series", series
                    )
            ));
        } catch (Exception ex) {
            context.addEvidence(DiagnosticEvidence.failed(
                    context.nextEvidenceId("initial-metrics"),
                    "query_metrics",
                    "prometheus",
                    context.instance(),
                    "초기 Prometheus 지표를 조회하지 못했습니다: " + ex.getMessage()
            ));
        }
    }

    private String cacheKey(AlertRequest.Alert alert) {
        String fingerprint = alert.fingerprint();
        if (fingerprint == null || fingerprint.isBlank()) {
            fingerprint = String.valueOf(alert.label("alertname"))
                    + "|" + alert.label("instance");
        }
        return fingerprint + "|" + alert.startsAt();
    }

    private void evictExpired() {
        recentAnalyses.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }

    private boolean isExpired(Instant recordedAt) {
        return recordedAt.plus(properties.getLlm().getCacheTtl()).isBefore(Instant.now());
    }
}
