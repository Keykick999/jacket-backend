package org.example.incidentanalyzer.diagnostic;

public record RunnableMethodCount(
        String methodName,
        long count
) {
}