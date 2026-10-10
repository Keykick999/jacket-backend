package org.example.incidentanalyzer.diagnostic.tool;

public interface DiagnosticTool<I> {

    String name();

    String description();

    Class<I> inputType();

    void validate(I input);

    Object execute(I input, AnalysisContext context);
}
