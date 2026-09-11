package org.example.incidentanalyzer.rule;

import org.example.incidentanalyzer.alert.AlertType;
import org.example.incidentanalyzer.alert.AlertRequest;
import org.example.incidentanalyzer.analysis.AlertAnalysisResult;
import org.example.incidentanalyzer.diagnostic.ThreadDumpService;
import org.springframework.stereotype.Component;

@Component
public class HighCpuAnalyzer implements AlertAnalyzer {

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

        String threadAnalysis = threadDumpService.analyzeThreadDump();

        return new AlertAnalysisResult(
                alert.labels().get("alertname"),
                alert.labels().get("severity"),
                alert.labels().get("instance"),
                """
                CPU 사용량 증가가 감지되었습니다.

                Thread Dump 분석:
                %s
                """.formatted(threadAnalysis)
        );
    }
}