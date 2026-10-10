package org.example.incidentanalyzer.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.incidentanalyzer.TestAlerts;
import org.example.incidentanalyzer.analysis.AlertAnalysisResult;
import org.example.incidentanalyzer.config.IncidentProperties;
import org.example.incidentanalyzer.diagnostic.tool.AnalysisContext;
import org.example.incidentanalyzer.diagnostic.tool.DiagnosticEvidence;
import org.example.incidentanalyzer.diagnostic.tool.DiagnosticTool;
import org.example.incidentanalyzer.diagnostic.tool.DiagnosticToolExecutor;
import org.example.incidentanalyzer.diagnostic.tool.DiagnosticToolRegistry;
import org.example.incidentanalyzer.diagnostic.tool.SensitiveDataMasker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LlmIncidentAnalyzerTest {

    private final AtomicInteger executions = new AtomicInteger();
    private ChatModel chatModel;
    private LlmIncidentAnalyzer analyzer;
    private AnalysisContext context;
    private AlertAnalysisResult ruleResult;

    @BeforeEach
    void setUp() {
        executions.set(0);
        chatModel = mock(ChatModel.class);

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        IncidentProperties properties = new IncidentProperties();
        properties.getLlm().setTimeout(java.time.Duration.ofSeconds(10));
        properties.getLlm().setMaxToolCalls(4);

        DiagnosticTool<Map> tool = new DiagnosticTool<>() {
            @Override
            public String name() {
                return "analyze_jfr";
            }

            @Override
            public String description() {
                return "analyze jfr";
            }

            @Override
            public Class<Map> inputType() {
                return Map.class;
            }

            @Override
            public void validate(Map input) {
            }

            @Override
            public Object execute(Map input, AnalysisContext analysisContext) {
                executions.incrementAndGet();
                return Map.of("methods", List.of("CpuTestController.burn()"));
            }
        };

        DiagnosticToolRegistry registry = new DiagnosticToolRegistry(List.of(tool));
        DiagnosticToolExecutor executor = new DiagnosticToolExecutor(
                registry,
                objectMapper,
                new SensitiveDataMasker(),
                properties
        );

        ObjectProvider<ChatModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(chatModel);

        analyzer = new LlmIncidentAnalyzer(
                provider,
                registry,
                executor,
                new IncidentPromptFactory(objectMapper),
                new LlmReportParser(objectMapper),
                objectMapper,
                properties,
                new SensitiveDataMasker(),
                "gemini-3.8-flash"
        );

        context = new AnalysisContext(
                "analysis-llm",
                TestAlerts.highCpu("fp-llm"),
                Instant.parse("2026-10-10T01:00:00Z"),
                Instant.now().plusSeconds(30),
                4
        );
        context.addEvidence(new DiagnosticEvidence(
                "ev-initial-metrics-1",
                "query_metrics",
                "prometheus",
                "host.docker.internal:8080",
                Instant.now(),
                org.example.incidentanalyzer.diagnostic.tool.EvidenceStatus.SUCCESS,
                null,
                Map.of("metric", "process_cpu_usage")
        ));
        ruleResult = new AlertAnalysisResult(
                "JacketHighCPU",
                "warning",
                "host.docker.internal:8080",
                "CPU 사용량 증가가 감지되었습니다."
        );
    }

    @Test
    void executesAllowedToolAndPassesResultToFinalPrompt() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(toolCallResponse())
                .thenReturn(finalJsonResponse());

        LlmIncidentReport report = analyzer.analyze(ruleResult, context);

        assertThat(executions.get()).isEqualTo(1);
        assertThat(report.summary()).contains("CPU");
        assertThat(context.evidence().stream().map(DiagnosticEvidence::toolName))
                .contains("analyze_jfr");

        ArgumentCaptor<Prompt> prompts = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(2)).call(prompts.capture());
        List<Message> secondMessages = prompts.getAllValues().get(1).getInstructions();
        assertThat(secondMessages)
                .filteredOn(UserMessage.class::isInstance)
                .map(Message::getText)
                .anyMatch(text -> text != null && text.contains("CpuTestController"));
    }

    @Test
    void describesNullPointerCauseInsteadOfLiteralNull() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new NullPointerException());

        assertThatThrownBy(() -> analyzer.analyze(ruleResult, context))
                .isInstanceOf(LlmAnalysisException.class)
                .hasMessageContaining("NullPointerException")
                .hasMessageNotContaining("실패했습니다: null");
    }

    @Test
    void rejectsUnknownToolWithoutExecutingRegisteredTool() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(unknownToolCallResponse())
                .thenReturn(finalJsonResponse());

        analyzer.analyze(ruleResult, context);

        assertThat(executions.get()).isZero();
        assertThat(context.evidence())
                .anyMatch(evidence ->
                        evidence.toolName().equals("search_logs")
                                && evidence.status() == org.example.incidentanalyzer.diagnostic.tool.EvidenceStatus.REJECTED
                );
    }

    private ChatResponse toolCallResponse() {
        return jsonResponse("""
                {"toolCalls":[{"name":"analyze_jfr","arguments":{"topN":5}}]}
                """);
    }

    private ChatResponse unknownToolCallResponse() {
        return jsonResponse("""
                {"toolCalls":[{"name":"search_logs","arguments":{"query":"error"}}]}
                """);
    }

    private ChatResponse jsonResponse(String json) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
    }

    private ChatResponse finalJsonResponse() {
        AssistantMessage assistant = new AssistantMessage("""
                {
                  "summary": "CPU 사용량이 높습니다",
                  "causeCandidates": [],
                  "uncertainties": [],
                  "followUpChecks": [],
                  "recommendedActions": [],
                  "ruleComparison": "일치"
                }
                """);
        return new ChatResponse(List.of(new Generation(assistant)));
    }
}
