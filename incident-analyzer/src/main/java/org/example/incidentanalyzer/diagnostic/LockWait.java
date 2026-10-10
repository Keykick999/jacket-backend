package org.example.incidentanalyzer.diagnostic;

import java.util.List;

public record LockWait(
        String waiterThread,
        String lockId,
        String lockClass,
        List<String> ownerThreads
) {
}
