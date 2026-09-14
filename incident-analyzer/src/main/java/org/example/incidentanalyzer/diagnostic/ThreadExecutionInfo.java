package org.example.incidentanalyzer.diagnostic;

/**
 * Thread의 CPU 사용량과 Stack Trace를 표현한다.
 */
public record ThreadExecutionInfo(
        String threadName,
        float cpuLoad,
        String stackTrace
) {
}