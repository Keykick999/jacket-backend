package org.example.incidentanalyzer.diagnostic.tool;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record CollectThreadDumpRequest(
        @JsonPropertyDescription("Maximum stack frames per thread. Maximum 20.")
        Integer maxStackDepth,
        @JsonPropertyDescription("Number of top thread stacks to return.")
        Integer topN
) {
}
