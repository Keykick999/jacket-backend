package org.example.incidentanalyzer.diagnostic.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.incidentanalyzer.config.IncidentProperties;
import org.springframework.stereotype.Component;

@Component
public class DiagnosticToolExecutor {

    private final DiagnosticToolRegistry registry;
    private final ObjectMapper objectMapper;
    private final SensitiveDataMasker masker;
    private final IncidentProperties properties;

    public DiagnosticToolExecutor(
            DiagnosticToolRegistry registry,
            ObjectMapper objectMapper,
            SensitiveDataMasker masker,
            IncidentProperties properties
    ) {
        this.registry = registry;
        this.objectMapper = objectMapper;
        this.masker = masker;
        this.properties = properties;
    }

    public DiagnosticEvidence execute(
            String toolName,
            String argumentsJson,
            AnalysisContext context
    ) {
        context.incrementToolCallCount();

        if (context.deadlineExceeded()) {
            return reject(context, toolName, "분석 시간 제한을 초과했습니다.");
        }
        if (context.exceedsMaxToolCalls()) {
            return reject(
                    context,
                    toolName,
                    "도구 호출 횟수 제한(%d)을 초과했습니다.".formatted(
                            properties.getLlm().getMaxToolCalls()
                    )
            );
        }
        if (!registry.contains(toolName)) {
            return reject(context, toolName, "허용되지 않은 도구입니다: " + toolName);
        }

        DiagnosticTool<?> tool = registry.find(toolName).orElseThrow();

        Object input;
        try {
            input = parseInput(tool, argumentsJson);
            validate(tool, input);
        } catch (InvalidToolParameterException ex) {
            return reject(context, toolName, ex.getMessage());
        } catch (Exception ex) {
            return reject(context, toolName, "도구 파라미터가 올바르지 않습니다: " + ex.getMessage());
        }

        String cacheKey = cacheKey(toolName, input);
        DiagnosticEvidence cached = context.cached(cacheKey);
        if (cached != null) {
            return remember(context, cached);
        }

        try {
            Object data = invoke(tool, input, context);
            DiagnosticEvidence evidence = new DiagnosticEvidence(
                    context.nextEvidenceId(toolName),
                    toolName,
                    toolName,
                    context.instance(),
                    java.time.Instant.now(),
                    EvidenceStatus.SUCCESS,
                    null,
                    truncate(data)
            );
            context.remember(cacheKey, evidence);
            return remember(context, evidence);
        } catch (Exception ex) {
            DiagnosticEvidence failed = DiagnosticEvidence.failed(
                    context.nextEvidenceId(toolName),
                    toolName,
                    toolName,
                    context.instance(),
                    masker.mask(ex.getMessage())
            );
            return remember(context, failed);
        }
    }

    private DiagnosticEvidence reject(AnalysisContext context, String toolName, String reason) {
        DiagnosticEvidence rejected = DiagnosticEvidence.rejected(
                context.nextEvidenceId(toolName == null ? "unknown" : toolName),
                toolName,
                context.instance(),
                reason
        );
        return remember(context, rejected);
    }

    private DiagnosticEvidence remember(AnalysisContext context, DiagnosticEvidence evidence) {
        context.addEvidence(evidence);
        return evidence;
    }

    private Object parseInput(DiagnosticTool<?> tool, String argumentsJson) throws Exception {
        String json = (argumentsJson == null || argumentsJson.isBlank()) ? "{}" : argumentsJson;
        return objectMapper.readValue(json, tool.inputType());
    }

    @SuppressWarnings("unchecked")
    private void validate(DiagnosticTool<?> tool, Object input) {
        DiagnosticTool<Object> typed = (DiagnosticTool<Object>) tool;
        typed.validate(input);
    }

    @SuppressWarnings("unchecked")
    private Object invoke(DiagnosticTool<?> tool, Object input, AnalysisContext context) {
        DiagnosticTool<Object> typed = (DiagnosticTool<Object>) tool;
        return typed.execute(input, context);
    }

    private String cacheKey(String toolName, Object input) {
        try {
            return toolName + ":" + objectMapper.writeValueAsString(input);
        } catch (Exception ex) {
            return toolName + ":" + String.valueOf(input);
        }
    }

    private Object truncate(Object data) {
        try {
            String json = masker.mask(objectMapper.writeValueAsString(data));
            int maxBytes = properties.getLlm().getMaxEvidenceBytes();
            byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (bytes.length <= maxBytes) {
                return objectMapper.readValue(json, Object.class);
            }
            String truncated = new String(bytes, 0, maxBytes, java.nio.charset.StandardCharsets.UTF_8);
            return MapOfTruncation.truncated(truncated);
        } catch (Exception ex) {
            return data;
        }
    }

    private record MapOfTruncation(boolean truncated, String preview) {
        static MapOfTruncation truncated(String preview) {
            return new MapOfTruncation(true, preview);
        }
    }
}
