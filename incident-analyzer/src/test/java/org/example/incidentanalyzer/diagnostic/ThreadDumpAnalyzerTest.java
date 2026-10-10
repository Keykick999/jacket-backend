package org.example.incidentanalyzer.diagnostic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadDumpAnalyzerTest {

    @Test
    void summarizesStatesLockWaitsAndTopStacks() {
        ThreadDumpAnalyzer analyzer = new ThreadDumpAnalyzer(new RunnableThreadAnalyzer());

        List<ThreadInfo> threads = List.of(
                new ThreadInfo(
                        "waiter",
                        Thread.State.BLOCKED,
                        List.of("at com.example.ThirdProject.Foo.bar(Foo.java:1)"),
                        List.of(),
                        List.of(new LockReference("0x1", "java.lang.Object"))
                ),
                new ThreadInfo(
                        "owner",
                        Thread.State.RUNNABLE,
                        List.of("at com.example.ThirdProject.Foo.hold(Foo.java:2)"),
                        List.of("0x1"),
                        List.of()
                ),
                new ThreadInfo(
                        "idle",
                        Thread.State.WAITING,
                        List.of(),
                        List.of(),
                        List.of()
                )
        );

        ThreadDumpAnalysisResult result = analyzer.analyze(threads, 5, 10);

        assertThat(result.totalThreads()).isEqualTo(3);
        assertThat(result.runnableThreads()).isEqualTo(1);
        assertThat(result.blockedThreads()).isEqualTo(1);
        assertThat(result.waitingThreads()).isEqualTo(1);
        assertThat(result.lockWaits()).hasSize(1);
        assertThat(result.lockWaits().get(0).waiterThread()).isEqualTo("waiter");
        assertThat(result.lockWaits().get(0).ownerThreads()).containsExactly("owner");
        assertThat(result.topStacks()).extracting(ThreadStackSummary::threadName)
                .contains("waiter", "owner");
    }
}
