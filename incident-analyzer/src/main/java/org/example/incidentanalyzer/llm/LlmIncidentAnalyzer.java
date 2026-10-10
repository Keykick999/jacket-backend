package org.example.incidentanalyzer.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.incidentanalyzer.analysis.AlertAnalysisResult;
import org.example.incidentanalyzer.config.IncidentProperties;
import org.example.incidentanalyzer.diagnostic.tool.AnalysisContext;
import org.example.incidentanalyzer.diagnostic.tool.DiagnosticEvidence;
import org.example.incidentanalyzer.diagnostic.tool.DiagnosticToolExecutor;
import org.example.incidentanalyzer.diagnostic.tool.DiagnosticToolRegistry;
import org.example.incidentanalyzer.diagnostic.tool.SensitiveDataMasker;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class LlmIncidentAnalyzer {

    static final String COMPACT_OUTPUT_FORMAT = """
            {
              "summary": "string",
              "causeCandidates": [
                {
                  "title": "string",
                  "confidence": "HIGH|MEDIUM|LOW",
                  "reasoning": "string",
                  "evidenceIds": ["evidence-id"],
                  "isInference": false
                }
              ],
              "uncertainties": ["string"],
              "followUpChecks": ["string"],
              "recommendedActions": ["string"],
              "ruleComparison": "string"
            }
            """;

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final DiagnosticToolRegistry toolRegistry;
    private final DiagnosticToolExecutor toolExecutor;
    private final IncidentPromptFactory promptFactory;
    private final LlmReportParser reportParser;
    private final ObjectMapper objectMapper;
    private final IncidentProperties properties;
    private final SensitiveDataMasker masker;
    private final String model;

    public LlmIncidentAnalyzer(
            ObjectProvider<ChatModel> chatModelProvider,
            DiagnosticToolRegistry toolRegistry,
            DiagnosticToolExecutor toolExecutor,
            IncidentPromptFactory promptFactory,
            LlmReportParser reportParser,
            ObjectMapper objectMapper,
            IncidentProperties properties,
            SensitiveDataMasker masker,
            @Value("${spring.ai.google.genai.chat.options.model:gemini-3.8-flash}") String model
    ) {
        this.chatModelProvider = chatModelProvider;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.promptFactory = promptFactory;
        this.reportParser = reportParser;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.masker = masker;
        this.model = model;
    }

    public LlmIncidentReport analyze(
            AlertAnalysisResult ruleResult,
            AnalysisContext context
    ) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new LlmAnalysisException("ChatModel이 구성되지 않았습니다.");
        }

        String outputFormat = COMPACT_OUTPUT_FORMAT;

        List<Message> history = new ArrayList<>();
        history.add(new SystemMessage(promptFactory.systemPrompt()));
        history.add(new UserMessage(promptFactory.initialUserPrompt(
                context.alert(),
                ruleResult,
                context.evidence(),
                toolRegistry.toolDefinitions(),
                outputFormat
        )));

        String firstText = requireText(callModel(
                chatModel,
                new Prompt(history, jsonOptions()),
                properties.getLlm().getTimeout()
        ));

        List<RequestedToolCall> toolCalls = reportParser.parseToolCalls(firstText);
        if (!toolCalls.isEmpty()) {
            List<DiagnosticEvidence> toolEvidence = executeRequestedTools(toolCalls, context);
            history.add(new UserMessage(firstText));
            history.add(new UserMessage(promptFactory.toolResultsPrompt(toolEvidence, outputFormat)));
            firstText = requireText(callModel(
                    chatModel,
                    new Prompt(history, jsonOptions()),
                    remainingTimeout(context)
            ));
        }

        Set<String> knownIds = context.evidence().stream()
                .map(DiagnosticEvidence::evidenceId)
                .collect(Collectors.toSet());
        return reportParser.parse(firstText, knownIds);
    }

    List<DiagnosticEvidence> executeRequestedTools(
            List<RequestedToolCall> toolCalls,
            AnalysisContext context
    ) {
        List<DiagnosticEvidence> evidence = new ArrayList<>();
        for (RequestedToolCall toolCall : toolCalls) {
            evidence.add(toolExecutor.execute(
                    toolCall.name(),
                    toolCall.argumentsJson(),
                    context
            ));
        }
        return evidence;
    }

    ChatResponse callModel(ChatModel chatModel, Prompt prompt, Duration timeout) {
        try {
            return CompletableFuture
                    .supplyAsync(() -> chatModel.call(prompt))
                    .orTimeout(Math.max(timeout.toMillis(), 1), TimeUnit.MILLISECONDS)
                    .join();
        } catch (Exception ex) {
            throw new LlmAnalysisException("LLM 호출에 실패했습니다: " + rootMessage(ex), ex);
        }
    }

    private String requireText(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new LlmAnalysisException("LLM 응답이 비어 있습니다.");
        }
        String text = response.getResult().getOutput().getText();
        if (text == null || text.isBlank()) {
            throw new LlmAnalysisException("LLM 응답이 비어 있습니다.");
        }
        return text;
    }

    private GoogleGenAiChatOptions jsonOptions() {
        return GoogleGenAiChatOptions.builder()
                .model(model)
                .temperature(0.2)
                .responseMimeType("application/json")
                .thinkingLevel(GoogleGenAiThinkingLevel.LOW)
                .includeServerSideToolInvocations(false)
                .build();
    }

    private Duration remainingTimeout(AnalysisContext context) {
        Duration remaining = Duration.between(java.time.Instant.now(), context.deadline());
        Duration floor = Duration.ofSeconds(30);
        if (remaining.compareTo(floor) < 0) {
            return floor;
        }
        return remaining;
    }

    private String rootMessage(Throwable ex) {
        StringBuilder chain = new StringBuilder();
        Throwable current = ex;
        int depth = 0;
        while (current != null && depth < 8) {
            if (chain.length() > 0) {
                chain.append(" ← ");
            }
            chain.append(current.getClass().getSimpleName());
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                chain.append(": ").append(current.getMessage());
            }
            if (current.getCause() == null || current.getCause() == current) {
                break;
            }
            current = current.getCause();
            depth++;
        }
        return masker.mask(chain.toString());
    }
}
