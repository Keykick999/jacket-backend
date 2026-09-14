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

    public List<ThreadInfo> parse(String threadDump) {

        List<ThreadInfo> threads = new ArrayList<>();

        String[] lines = threadDump.split("\\R");

        String threadName = null;
        Thread.State state = null;
        List<String> stackTrace = new ArrayList<>();

        for (String line : lines) {

            Matcher threadMatcher = THREAD_PATTERN.matcher(line);

            if (threadMatcher.matches()) {

                if (threadName != null) {
                    threads.add(
                            new ThreadInfo(
                                    threadName,
                                    state,
                                    List.copyOf(stackTrace)
                            )
                    );
                }

                threadName = threadMatcher.group(1);
                state = null;
                stackTrace = new ArrayList<>();

                continue;
            }

            Matcher stateMatcher = STATE_PATTERN.matcher(line);

            if (stateMatcher.find()) {
                state = Thread.State.valueOf(stateMatcher.group(1));
                continue;
            }

            if (threadName != null && line.trim().startsWith("at ")) {
                stackTrace.add(line.trim());
            }
        }

        if (threadName != null) {
            threads.add(
                    new ThreadInfo(
                            threadName,
                            state,
                            List.copyOf(stackTrace)
                    )
            );
        }

        return threads;
    }
}