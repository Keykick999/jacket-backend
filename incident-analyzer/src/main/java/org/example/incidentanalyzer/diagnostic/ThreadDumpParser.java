package org.example.incidentanalyzer.diagnostic;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ThreadDumpParser {

    private static final Pattern THREAD_PATTERN =
            Pattern.compile("^\"(.+?)\".*$");

    private static final Pattern STATE_PATTERN =
            Pattern.compile("^\\s*java\\.lang\\.Thread\\.State:\\s+(\\S+)");

    private static final Pattern LOCKED_PATTERN =
            Pattern.compile("^\\s*- locked <([^>]+)>\\s*(?:\\(a\\s+([^)]+)\\))?");

    private static final Pattern WAITING_TO_LOCK_PATTERN =
            Pattern.compile("^\\s*- waiting to lock <([^>]+)>\\s*(?:\\(a\\s+([^)]+)\\))?");

    private static final Pattern PARKING_PATTERN =
            Pattern.compile("^\\s*- parking to wait for\\s+<([^>]+)>\\s*(?:\\(a\\s+([^)]+)\\))?");

    public List<ThreadInfo> parse(String threadDump) {

        List<ThreadInfo> threads = new ArrayList<>();

        if (threadDump == null || threadDump.isBlank()) {
            return threads;
        }

        String[] lines = threadDump.split("\\R");

        String threadName = null;
        Thread.State state = null;
        List<String> stackTrace = new ArrayList<>();
        List<String> lockedMonitors = new ArrayList<>();
        List<LockReference> waitingToLock = new ArrayList<>();

        for (String line : lines) {

            Matcher threadMatcher = THREAD_PATTERN.matcher(line);

            if (threadMatcher.matches()) {

                if (threadName != null) {
                    threads.add(toThreadInfo(
                            threadName,
                            state,
                            stackTrace,
                            lockedMonitors,
                            waitingToLock
                    ));
                }

                threadName = threadMatcher.group(1);
                state = null;
                stackTrace = new ArrayList<>();
                lockedMonitors = new ArrayList<>();
                waitingToLock = new ArrayList<>();

                continue;
            }

            Matcher stateMatcher = STATE_PATTERN.matcher(line);

            if (stateMatcher.find()) {
                state = parseState(stateMatcher.group(1));
                continue;
            }

            Matcher lockedMatcher = LOCKED_PATTERN.matcher(line);
            if (lockedMatcher.find()) {
                lockedMonitors.add(lockedMatcher.group(1));
                continue;
            }

            Matcher waitingMatcher = WAITING_TO_LOCK_PATTERN.matcher(line);
            if (waitingMatcher.find()) {
                waitingToLock.add(new LockReference(
                        waitingMatcher.group(1),
                        waitingMatcher.group(2)
                ));
                continue;
            }

            Matcher parkingMatcher = PARKING_PATTERN.matcher(line);
            if (parkingMatcher.find()) {
                waitingToLock.add(new LockReference(
                        parkingMatcher.group(1),
                        parkingMatcher.group(2)
                ));
                continue;
            }

            if (threadName != null && line.trim().startsWith("at ")) {
                stackTrace.add(line.trim());
            }
        }

        if (threadName != null) {
            threads.add(toThreadInfo(
                    threadName,
                    state,
                    stackTrace,
                    lockedMonitors,
                    waitingToLock
            ));
        }

        return threads;
    }

    private ThreadInfo toThreadInfo(
            String threadName,
            Thread.State state,
            List<String> stackTrace,
            List<String> lockedMonitors,
            List<LockReference> waitingToLock
    ) {
        return new ThreadInfo(
                threadName,
                state,
                List.copyOf(stackTrace),
                List.copyOf(lockedMonitors),
                List.copyOf(waitingToLock)
        );
    }

    private Thread.State parseState(String raw) {
        try {
            return Thread.State.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
