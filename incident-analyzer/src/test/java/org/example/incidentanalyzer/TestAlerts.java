package org.example.incidentanalyzer;

import org.example.incidentanalyzer.alert.AlertRequest;

import java.time.Instant;
import java.util.Map;

public final class TestAlerts {

    private TestAlerts() {
    }

    public static AlertRequest.Alert highCpu(String fingerprint) {
        return new AlertRequest.Alert(
                "firing",
                Map.of(
                        "alertname", "JacketHighCPU",
                        "severity", "warning",
                        "instance", "host.docker.internal:8080",
                        "service", "jacket"
                ),
                Map.of(
                        "summary", "Jacket CPU usage is high",
                        "description", "CPU usage has been above 80% for 1 minute."
                ),
                Instant.parse("2026-10-10T01:00:00Z"),
                null,
                fingerprint
        );
    }
}
