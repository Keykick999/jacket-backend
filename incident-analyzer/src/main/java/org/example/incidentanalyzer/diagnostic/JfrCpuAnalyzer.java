package org.example.incidentanalyzer.diagnostic;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class JfrCpuAnalyzer {

    private static final String PROJECT_PACKAGE =
            "com.example.ThirdProject";

    private static final Pattern STACK_FRAME_PATTERN =
            Pattern.compile(
                    "^at\\s+(?:app//)?([\\w.$]+)\\.([\\w$<>]+)\\(([^)]*)\\)"
            );

    public List<MethodCpuAnalysis> analyze(
            String filePath
    ) throws IOException {

        List<RecordedEvent> events =
                RecordingFile.readAllEvents(Path.of(filePath));

        List<ThreadCpuLoad> threadCpuLoads =
                events.stream()
                        .filter(this::isThreadCpuLoad)
                        .map(this::toThreadCpuLoad)
                        .toList();

        List<ExecutionSampleInfo> executionSamples =
                events.stream()
                        .filter(this::isExecutionSample)
                        .map(this::toExecutionSampleInfo)
                        .filter(Objects::nonNull)
                        .toList();

        Map<String, List<ThreadCpuLoad>> cpuLoadsByThread =
                groupCpuLoadsByThread(threadCpuLoads);

        return analyzeMethods(
                cpuLoadsByThread,
                executionSamples
        );
    }

    private ExecutionSampleInfo toExecutionSampleInfo(
            RecordedEvent event
    ) {

        if (event.getThread() == null || event.getStackTrace() == null) {
            return null;
        }

        String threadName = event.getThread().getJavaName();

        String stackTrace =
                event.getStackTrace()
                        .getFrames()
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.joining("\n"));

        return new ExecutionSampleInfo(
                threadName,
                stackTrace,
                event.getStartTime()
        );
    }

    private boolean isThreadCpuLoad(
            RecordedEvent event
    ) {
        return event.getEventType()
                .getName()
                .equals("jdk.ThreadCPULoad");
    }

    private boolean isExecutionSample(
            RecordedEvent event
    ) {
        return event.getEventType()
                .getName()
                .equals("jdk.ExecutionSample");
    }

    private ThreadCpuLoad toThreadCpuLoad(
            RecordedEvent event
    ) {
        String threadName =
                event.getThread().getJavaName();

        float userLoad =
                event.getFloat("user");

        float systemLoad =
                event.getFloat("system");

        return new ThreadCpuLoad(
                threadName,
                userLoad,
                systemLoad,
                event.getStartTime()
        );
    }

    private List<MethodCpuAnalysis> analyzeMethods(
            Map<String, List<ThreadCpuLoad>> cpuLoadsByThread,
            List<ExecutionSampleInfo> executionSamples
    ) {
        Map<String, List<Float>> cpuLoadsByMethod =
                executionSamples.stream()
                        .map(sample -> {

                            ThreadCpuLoad cpuLoad =
                                    findNearestCpuLoad(
                                            sample,
                                            cpuLoadsByThread
                                    );

                            if (cpuLoad == null) {
                                return null;
                            }

                            String methodName = extractApplicationMethod(
                                            sample.stackTrace()
                                    );

                            if (methodName == null) {
                                return null;
                            }

                            return Map.entry(
                                    methodName,
                                    cpuLoad.totalLoad()
                            );
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(
                                Map.Entry::getKey,
                                Collectors.mapping(
                                        Map.Entry::getValue,
                                        Collectors.toList()
                                )
                        ));

        return cpuLoadsByMethod.entrySet()
                .stream()
                .map(entry -> {

                    List<Float> cpuLoads =
                            entry.getValue();

                    float averageCpuLoad =
                            (float) cpuLoads.stream()
                                    .mapToDouble(Float::doubleValue)
                                    .average()
                                    .orElse(0.0);

                    float maxCpuLoad =
                            cpuLoads.stream()
                                    .max(Float::compare)
                                    .orElse(0.0f);

                    return new MethodCpuAnalysis(
                            entry.getKey(),
                            averageCpuLoad,
                            maxCpuLoad,
                            cpuLoads.size()
                    );
                })
                .toList();
    }

    private String extractApplicationMethod(
            String stackTrace
    ) {
        for (String stackFrame : stackTrace.split("\n")) {

            if (!stackFrame.contains(PROJECT_PACKAGE)) {
                continue;
            }

            Matcher matcher =
                    STACK_FRAME_PATTERN.matcher(stackFrame);

            if (!matcher.matches()) {
                continue;
            }

            String classPath = matcher.group(1);
            String methodName = matcher.group(2);

            int lastDot = classPath.lastIndexOf('.');

            if (lastDot == -1) {
                return null;
            }

            String className = classPath.substring(lastDot + 1);

            return className + "." + methodName + "()";
        }

        return null;
    }

    private Map<String, List<ThreadCpuLoad>> groupCpuLoadsByThread(
            List<ThreadCpuLoad> threadCpuLoads
    ) {
        return threadCpuLoads.stream()
                .collect(Collectors.groupingBy(
                        ThreadCpuLoad::threadName
                ));
    }

    private ThreadCpuLoad findNearestCpuLoad(
            ExecutionSampleInfo sample,
            List<ThreadCpuLoad> cpuLoads
    ) {
        return cpuLoads.stream()
                .min(
                        Comparator.comparing(
                                cpuLoad -> Math.abs(
                                        Duration.between(
                                                sample.timestamp(),
                                                cpuLoad.timestamp()
                                        ).toNanos()
                                )
                        )
                )
                .orElse(null);
    }

    private ThreadCpuLoad findNearestCpuLoad(
            ExecutionSampleInfo sample,
            Map<String, List<ThreadCpuLoad>> cpuLoadsByThread
    ) {
        List<ThreadCpuLoad> cpuLoads =
                cpuLoadsByThread.get(sample.threadName());

        if (cpuLoads == null) {
            return null;
        }

        return findNearestCpuLoad(sample, cpuLoads);
    }
}