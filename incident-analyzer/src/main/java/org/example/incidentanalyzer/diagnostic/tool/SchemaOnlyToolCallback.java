package org.example.incidentanalyzer.diagnostic.tool;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * 모델에 도구 스키마만 전달한다. 실제 실행은 DiagnosticToolExecutor가 담당한다.
 */
public class SchemaOnlyToolCallback implements ToolCallback {

    private final ToolDefinition definition;

    public SchemaOnlyToolCallback(ToolDefinition definition) {
        this.definition = definition;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return definition;
    }

    @Override
    public String call(String toolInput) {
        throw new UnsupportedOperationException(
                "Diagnostic tools must be executed by DiagnosticToolExecutor"
        );
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return call(toolInput);
    }
}
