package org.example.incidentanalyzer.rule;

import org.example.incidentanalyzer.alert.AlertRequest;
import org.example.incidentanalyzer.alert.AlertType;
import org.example.incidentanalyzer.analysis.AlertAnalysisResult;
import org.example.incidentanalyzer.diagnostic.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HighCpuAnalyzer implements AlertAnalyzer {

    private static final int MAX_CANDIDATES = 5;

    private final JfrCpuAnalyzer jfrCpuAnalyzer;

    public HighCpuAnalyzer(JfrCpuAnalyzer jfrCpuAnalyzer) {
        this.jfrCpuAnalyzer = jfrCpuAnalyzer;
    }

    @Override
    public boolean supports(AlertType alertType) {
        return alertType == AlertType.HIGH_CPU;
    }

    @Override
    public AlertAnalysisResult analyze(AlertRequest.Alert alert) {

        String jfrFilePath = "C:/Users/brian/ThirdProject/jacket-cpu-test.jfr";;

        try {
            List<MethodCpuAnalysis> methods =
                    jfrCpuAnalyzer.analyze(jfrFilePath);

            String methodAnalysis =
                    methods.stream()
                            .limit(MAX_CANDIDATES)
                            .map(method ->
                                    "- %s (평균 CPU: %.2f, 최대 CPU: %.2f, Sample: %d)"
                                            .formatted(
                                                    method.methodName(),
                                                    method.averageCpuLoad(),
                                                    method.maxCpuLoad(),
                                                    method.sampleCount()
                                            )
                            )
                            .collect(Collectors.joining("\n"));

            String analysis = """
                    CPU 사용량 증가가 감지되었습니다.
                    
                    JFR CPU 분석:
                    %s
                    """.formatted(methodAnalysis);

            return new AlertAnalysisResult(
                    alert.labels().get("alertname"),
                    alert.labels().get("severity"),
                    alert.labels().get("instance"),
                    analysis
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "JFR CPU 분석에 실패했습니다.",
                    e
            );
        }
    }

//    @Override
//    public AlertAnalysisResult analyze(AlertRequest.Alert alert) {
//        ThreadDumpAnalysisResult threadAnalysis =
//                threadDumpService.analyzeThreadDump();
//
//        List<RunnableMethodCount> candidates =
//                threadAnalysis.runnableMethods().stream()
//                        .limit(MAX_CANDIDATES)
//                        .toList();
//
//        String candidateAnalysis =
//                candidates.stream()
//                        .map(candidate ->
//                                "- %s (%d개 RUNNABLE Thread)"
//                                        .formatted(
//                                                candidate.methodName(),
//                                                candidate.count()
//                                        )
//                        )
//                        .collect(Collectors.joining("\n"));
//
//        String analysis = """
//                CPU 사용량 증가가 감지되었습니다.
//
//                Thread Dump 분석:
//                - RUNNABLE: %d
//                - BLOCKED: %d
//                - WAITING: %d
//                - TIMED_WAITING: %d
//
//                CPU 원인 후보:
//                %s
//                """.formatted(
//                threadAnalysis.runnableThreads(),
//                threadAnalysis.blockedThreads(),
//                threadAnalysis.waitingThreads(),
//                threadAnalysis.timedWaitingThreads(),
//                candidateAnalysis
//        );
//
//        return new AlertAnalysisResult(
//                alert.labels().get("alertname"),
//                alert.labels().get("severity"),
//                alert.labels().get("instance"),
//                analysis
//        );
//    }
}