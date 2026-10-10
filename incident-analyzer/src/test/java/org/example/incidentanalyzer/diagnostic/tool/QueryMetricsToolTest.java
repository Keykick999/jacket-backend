package org.example.incidentanalyzer.diagnostic.tool;

import org.example.incidentanalyzer.TestAlerts;
import org.example.incidentanalyzer.diagnostic.PrometheusQueryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryMetricsToolTest {

    private PrometheusQueryClient prometheusQueryClient;
    private QueryMetricsTool tool;

    @BeforeEach
    void setUp() {
        prometheusQueryClient = mock(PrometheusQueryClient.class);
        when(prometheusQueryClient.listMetricNames()).thenReturn(List.of(
                "process_cpu_usage",
                "jvm_threads_live_threads"
        ));
        tool = new QueryMetricsTool(prometheusQueryClient);
    }

    @Test
    void rejectsUnknownMetric() {
        assertThatThrownBy(() -> tool.validate(
                new QueryMetricsRequest("not_a_real_metric", 5, 5, 15)
        )).isInstanceOf(InvalidToolParameterException.class)
                .hasMessageContaining("허용되지 않은 지표");
    }

    @Test
    void rejectsMetricNotPresentInPrometheus() {
        assertThatThrownBy(() -> tool.validate(
                new QueryMetricsRequest("system_cpu_usage", 5, 5, 15)
        )).isInstanceOf(InvalidToolParameterException.class)
                .hasMessageContaining("system_cpu_usage");
    }

    @Test
    void rejectsMinutesBeforeOverLimit() {
        assertThatThrownBy(() -> tool.validate(
                new QueryMetricsRequest("process_cpu_usage", 31, 5, 15)
        )).isInstanceOf(InvalidToolParameterException.class)
                .hasMessageContaining("최대");
    }

    @Test
    void rejectsSmallStep() {
        assertThatThrownBy(() -> tool.validate(
                new QueryMetricsRequest("process_cpu_usage", 5, 5, 1)
        )).isInstanceOf(InvalidToolParameterException.class)
                .hasMessageContaining("stepSeconds");
    }

    @Test
    void acceptsAllowedMetric() {
        tool.validate(new QueryMetricsRequest("process_cpu_usage", 5, 5, 15));
    }

    @Test
    void unusedContextCompiles() {
        AnalysisContext context = new AnalysisContext(
                "a",
                TestAlerts.highCpu("fp"),
                Instant.parse("2026-10-10T01:00:00Z"),
                Instant.now().plusSeconds(30),
                4
        );
        tool.validate(new QueryMetricsRequest("jvm_threads_live_threads", 1, 1, 15));
        assert context.instance() != null;
    }
}
