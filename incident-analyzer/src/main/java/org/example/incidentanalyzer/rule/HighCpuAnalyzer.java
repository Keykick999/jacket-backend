package org.example.incidentanalyzer.rule;

import org.example.incidentanalyzer.alert.AlertRequest;
import org.example.incidentanalyzer.alert.AlertType;
import org.example.incidentanalyzer.analysis.AlertAnalysisResult;
import org.example.incidentanalyzer.diagnostic.RunnableMethodCount;
import org.example.incidentanalyzer.diagnostic.ThreadDumpAnalysisResult;
import org.example.incidentanalyzer.diagnostic.ThreadDumpService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class HighCpuAnalyzer implements AlertAnalyzer {

    private static final int MAX_CANDIDATES = 5;

    private final ThreadDumpService threadDumpService;

    public HighCpuAnalyzer(ThreadDumpService threadDumpService) {
        this.threadDumpService = threadDumpService;
    }

    @Override
    public boolean supports(AlertType alertType) {
        return alertType == AlertType.HIGH_CPU;
    }

    @Override
    public AlertAnalysisResult analyze(AlertRequest.Alert alert) {
        ThreadDumpAnalysisResult threadAnalysis =
                threadDumpService.analyzeThreadDump();

        List<RunnableMethodCount> candidates =
                threadAnalysis.runnableMethods().stream()
                        .limit(MAX_CANDIDATES)
                        .toList();

        String candidateAnalysis =
                candidates.stream()
                        .map(candidate ->
                                "- %s (%d개 RUNNABLE Thread)"
                                        .formatted(
                                                candidate.methodName(),
                                                candidate.count()
                                        )
                        )
                        .collect(Collectors.joining("\n"));

        String analysis = """
                CPU 사용량 증가가 감지되었습니다.

                Thread Dump 분석:
                - RUNNABLE: %d
                - BLOCKED: %d
                - WAITING: %d
                - TIMED_WAITING: %d

                CPU 원인 후보:
                %s
                """.formatted(
                threadAnalysis.runnableThreads(),
                threadAnalysis.blockedThreads(),
                threadAnalysis.waitingThreads(),
                threadAnalysis.timedWaitingThreads(),
                candidateAnalysis
        );

        return new AlertAnalysisResult(
                alert.labels().get("alertname"),
                alert.labels().get("severity"),
                alert.labels().get("instance"),
                analysis
        );
    }
}