package org.example.incidentanalyzer.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.incidentanalyzer.analysis.AlertAnalysisResult;
import org.example.incidentanalyzer.diagnostic.tool.DiagnosticEvidence;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class IncidentPromptFactory {

    private static final Set<String> ALLOWED_LABELS =
            Set.of("alertname", "severity", "instance", "service");
    private static final Set<String> ALLOWED_ANNOTATIONS =
            Set.of("summary", "description");

    private final ObjectMapper objectMapper;

    public IncidentPromptFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String systemPrompt() {
        return """
                당신은 Java 애플리케이션 장애를 분석하는 엔지니어입니다.
                서버가 제공한 진단 자료만 근거로 사용하십시오.
                제공되지 않은 로그, 지표, 파일, 코드를 사실처럼 쓰지 마십시오.
                각 원인 후보의 evidenceIds에는 실제로 전달된 evidenceId만 넣으십시오.
                observedFacts와 추론을 구분하고, isInference로 표시하십시오.
                Rule 기반 정량 분석과 판단이 다르면 숨기지 말고 ruleComparison에 적으십시오.
                확실하지 않으면 uncertainties와 followUpChecks에 명시하십시오.
                """;
    }

    public String initialUserPrompt(
            org.example.incidentanalyzer.alert.AlertRequest.Alert alert,
            AlertAnalysisResult ruleResult,
            List<DiagnosticEvidence> initialEvidence,
            List<ToolDefinition> allowedTools,
            String outputFormat
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("alert", sanitizeAlert(alert));
        payload.put("ruleBasedAnalysis", ruleResult);
        payload.put("initialEvidence", initialEvidence);
        payload.put("allowedTools", toToolPayload(allowedTools));
        payload.put("instructions", """
                1차 분석입니다.
                추가 증거가 필요하면 최종 보고서 대신 아래 JSON만 반환하십시오:
                {"toolCalls":[{"name":"도구이름","arguments":{}}]}
                allowedTools에 있는 도구만 요청할 수 있습니다. search_logs는 지원하지 않습니다.
                현재 자료로 충분하면 최종 분석 JSON을 바로 반환하십시오.
                """);
        payload.put("finalOutputHint", outputFormat);

        return write(payload);
    }

    public String toolResultsPrompt(
            List<DiagnosticEvidence> toolEvidence,
            String outputFormat
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("toolResults", toolEvidence);
        payload.put("instructions", """
                요청한 도구 실행 결과입니다. 최종 분석 JSON만 작성하십시오.
                제공된 evidenceId만 근거로 사용하십시오.
                """);
        payload.put("finalOutputHint", outputFormat);
        return write(payload);
    }

    public String finalAnalysisInstruction(String outputFormat) {
        return """
                추가 진단 자료를 반영하여 최종 분석 JSON만 작성하십시오.
                제공된 evidenceId만 근거로 사용하고, 아래 형식을 지키십시오.
                
                %s
                """.formatted(outputFormat);
    }

    public String noAdditionalEvidenceInstruction(String outputFormat) {
        return """
                추가 진단 자료 요청이 없었습니다. 현재 자료만으로 최종 분석 JSON을 작성하십시오.
                
                %s
                """.formatted(outputFormat);
    }

    private Map<String, Object> sanitizeAlert(
            org.example.incidentanalyzer.alert.AlertRequest.Alert alert
    ) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        sanitized.put("status", alert.status());
        sanitized.put("startsAt", alert.startsAt());
        sanitized.put("fingerprint", alert.fingerprint());
        sanitized.put("labels", filterMap(alert.labels(), ALLOWED_LABELS));
        sanitized.put("annotations", filterMap(alert.annotations(), ALLOWED_ANNOTATIONS));
        return sanitized;
    }

    private Map<String, String> filterMap(Map<String, String> source, Set<String> allowed) {
        if (source == null) {
            return Map.of();
        }
        Map<String, String> filtered = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (allowed.contains(key)) {
                filtered.put(key, value);
            }
        });
        return filtered;
    }

    private List<Map<String, Object>> toToolPayload(List<ToolDefinition> allowedTools) {
        if (allowedTools == null || allowedTools.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> payload = new ArrayList<>();
        for (ToolDefinition tool : allowedTools) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", tool.name());
            item.put("description", tool.description());
            item.put("inputSchema", parseSchema(tool.inputSchema()));
            payload.add(item);
        }
        return payload;
    }

    private Object parseSchema(String inputSchema) {
        if (inputSchema == null || inputSchema.isBlank()) {
            return Map.of("type", "object", "properties", Map.of());
        }
        try {
            return objectMapper.readValue(inputSchema, Object.class);
        } catch (Exception ex) {
            return inputSchema;
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }
}
