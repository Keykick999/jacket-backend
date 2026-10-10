package org.example.incidentanalyzer.diagnostic;

import java.util.List;

public record ThreadStackSummary(
        String threadName,
        Thread.State state,
        List<String> stackTrace
) {
}
