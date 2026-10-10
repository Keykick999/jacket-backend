package org.example.incidentanalyzer.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CauseCandidate(
        String title,
        Confidence confidence,
        String reasoning,
        List<String> evidenceIds,
        boolean isInference
) {
}
