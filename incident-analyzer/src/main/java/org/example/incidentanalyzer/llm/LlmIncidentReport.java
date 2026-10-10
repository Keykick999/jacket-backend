package org.example.incidentanalyzer.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmIncidentReport(
        String summary,
        List<CauseCandidate> causeCandidates,
        List<String> uncertainties,
        List<String> followUpChecks,
        List<String> recommendedActions,
        String ruleComparison
) {
}
