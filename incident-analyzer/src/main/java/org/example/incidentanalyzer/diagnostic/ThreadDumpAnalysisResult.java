package org.example.incidentanalyzer.diagnostic;

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
        List<RunnableMethodCount> runnableMethods,
        List<LockWait> lockWaits,
        List<ThreadStackSummary> topStacks
) {
    public ThreadDumpAnalysisResult(
            int totalThreads,
            long runnableThreads,
            long blockedThreads,
            long waitingThreads,
            long timedWaitingThreads,
            List<RunnableMethodCount> runnableMethods
    ) {
        this(
                totalThreads,
                runnableThreads,
                blockedThreads,
                waitingThreads,
                timedWaitingThreads,
                runnableMethods,
                List.of(),
                List.of()
        );
    }
}
