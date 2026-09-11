package org.example.incidentanalyzer.diagnostic;

import java.util.List;

public record ThreadInfo(
        String name,
        Thread.State state,
        List<String> stackTrace
) {}