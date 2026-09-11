package org.example.incidentanalyzer.diagnostic;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class RunnableThreadAnalyzer {

    private static final String PROJECT_PACKAGE =
            "com.example.ThirdProject";

    private static final Pattern STACK_FRAME_PATTERN =
            Pattern.compile(
                    "^at\\s+([\\w.$]+)\\.([\\w$<>]+)\\(([^)]*)\\)"
            );

    /**
     * RUNNABLE 상태의 Thread에서
     * 애플리케이션 메서드별 실행 중인 Thread 수를 집계한다.
     */
    public List<RunnableMethodCount> analyze(List<ThreadInfo> threads) {
        Map<String, Long> methodCounts = countRunnableMethods(threads);

        return methodCounts.entrySet().stream()
                .map(entry -> new RunnableMethodCount(
                        entry.getKey(),
                        entry.getValue()
                ))
                .toList();
    }

    /**
     * RUNNABLE Thread만 추려서
     * 각 Thread에서 실행 중인 애플리케이션 메서드를 찾고 개수를 집계한다.
     */
    private Map<String, Long> countRunnableMethods(
            List<ThreadInfo> threads
    ) {
        return threads.stream()
                .filter(thread -> thread.state() == Thread.State.RUNNABLE)
                .map(this::findStackFrameMatchingProjectPackage)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        stackFrame -> stackFrame,
                        Collectors.counting()
                ));
    }

    /**
     * Thread의 Stack Trace에서
     * 프로젝트 패키지에 해당하는 Stack Frame을 찾아
     * 클래스명과 메서드명을 반환한다.
     */
    private String findStackFrameMatchingProjectPackage(ThreadInfo thread) {

        for (String stackFrame : thread.stackTrace()) {

            if (!stackFrame.contains(PROJECT_PACKAGE)) {
                continue;
            }

            String simplified = extractClassAndMethodName(stackFrame);

            if (simplified != null) {
                return simplified;
            }
        }

        return null;
    }

    /**
     * Stack Frame에서 패키지명을 제거하고
     * 클래스명과 메서드명을 추출하여 "ClassName.methodName()" 형태로 반환한다.
     */
    private String extractClassAndMethodName(String frame) {

        Matcher matcher = STACK_FRAME_PATTERN.matcher(frame);

        if (!matcher.matches()) {
            return null;
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
}