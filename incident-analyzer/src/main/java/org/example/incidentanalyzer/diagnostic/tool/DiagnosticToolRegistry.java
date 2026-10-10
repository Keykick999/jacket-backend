package org.example.incidentanalyzer.diagnostic.tool;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DiagnosticToolRegistry {

    private final Map<String, DiagnosticTool<?>> tools = new LinkedHashMap<>();

    public DiagnosticToolRegistry(List<DiagnosticTool<?>> registeredTools) {
        for (DiagnosticTool<?> tool : registeredTools) {
            if (tools.containsKey(tool.name())) {
                throw new IllegalStateException("중복된 진단 도구 이름입니다: " + tool.name());
            }
            tools.put(tool.name(), tool);
        }
    }

    public Optional<DiagnosticTool<?>> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public boolean contains(String name) {
        return tools.containsKey(name);
    }

    public List<ToolDefinition> toolDefinitions() {
        return tools.values().stream()
                .map(this::toDefinition)
                .toList();
    }

    public List<ToolCallback> schemaOnlyCallbacks() {
        return toolDefinitions().stream()
                .map(SchemaOnlyToolCallback::new)
                .map(ToolCallback.class::cast)
                .toList();
    }

    private ToolDefinition toDefinition(DiagnosticTool<?> tool) {
        return ToolDefinition.builder()
                .name(tool.name())
                .description(tool.description())
                .inputSchema(JsonSchemaGenerator.generateForType(tool.inputType()))
                .build();
    }
}
