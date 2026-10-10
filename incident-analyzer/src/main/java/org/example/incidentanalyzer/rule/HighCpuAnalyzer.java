package org.example.incidentanalyzer.rule;

import org.example.incidentanalyzer.alert.AlertRequest;
import org.example.incidentanalyzer.alert.AlertType;
import org.example.incidentanalyzer.analysis.AlertAnalysisResult;
import org.example.incidentanalyzer.diagnostic.JfrCpuAnalyzer;
import org.example.incidentanalyzer.diagnostic.JfrFileResolver;
import org.example.incidentanalyzer.diagnostic.MethodCpuAnalysis;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HighCpuAnalyzer implements AlertAnalyzer {

    private static final int MAX_CANDIDATES = 5;

    private final JfrCpuAnalyzer jfrCpuAnalyzer;
    private final JfrFileResolver jfrFileResolver;

    public HighCpuAnalyzer(
            JfrCpuAnalyzer jfrCpuAnalyzer,
            JfrFileResolver jfrFileResolver
    ) {
        this.jfrCpuAnalyzer = jfrCpuAnalyzer;
        this.jfrFileResolver = jfrFileResolver;
    }

    @Override
    public boolean supports(AlertType alertType) {
        return alertType == AlertType.HIGH_CPU;
    }

    @Override
    public AlertAnalysisResult analyze(AlertRequest.Alert alert) {
        Path jfrFilePath = jfrFileResolver.resolve();

        if (!Files.exists(jfrFilePath)) {
            throw new IllegalStateException(
                    "JFR 파일을 찾을 수 없습니다: " + jfrFilePath
            );
        }

        try {
            List<MethodCpuAnalysis> methods =
                    jfrCpuAnalyzer.analyze(jfrFilePath.toString());

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
                    alert.label("alertname"),
                    alert.label("severity"),
                    alert.label("instance"),
                    analysis
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "JFR CPU 분석에 실패했습니다.",
                    e
            );
        }
    }
}
