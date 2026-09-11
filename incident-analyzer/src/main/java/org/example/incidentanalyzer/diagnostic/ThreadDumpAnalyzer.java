package org.example.incidentanalyzer.diagnostic;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ThreadDumpAnalyzer {

    private final RunnableThreadAnalyzer runnableThreadAnalyzer;

    public ThreadDumpAnalyzer(RunnableThreadAnalyzer runnableThreadAnalyzer) {
        this.runnableThreadAnalyzer = runnableThreadAnalyzer;
    }

    public String analyze(List<ThreadInfo> threads) {
        Map<Thread.State, Long> stateCounts = threads.stream()
                .collect(Collectors.groupingBy(
                        ThreadInfo::state,
                        Collectors.counting()
                ));

        List<RunnableMethodCount> runnableMethods =
                runnableThreadAnalyzer.analyze(threads);

        String runnableMethodAnalysis = runnableMethods.stream()
                .sorted(Comparator.comparingLong(
                        RunnableMethodCount::count
                ).reversed())
                .map(method ->
                        "- %s : %d threads"
                                .formatted(
                                        method.methodName(),
                                        method.count()
                                )
                )
                .collect(Collectors.joining("\n"));

        return """
            Thread Dump Analysis

            Total Threads: %d
            RUNNABLE: %d
            BLOCKED: %d
            WAITING: %d
            TIMED_WAITING: %d

            RUNNABLE 메서드
            %s
            """.formatted(
                threads.size(),
                stateCounts.getOrDefault(Thread.State.RUNNABLE, 0L),
                stateCounts.getOrDefault(Thread.State.BLOCKED, 0L),
                stateCounts.getOrDefault(Thread.State.WAITING, 0L),
                stateCounts.getOrDefault(Thread.State.TIMED_WAITING, 0L),
                runnableMethodAnalysis
        );
    }
}