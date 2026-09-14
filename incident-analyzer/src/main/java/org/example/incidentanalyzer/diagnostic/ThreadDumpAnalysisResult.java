package org.example.incidentanalyzer.diagnostic;

import org.example.incidentanalyzer.diagnostic.RunnableMethodCount;

import java.util.List;

/**
 * Thread Dump 분석 결과를 표현한다.
 */
public record ThreadDumpAnalysisResult(
        int totalThreads,
        long runnableThreads,
        long blockedThreads,
        long waitingThreads,
        long timedWaitingThreads,
        List<RunnableMethodCount> runnableMethods
) {
}