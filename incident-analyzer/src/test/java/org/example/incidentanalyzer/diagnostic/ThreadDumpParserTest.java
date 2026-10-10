package org.example.incidentanalyzer.diagnostic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadDumpParserTest {

    private final ThreadDumpParser parser = new ThreadDumpParser();

    @Test
    void parsesStatesStacksAndLocks() {
        String dump = """
                "http-nio-8080-exec-1" #27 prio=5
                   java.lang.Thread.State: BLOCKED (on object monitor)
                    at com.example.ThirdProject.Foo.waitForLock(Foo.java:10)
                    - waiting to lock <0x000000076ab0c0e8> (a java.lang.Object)
                    - locked <0x000000076ab0c0f0> (a java.lang.String)
                
                "http-nio-8080-exec-2" #28 prio=5
                   java.lang.Thread.State: RUNNABLE
                    at com.example.ThirdProject.Foo.holdLock(Foo.java:20)
                    - locked <0x000000076ab0c0e8> (a java.lang.Object)
                
                "async-1" #29 prio=5
                   java.lang.Thread.State: WAITING (parking)
                    at jdk.internal.misc.Unsafe.park(Native Method)
                    - parking to wait for  <0x000000076ab0aaaa> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)
                """;

        List<ThreadInfo> threads = parser.parse(dump);

        assertThat(threads).hasSize(3);
        ThreadInfo blocked = threads.get(0);
        assertThat(blocked.name()).isEqualTo("http-nio-8080-exec-1");
        assertThat(blocked.state()).isEqualTo(Thread.State.BLOCKED);
        assertThat(blocked.stackTrace()).isNotEmpty();
        assertThat(blocked.waitingToLock()).extracting(LockReference::lockId)
                .containsExactly("0x000000076ab0c0e8");
        assertThat(blocked.lockedMonitors()).containsExactly("0x000000076ab0c0f0");

        ThreadInfo runnable = threads.get(1);
        assertThat(runnable.state()).isEqualTo(Thread.State.RUNNABLE);
        assertThat(runnable.lockedMonitors()).containsExactly("0x000000076ab0c0e8");

        ThreadInfo waiting = threads.get(2);
        assertThat(waiting.waitingToLock()).extracting(LockReference::lockId)
                .containsExactly("0x000000076ab0aaaa");
    }
}
