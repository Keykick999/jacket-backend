package org.example.incidentanalyzer.diagnostic;

import java.time.Instant;

/**
 * ExecutionSample에서 추출한 실행 정보
 */
public record ExecutionSampleInfo(
        String threadName,
        String stackTrace,
        Instant timestamp
) {
}