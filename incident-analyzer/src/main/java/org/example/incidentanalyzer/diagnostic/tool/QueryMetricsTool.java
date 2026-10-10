package org.example.incidentanalyzer.diagnostic.tool;

import org.example.incidentanalyzer.diagnostic.PrometheusQueryClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class QueryMetricsTool implements DiagnosticTool<QueryMetricsRequest> {

    static final List<String> CANDIDATE_METRICS = List.of(
            "process_cpu_usage",
            "system_cpu_usage",
            "system_load_average_1m",
            "jvm_threads_live_threads",
            "jvm_threads_states_threads"
    );

    private static final int MAX_MINUTES = 30;
    private static final int MIN_STEP_SECONDS = 15;

    private final PrometheusQueryClient prometheusQueryClient;
    private volatile Set<String> discoveredMetrics;

    public QueryMetricsTool(PrometheusQueryClient prometheusQueryClient) {
        this.prometheusQueryClient = prometheusQueryClient;
    }

    @Override
    public String name() {
        return "query_metrics";
    }

    @Override
    public String description() {
        return "Query Prometheus metrics around the incident time. "
                + "Allowed metrics: " + String.join(", ", CANDIDATE_METRICS);
    }

    @Override
    public Class<QueryMetricsRequest> inputType() {
        return QueryMetricsRequest.class;
    }

    @Override
    public void validate(QueryMetricsRequest input) {
        if (input == null || input.metric() == null || input.metric().isBlank()) {
            throw new InvalidToolParameterException("metric은 필수입니다.");
        }
        if (!allowedMetrics().contains(input.metric())) {
            throw new InvalidToolParameterException(
                    "허용되지 않은 지표입니다: " + input.metric()
            );
        }
        if (minutes(input.minutesBefore()) > MAX_MINUTES
                || minutes(input.minutesAfter()) > MAX_MINUTES) {
            throw new InvalidToolParameterException(
                    "조회 범위는 전후 각각 최대 %d분입니다.".formatted(MAX_MINUTES)
            );
        }
        if (input.minutesBefore() != null && input.minutesBefore() < 0) {
            throw new InvalidToolParameterException("minutesBefore는 0 이상이어야 합니다.");
        }
        if (input.minutesAfter() != null && input.minutesAfter() < 0) {
            throw new InvalidToolParameterException("minutesAfter는 0 이상이어야 합니다.");
        }
        if (input.stepSeconds() != null && input.stepSeconds() < MIN_STEP_SECONDS) {
            throw new InvalidToolParameterException(
                    "stepSeconds는 %d 이상이어야 합니다.".formatted(MIN_STEP_SECONDS)
            );
        }
    }

    @Override
    public Object execute(QueryMetricsRequest input, AnalysisContext context) {
        Instant incidentTime = context.incidentTime();
        Instant start = incidentTime.minus(Duration.ofMinutes(minutes(input.minutesBefore(), 5)));
        Instant end = incidentTime.plus(Duration.ofMinutes(minutes(input.minutesAfter(), 5)));
        int step = input.stepSeconds() == null ? MIN_STEP_SECONDS : input.stepSeconds();

        String query = buildQuery(input.metric(), context.instance());
        List<PrometheusQueryClient.MetricSeries> series =
                prometheusQueryClient.queryRange(query, start, end, step);

        return Map.of(
                "metric", input.metric(),
                "query", query,
                "start", start.toString(),
                "end", end.toString(),
                "stepSeconds", step,
                "series", series
        );
    }

    Set<String> allowedMetrics() {
        Set<String> candidates = new LinkedHashSet<>(CANDIDATE_METRICS);
        Set<String> discovered = discoveredMetrics();
        if (discovered.isEmpty()) {
            return candidates;
        }
        candidates.retainAll(discovered);
        return candidates.isEmpty() ? Set.copyOf(CANDIDATE_METRICS) : candidates;
    }

    private Set<String> discoveredMetrics() {
        if (discoveredMetrics != null) {
            return discoveredMetrics;
        }
        synchronized (this) {
            if (discoveredMetrics == null) {
                try {
                    discoveredMetrics = Set.copyOf(prometheusQueryClient.listMetricNames());
                } catch (Exception ex) {
                    discoveredMetrics = Set.of();
                }
            }
            return discoveredMetrics;
        }
    }

    void resetDiscoveredMetrics() {
        discoveredMetrics = null;
    }

    private String buildQuery(String metric, String instance) {
        if (instance == null || instance.isBlank()) {
            return metric;
        }
        return "%s{instance=\"%s\"}".formatted(metric, instance.replace("\"", ""));
    }

    private int minutes(Integer value) {
        return value == null ? 0 : value;
    }

    private int minutes(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }
}
