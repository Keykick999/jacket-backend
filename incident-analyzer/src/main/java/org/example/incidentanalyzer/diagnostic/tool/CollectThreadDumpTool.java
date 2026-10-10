package org.example.incidentanalyzer.diagnostic.tool;

import org.example.incidentanalyzer.diagnostic.ThreadDumpService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class CollectThreadDumpTool implements DiagnosticTool<CollectThreadDumpRequest> {

    private static final int DEFAULT_TOP_N = 8;
    private static final int DEFAULT_MAX_STACK_DEPTH = 15;
    private static final int MAX_STACK_DEPTH = 20;

    private final ThreadDumpService threadDumpService;

    public CollectThreadDumpTool(ThreadDumpService threadDumpService) {
        this.threadDumpService = threadDumpService;
    }

    @Override
    public String name() {
        return "collect_thread_dump";
    }

    @Override
    public String description() {
        return "Collect a live thread dump from the application actuator endpoint "
                + "and return state counts, lock waits, and top stacks.";
    }

    @Override
    public Class<CollectThreadDumpRequest> inputType() {
        return CollectThreadDumpRequest.class;
    }

    @Override
    public void validate(CollectThreadDumpRequest input) {
        if (input != null && input.maxStackDepth() != null
                && (input.maxStackDepth() < 1 || input.maxStackDepth() > MAX_STACK_DEPTH)) {
            throw new InvalidToolParameterException(
                    "maxStackDepth는 1 이상 %d 이하여야 합니다.".formatted(MAX_STACK_DEPTH)
            );
        }
        if (input != null && input.topN() != null && input.topN() < 1) {
            throw new InvalidToolParameterException("topN은 1 이상이어야 합니다.");
        }
    }

    @Override
    public Object execute(CollectThreadDumpRequest input, AnalysisContext context) {
        int topN = input == null || input.topN() == null ? DEFAULT_TOP_N : input.topN();
        int maxStackDepth = input == null || input.maxStackDepth() == null
                ? DEFAULT_MAX_STACK_DEPTH
                : input.maxStackDepth();

        var result = threadDumpService.analyzeThreadDump(topN, maxStackDepth);
        return Map.of(
                "collectedAt", Instant.now().toString(),
                "source", "actuator/threaddump",
                "instance", String.valueOf(context.instance()),
                "analysis", result
        );
    }
}
