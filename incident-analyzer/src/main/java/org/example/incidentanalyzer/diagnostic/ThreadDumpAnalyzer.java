package org.example.incidentanalyzer.diagnostic;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ThreadDumpAnalyzer {

    private final RunnableThreadAnalyzer runnableThreadAnalyzer;

    public ThreadDumpAnalyzer(RunnableThreadAnalyzer runnableThreadAnalyzer) {
        this.runnableThreadAnalyzer = runnableThreadAnalyzer;
    }

    /**
     * Thread Dump에서 Thread 상태와
     * RUNNABLE 메서드 정보를 분석한다.
     */
    public ThreadDumpAnalysisResult analyze(List<ThreadInfo> threads) {

        Map<Thread.State, Long> stateCounts = threads.stream()
                .collect(Collectors.groupingBy(
                        ThreadInfo::state,
                        Collectors.counting()
                ));

        List<RunnableMethodCount> runnableMethods =
                runnableThreadAnalyzer.analyze(threads);

        return new ThreadDumpAnalysisResult(
                threads.size(),
                stateCounts.getOrDefault(
                        Thread.State.RUNNABLE, 0L
                ),
                stateCounts.getOrDefault(
                        Thread.State.BLOCKED, 0L
                ),
                stateCounts.getOrDefault(
                        Thread.State.WAITING, 0L
                ),
                stateCounts.getOrDefault(
                        Thread.State.TIMED_WAITING, 0L
                ),
                runnableMethods
        );
    }
}