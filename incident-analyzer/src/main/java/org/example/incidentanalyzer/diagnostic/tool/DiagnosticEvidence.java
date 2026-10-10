package org.example.incidentanalyzer.diagnostic.tool;

import java.time.Instant;

public record DiagnosticEvidence(
        String evidenceId,
        String toolName,
        String source,
        String instance,
        Instant collectedAt,
        EvidenceStatus status,
        String reason,
        Object data
) {
    public static DiagnosticEvidence rejected(
            String evidenceId,
            String toolName,
            String instance,
            String reason
    ) {
        return new DiagnosticEvidence(
                evidenceId,
                toolName,
                "incident-analyzer",
                instance,
                Instant.now(),
                EvidenceStatus.REJECTED,
                reason,
                null
        );
    }

    public static DiagnosticEvidence failed(
            String evidenceId,
            String toolName,
            String source,
            String instance,
            String reason
    ) {
        return new DiagnosticEvidence(
                evidenceId,
                toolName,
                source,
                instance,
                Instant.now(),
                EvidenceStatus.FAILED,
                reason,
                null
        );
    }
}
