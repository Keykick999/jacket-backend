package org.example.incidentanalyzer.diagnostic;

import java.time.Duration;

/**
 * ExecutionSample과 가장 가까운 CPU 사용량을 표현한다.
 */
public record MatchedThreadCpu(
        ExecutionSampleInfo executionSample,
        ThreadCpuLoad cpuLoad,
        Duration timeDifference
) {
}