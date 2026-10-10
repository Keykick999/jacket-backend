package org.example.incidentanalyzer.llm;

public record RequestedToolCall(
        String name,
        String argumentsJson
) {
}
