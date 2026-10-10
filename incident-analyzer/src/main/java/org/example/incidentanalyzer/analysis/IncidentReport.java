package org.example.incidentanalyzer.analysis;

import org.example.incidentanalyzer.llm.LlmIncidentReport;

public record IncidentReport(
        AlertAnalysisResult ruleResult,
        LlmIncidentReport llmReport,
        LlmStatus llmStatus,
        String llmFailureReason
) {
}
