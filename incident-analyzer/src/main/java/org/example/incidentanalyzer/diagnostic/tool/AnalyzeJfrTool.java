package org.example.incidentanalyzer.diagnostic.tool;

import org.example.incidentanalyzer.diagnostic.JfrCpuAnalyzer;
import org.example.incidentanalyzer.diagnostic.JfrFileResolver;
import org.example.incidentanalyzer.diagnostic.MethodCpuAnalysis;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
public class AnalyzeJfrTool implements DiagnosticTool<AnalyzeJfrRequest> {

    private static final int DEFAULT_TOP_N = 5;
    private static final int MAX_TOP_N = 10;

    private final JfrCpuAnalyzer jfrCpuAnalyzer;
    private final JfrFileResolver jfrFileResolver;

    public AnalyzeJfrTool(
            JfrCpuAnalyzer jfrCpuAnalyzer,
            JfrFileResolver jfrFileResolver
    ) {
        this.jfrCpuAnalyzer = jfrCpuAnalyzer;
        this.jfrFileResolver = jfrFileResolver;
    }

    @Override
    public String name() {
        return "analyze_jfr";
    }

    @Override
    public String description() {
        return "Analyze the configured JFR recording and return top application methods by CPU impact. "
                + "Do not pass a file path. The current file may not have been captured at incident time.";
    }

    @Override
    public Class<AnalyzeJfrRequest> inputType() {
        return AnalyzeJfrRequest.class;
    }

    @Override
    public void validate(AnalyzeJfrRequest input) {
        if (input != null && input.topN() != null
                && (input.topN() < 1 || input.topN() > MAX_TOP_N)) {
            throw new InvalidToolParameterException(
                    "topN은 1 이상 %d 이하여야 합니다.".formatted(MAX_TOP_N)
            );
        }
    }

    @Override
    public Object execute(AnalyzeJfrRequest input, AnalysisContext context) {
        Path file = jfrFileResolver.resolve();
        if (!Files.exists(file)) {
            throw new IllegalStateException("설정된 JFR 파일을 찾을 수 없습니다: " + file.getFileName());
        }

        try {
            int topN = input == null || input.topN() == null ? DEFAULT_TOP_N : input.topN();
            List<MethodCpuAnalysis> methods = jfrCpuAnalyzer.analyze(file.toString())
                    .stream()
                    .limit(topN)
                    .toList();

            return Map.of(
                    "fileName", file.getFileName().toString(),
                    "collectedAt", jfrFileResolver.lastModified(file).toString(),
                    "warning", "이 JFR 파일은 장애 시점에 수집된 기록이 아닐 수 있습니다.",
                    "methods", methods
            );
        } catch (Exception ex) {
            throw new IllegalStateException("JFR 분석에 실패했습니다: " + ex.getMessage(), ex);
        }
    }
}
