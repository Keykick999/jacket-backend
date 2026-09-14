package org.example.incidentanalyzer.diagnostic;

/**
 * 애플리케이션 메서드별 CPU 분석 결과
 */
public record MethodCpuAnalysis(
        String methodName,
        float averageCpuLoad,
        float maxCpuLoad,
        long sampleCount
) {}