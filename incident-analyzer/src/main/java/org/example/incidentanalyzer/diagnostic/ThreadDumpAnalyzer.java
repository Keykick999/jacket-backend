package org.example.incidentanalyzer.diagnostic;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ThreadDumpAnalyzer {

    private static final int DEFAULT_TOP_N = 10;
    private static final int DEFAULT_MAX_STACK_DEPTH = 15;

    private final RunnableThreadAnalyzer runnableThreadAnalyzer;

    public ThreadDumpAnalyzer(RunnableThreadAnalyzer runnableThreadAnalyzer) {
        this.runnableThreadAnalyzer = runnableThreadAnalyzer;
    }

    /**
     * Thread Dump에서 Thread 상태와
     * RUNNABLE 메서드 정보를 분석한다.
     */
    public ThreadDumpAnalysisResult analyze(List<ThreadInfo> threads) {
        return analyze(threads, DEFAULT_TOP_N, DEFAULT_MAX_STACK_DEPTH);
    }

    public ThreadDumpAnalysisResult analyze(
            List<ThreadInfo> threads,
            int topN,
            int maxStackDepth
    ) {

        Map<Thread.State, Long> stateCounts = threads.stream()
                .filter(thread -> thread.state() != null)
                .collect(Collectors.groupingBy(
                        ThreadInfo::state,
                        Collectors.counting()
                ));

        List<RunnableMethodCount> runnableMethods =
                runnableThreadAnalyzer.analyze(threads);

        return new ThreadDumpAnalysisResult(
                threads.size(),
                stateCounts.getOrDefault(Thread.State.RUNNABLE, 0L),
                stateCounts.getOrDefault(Thread.State.BLOCKED, 0L),
                stateCounts.getOrDefault(Thread.State.WAITING, 0L),
                stateCounts.getOrDefault(Thread.State.TIMED_WAITING, 0L),
                runnableMethods,
                buildLockWaits(threads),
                buildTopStacks(threads, topN, maxStackDepth)
        );
    }

    private List<LockWait> buildLockWaits(List<ThreadInfo> threads) {
        Map<String, List<String>> ownersByLock = new LinkedHashMap<>();

        for (ThreadInfo thread : threads) {
            for (String lockId : thread.lockedMonitors()) {
                ownersByLock
                        .computeIfAbsent(lockId, key -> new ArrayList<>())
                        .add(thread.name());
            }
        }

        List<LockWait> lockWaits = new ArrayList<>();
        for (ThreadInfo thread : threads) {
            for (LockReference waiting : thread.waitingToLock()) {
                lockWaits.add(new LockWait(
                        thread.name(),
                        waiting.lockId(),
                        waiting.lockClass(),
                        List.copyOf(ownersByLock.getOrDefault(waiting.lockId(), List.of()))
                ));
            }
        }
        return List.copyOf(lockWaits);
    }

    private List<ThreadStackSummary> buildTopStacks(
            List<ThreadInfo> threads,
            int topN,
            int maxStackDepth
    ) {
        EnumSet<Thread.State> interesting =
                EnumSet.of(Thread.State.RUNNABLE, Thread.State.BLOCKED);

        return threads.stream()
                .filter(thread -> thread.state() != null && interesting.contains(thread.state()))
                .limit(Math.max(topN, 0))
                .map(thread -> new ThreadStackSummary(
                        thread.name(),
                        thread.state(),
                        thread.stackTrace().stream()
                                .limit(Math.max(maxStackDepth, 0))
                                .toList()
                ))
                .toList();
    }
}
