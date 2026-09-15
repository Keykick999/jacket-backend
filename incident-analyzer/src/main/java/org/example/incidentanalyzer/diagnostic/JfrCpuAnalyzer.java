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

    private static class MethodCpuData {

        private float totalAverageCpuLoad;
        private float maxCpuLoad;
        private long sampleCount;

        public void add(
                float averageCpuLoad,
                float maxCpuLoad,
                long sampleCount
        ) {
            this.totalAverageCpuLoad += averageCpuLoad;
            this.maxCpuLoad =
                    Math.max(this.maxCpuLoad, maxCpuLoad);
            this.sampleCount += sampleCount;
        }

        public float averageCpuLoad() {
            if (sampleCount == 0) {
                return 0.0f;
            }

            return totalAverageCpuLoad / sampleCount;
        }

        public float maxCpuLoad() {
            return maxCpuLoad;
        }

        public long sampleCount() {
            return sampleCount;
        }
    }

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

        long executionSampleEventCount =
                events.stream()
                        .filter(this::isExecutionSample)
                        .count();

        System.out.println(
                "ExecutionSample EVENT count = "
                        + executionSampleEventCount
        );

        events.stream()
                .filter(this::isExecutionSample)
                .limit(5)
                .forEach(event -> {

                    System.out.println("===== RAW EXECUTION SAMPLE =====");
                    System.out.println("event type = "
                            + event.getEventType().getName());

                    System.out.println("thread = "
                            + event.getThread());

                    System.out.println("sampledThread = "
                            + event.getThread("sampledThread"));

                    System.out.println("stackTrace = "
                            + event.getStackTrace());

                    System.out.println("timestamp = "
                            + event.getStartTime());
                });

        List<ThreadCpuLoad> threadCpuLoads =
                events.stream()
                        .filter(this::isThreadCpuLoad)
                        .map(this::toThreadCpuLoad)
                        .filter(Objects::nonNull)
                        .toList();

        List<ExecutionSampleInfo> executionSamples =
                events.stream()
                        .filter(this::isExecutionSample)
                        .map(this::toExecutionSampleInfo)
                        .filter(Objects::nonNull)
                        .toList();

        Map<String, List<ThreadCpuLoad>> cpuLoadsByThread =
                groupCpuLoadsByThread(threadCpuLoads);

        Map<String, List<ExecutionSampleInfo>> samplesByThread =
                groupExecutionSamplesByThread(executionSamples);

        Map<String, List<Float>> cpuLoadsByMethod =
                new HashMap<>();

        for (Map.Entry<String, List<ExecutionSampleInfo>> entry
                : samplesByThread.entrySet()) {
            String threadName = entry.getKey();

            List<ExecutionSampleInfo> samples =
                    entry.getValue();

            List<ThreadCpuLoad> cpuLoads =
                    cpuLoadsByThread.get(threadName);

            if (cpuLoads == null || cpuLoads.isEmpty()) {
                continue;
            }

            for (ExecutionSampleInfo sample : samples) {

                String methodName =
                        extractApplicationMethod(
                                sample.stackTrace()
                        );

                if (methodName == null) {
                    continue;
                }

                ThreadCpuLoad cpuLoad =
                        findNearestCpuLoad(
                                sample,
                                cpuLoads
                        );

                System.out.println(
                        "===== SAMPLE MATCH ====="
                );
                System.out.println(
                        "thread = " + sample.threadName()
                );
                System.out.println(
                        "sample timestamp = " + sample.timestamp()
                );
                System.out.println(
                        "method = " + methodName
                );
                System.out.println(
                        "nearest cpuLoad = " + cpuLoad
                );

                if (cpuLoad == null) {
                    continue;
                }

                cpuLoadsByMethod
                        .computeIfAbsent(
                                methodName,
                                key -> new ArrayList<>()
                        )
                        .add(cpuLoad.totalLoad());
            }
        }

        System.out.println("===== THREAD CPU LOADS =====");
        System.out.println("size = " + threadCpuLoads.size());

        threadCpuLoads.stream()
                .limit(10)
                .forEach(System.out::println);

        System.out.println("===== THREAD CPU LOADS =====");
        System.out.println("size = " + threadCpuLoads.size());

        threadCpuLoads.stream()
                .limit(10)
                .forEach(System.out::println);


        System.out.println("===== EXECUTION SAMPLES =====");
        System.out.println("size = " + executionSamples.size());

        executionSamples.stream()
                .limit(10)
                .forEach(System.out::println);


        System.out.println("===== CPU LOADS BY METHOD =====");
        System.out.println("size = " + cpuLoadsByMethod.size());

        cpuLoadsByMethod.forEach((method, cpuLoads) ->
                System.out.println(
                        "method=" + method +
                                ", cpuLoads=" + cpuLoads
                )
        );

        return cpuLoadsByMethod.entrySet()
                .stream()
                .map(entry -> {

                    String methodName =
                            entry.getKey();

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

                    long sampleCount =
                            cpuLoads.size();

                    return new MethodCpuAnalysis(
                            methodName,
                            averageCpuLoad,
                            maxCpuLoad,
                            sampleCount
                    );
                })
                .sorted(
                        Comparator.comparing(
                                MethodCpuAnalysis::impactScore
                        ).reversed()
                )
                .toList();
    }

    private ExecutionSampleInfo toExecutionSampleInfo(
            RecordedEvent event
    ) {
        if (event.getStackTrace() == null) {
            return null;
        }

        var sampledThread =
                event.getThread("sampledThread");

        if (sampledThread == null) {
            return null;
        }

        String threadName =
                sampledThread.getJavaName();

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
        if (event.getThread() == null) {
            return null;
        }

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
                .sorted(
                        Comparator.comparing(
                                MethodCpuAnalysis::impactScore
                        ).reversed()
                )
                .toList();
    }

    private String extractApplicationMethod(
            String stackTrace
    ) {
        for (String stackFrame : stackTrace.split("\n")) {

            if (!stackFrame.contains("method = " + PROJECT_PACKAGE)) {
                continue;
            }

            String method = stackFrame
                    .substring(
                            stackFrame.indexOf("method = ")
                                    + "method = ".length()
                    )
                    .trim();

            int openParenthesis = method.indexOf('(');

            if (openParenthesis == -1) {
                continue;
            }

            String fullMethodName =
                    method.substring(0, openParenthesis);

            int lastDot =
                    fullMethodName.lastIndexOf('.');

            if (lastDot == -1) {
                continue;
            }

            String classPath =
                    fullMethodName.substring(0, lastDot);

            String methodName =
                    fullMethodName.substring(lastDot + 1);

            String className =
                    classPath.substring(
                            classPath.lastIndexOf('.') + 1
                    );

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

    private Map<String, List<ExecutionSampleInfo>> groupExecutionSamplesByThread(
            List<ExecutionSampleInfo> samples
    ) {
        return samples.stream()
                .collect(Collectors.groupingBy(
                        ExecutionSampleInfo::threadName
                ));
    }
}