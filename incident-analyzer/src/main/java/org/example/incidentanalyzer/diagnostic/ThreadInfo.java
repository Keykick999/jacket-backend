package org.example.incidentanalyzer.diagnostic;

import java.util.List;

public record ThreadInfo(
        String name,
        Thread.State state,
        List<String> stackTrace,
        List<String> lockedMonitors,
        List<LockReference> waitingToLock
) {
    public ThreadInfo(String name, Thread.State state, List<String> stackTrace) {
        this(name, state, stackTrace, List.of(), List.of());
    }
}
