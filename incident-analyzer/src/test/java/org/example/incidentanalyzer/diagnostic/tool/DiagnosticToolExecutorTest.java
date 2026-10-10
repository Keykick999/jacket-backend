package org.example.incidentanalyzer.diagnostic.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.incidentanalyzer.TestAlerts;
import org.example.incidentanalyzer.config.IncidentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticToolExecutorTest {

    private final AtomicInteger executions = new AtomicInteger();
    private DiagnosticToolExecutor executor;
    private AnalysisContext context;

    @BeforeEach
    void setUp() {
        executions.set(0);
        IncidentProperties properties = new IncidentProperties();
        properties.getLlm().setMaxToolCalls(2);
        properties.getLlm().setMaxEvidenceBytes(4096);

        DiagnosticTool<Map> tool = new DiagnosticTool<>() {
            @Override
            public String name() {
                return "query_metrics";
            }

            @Override
            public String description() {
                return "test tool";
            }

            @Override
            public Class<Map> inputType() {
                return Map.class;
            }

            @Override
            public void validate(Map input) {
                if (input.containsKey("invalid")) {
                    throw new InvalidToolParameterException("잘못된 파라미터입니다.");
                }
            }

            @Override
            public Object execute(Map input, AnalysisContext analysisContext) {
                executions.incrementAndGet();
                return Map.of("ok", true, "metric", input.get("metric"));
            }
        };

        executor = new DiagnosticToolExecutor(
                new DiagnosticToolRegistry(List.of(tool)),
                new ObjectMapper().registerModule(new JavaTimeModule()),
                new SensitiveDataMasker(),
                properties
        );
        context = newContext(Instant.now().plusSeconds(30));
    }

    @Test
    void executesAllowedTool() {
        DiagnosticEvidence evidence = executor.execute(
                "query_metrics",
                "{\"metric\":\"process_cpu_usage\"}",
                context
        );

        assertThat(evidence.status()).isEqualTo(EvidenceStatus.SUCCESS);
        assertThat(executions.get()).isEqualTo(1);
        assertThat(evidence.evidenceId()).startsWith("ev-query_metrics-");
    }

    @Test
    void rejectsUnknownTool() {
        DiagnosticEvidence evidence = executor.execute(
                "search_logs",
                "{}",
                context
        );

        assertThat(evidence.status()).isEqualTo(EvidenceStatus.REJECTED);
        assertThat(evidence.reason()).contains("허용되지 않은 도구");
        assertThat(executions.get()).isZero();
    }

    @Test
    void rejectsInvalidParameters() {
        DiagnosticEvidence evidence = executor.execute(
                "query_metrics",
                "{\"invalid\":true}",
                context
        );

        assertThat(evidence.status()).isEqualTo(EvidenceStatus.REJECTED);
        assertThat(evidence.reason()).contains("잘못된 파라미터");
        assertThat(executions.get()).isZero();
    }

    @Test
    void rejectsMalformedJson() {
        DiagnosticEvidence evidence = executor.execute(
                "query_metrics",
                "{not-json",
                context
        );

        assertThat(evidence.status()).isEqualTo(EvidenceStatus.REJECTED);
        assertThat(evidence.reason()).contains("파라미터가 올바르지 않습니다");
        assertThat(executions.get()).isZero();
    }

    @Test
    void cachesDuplicateRequests() {
        executor.execute("query_metrics", "{\"metric\":\"process_cpu_usage\"}", context);
        DiagnosticEvidence second = executor.execute(
                "query_metrics",
                "{\"metric\":\"process_cpu_usage\"}",
                context
        );

        assertThat(second.status()).isEqualTo(EvidenceStatus.SUCCESS);
        assertThat(executions.get()).isEqualTo(1);
    }

    @Test
    void rejectsWhenToolCallLimitExceeded() {
        executor.execute("query_metrics", "{\"metric\":\"a\"}", context);
        executor.execute("query_metrics", "{\"metric\":\"b\"}", context);
        DiagnosticEvidence third = executor.execute(
                "query_metrics",
                "{\"metric\":\"c\"}",
                context
        );

        assertThat(third.status()).isEqualTo(EvidenceStatus.REJECTED);
        assertThat(third.reason()).contains("횟수 제한");
        assertThat(executions.get()).isEqualTo(2);
    }

    private AnalysisContext newContext(Instant deadline) {
        return new AnalysisContext(
                "analysis-1",
                TestAlerts.highCpu("fp-1"),
                Instant.parse("2026-10-10T01:00:00Z"),
                deadline,
                2
        );
    }
}
