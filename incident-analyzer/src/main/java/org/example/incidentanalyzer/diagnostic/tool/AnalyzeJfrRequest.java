package org.example.incidentanalyzer.diagnostic.tool;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record AnalyzeJfrRequest(
        @JsonPropertyDescription("Number of top CPU methods to return. Range 1-10.")
        Integer topN
) {
}
