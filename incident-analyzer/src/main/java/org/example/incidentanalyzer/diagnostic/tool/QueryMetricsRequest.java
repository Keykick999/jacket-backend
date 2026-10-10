package org.example.incidentanalyzer.diagnostic.tool;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record QueryMetricsRequest(
        @JsonPropertyDescription("Allowed Prometheus metric name")
        String metric,
        @JsonPropertyDescription("Minutes before the incident time. Maximum 30.")
        Integer minutesBefore,
        @JsonPropertyDescription("Minutes after the incident time. Maximum 30.")
        Integer minutesAfter,
        @JsonPropertyDescription("Query step in seconds. Minimum 15.")
        Integer stepSeconds
) {
}
