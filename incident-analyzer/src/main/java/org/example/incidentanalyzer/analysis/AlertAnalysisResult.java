package org.example.incidentanalyzer.analysis;

/**
 * 장애 분석 결과를 표현한다.
 */
public record AlertAnalysisResult(
        String alertName,
        String severity,
        String instance,
        String analysis
) {
}