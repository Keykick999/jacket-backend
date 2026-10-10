package org.example.incidentanalyzer.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LlmReportParser {

    private static final Pattern FENCED_JSON =
            Pattern.compile("```(?:json)?\\s*(\\{.*})\\s*```", Pattern.DOTALL);

    private final ObjectMapper objectMapper;

    public LlmReportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<RequestedToolCall> parseToolCalls(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(unwrap(raw));
            JsonNode toolCalls = root.get("toolCalls");
            if (toolCalls == null || !toolCalls.isArray() || toolCalls.isEmpty()) {
                return List.of();
            }
            List<RequestedToolCall> parsed = new ArrayList<>();
            for (JsonNode call : toolCalls) {
                if (call == null || call.isNull()) {
                    continue;
                }
                String name = textOrNull(call.get("name"));
                if (name == null || name.isBlank()) {
                    continue;
                }
                parsed.add(new RequestedToolCall(name, argumentsJson(call.get("arguments"))));
            }
            return List.copyOf(parsed);
        } catch (Exception ex) {
            return List.of();
        }
    }

    public LlmIncidentReport parse(String raw, Set<String> knownEvidenceIds) {
        if (raw == null || raw.isBlank()) {
            throw new LlmAnalysisException("LLM 응답이 비어 있습니다.");
        }

        try {
            LlmIncidentReport report = objectMapper.readValue(unwrap(raw), LlmIncidentReport.class);
            validateRequired(report);
            return sanitizeEvidenceIds(report, knownEvidenceIds);
        } catch (LlmAnalysisException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LlmAnalysisException("LLM 응답을 구조화된 보고서로 변환하지 못했습니다.", ex);
        }
    }

    String unwrap(String raw) {
        String trimmed = raw.trim();
        Matcher matcher = FENCED_JSON.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private void validateRequired(LlmIncidentReport report) {
        if (report.summary() == null || report.summary().isBlank()) {
            throw new LlmAnalysisException("LLM 응답에 summary가 없습니다.");
        }
        if (report.causeCandidates() == null) {
            throw new LlmAnalysisException("LLM 응답에 causeCandidates가 없습니다.");
        }
    }

    private LlmIncidentReport sanitizeEvidenceIds(
            LlmIncidentReport report,
            Set<String> knownEvidenceIds
    ) {
        List<CauseCandidate> sanitized = new ArrayList<>();
        for (CauseCandidate candidate : report.causeCandidates()) {
            if (candidate == null) {
                continue;
            }
            List<String> evidenceIds = candidate.evidenceIds() == null
                    ? List.of()
                    : candidate.evidenceIds();
            List<String> unknown = evidenceIds.stream()
                    .filter(id -> knownEvidenceIds == null || !knownEvidenceIds.contains(id))
                    .toList();
            String reasoning = candidate.reasoning() == null ? "" : candidate.reasoning();
            if (!unknown.isEmpty()) {
                reasoning = reasoning + " (근거 ID 미확인: " + String.join(", ", unknown) + ")";
            }
            sanitized.add(new CauseCandidate(
                    candidate.title(),
                    candidate.confidence() == null ? Confidence.LOW : candidate.confidence(),
                    reasoning,
                    evidenceIds,
                    candidate.isInference()
            ));
        }

        return new LlmIncidentReport(
                report.summary(),
                List.copyOf(sanitized),
                nullToEmpty(report.uncertainties()),
                nullToEmpty(report.followUpChecks()),
                nullToEmpty(report.recommendedActions()),
                report.ruleComparison()
        );
    }

    private List<String> nullToEmpty(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private String argumentsJson(JsonNode arguments) {
        if (arguments == null || arguments.isNull()) {
            return "{}";
        }
        if (arguments.isTextual()) {
            String text = arguments.asText();
            return text == null || text.isBlank() ? "{}" : text;
        }
        try {
            return objectMapper.writeValueAsString(arguments);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || !node.isTextual()) {
            return null;
        }
        return node.asText();
    }
}
