package org.example.incidentanalyzer.diagnostic;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Thread Dump를 수집하고 파싱하여 분석한다.
 */
@Component
public class ThreadDumpService {

    private final ServerDataCollector serverDataCollector;
    private final ThreadDumpParser threadDumpParser;
    private final ThreadDumpAnalyzer threadDumpAnalyzer;

    public ThreadDumpService(
            ServerDataCollector serverDataCollector,
            ThreadDumpParser threadDumpParser,
            ThreadDumpAnalyzer threadDumpAnalyzer
    ) {
        this.serverDataCollector = serverDataCollector;
        this.threadDumpParser = threadDumpParser;
        this.threadDumpAnalyzer = threadDumpAnalyzer;
    }

    public ThreadDumpAnalysisResult analyzeThreadDump() {
        String threadDump = serverDataCollector.collectThreadDump();

        List<ThreadInfo> threads = threadDumpParser.parse(threadDump);

        return threadDumpAnalyzer.analyze(threads);
    }
}