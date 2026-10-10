package org.example.incidentanalyzer.diagnostic.tool;

import org.example.incidentanalyzer.alert.AlertRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AnalysisContext {

    private final String analysisId;
    private final AlertRequest.Alert alert;
    private final Instant incidentTime;
    private final Instant deadline;
    private final int maxToolCalls;
    private final AtomicInteger toolCallCount = new AtomicInteger();
    private final ConcurrentHashMap<String, DiagnosticEvidence> cache = new ConcurrentHashMap<>();
    private final List<DiagnosticEvidence> evidence = new ArrayList<>();
    private final AtomicInteger evidenceSequence = new AtomicInteger();

    public AnalysisContext(
            String analysisId,
            AlertRequest.Alert alert,
            Instant incidentTime,
            Instant deadline,
            int maxToolCalls
    ) {
        this.analysisId = analysisId;
        this.alert = alert;
        this.incidentTime = incidentTime;
        this.deadline = deadline;
        this.maxToolCalls = maxToolCalls;
    }

    public String analysisId() {
        return analysisId;
    }

    public AlertRequest.Alert alert() {
        return alert;
    }

    public Instant incidentTime() {
        return incidentTime;
    }

    public Instant deadline() {
        return deadline;
    }

    public String instance() {
        return alert.label("instance");
    }

    public boolean deadlineExceeded() {
        return Instant.now().isAfter(deadline);
    }

    public int incrementToolCallCount() {
        return toolCallCount.incrementAndGet();
    }

    public boolean exceedsMaxToolCalls() {
        return toolCallCount.get() > maxToolCalls;
    }

    public DiagnosticEvidence cached(String cacheKey) {
        return cache.get(cacheKey);
    }

    public void remember(String cacheKey, DiagnosticEvidence evidenceItem) {
        cache.put(cacheKey, evidenceItem);
    }

    public synchronized void addEvidence(DiagnosticEvidence evidenceItem) {
        evidence.add(evidenceItem);
    }

    public synchronized List<DiagnosticEvidence> evidence() {
        return List.copyOf(evidence);
    }

    public String nextEvidenceId(String toolName) {
        return "ev-%s-%d".formatted(toolName, evidenceSequence.incrementAndGet());
    }
}
