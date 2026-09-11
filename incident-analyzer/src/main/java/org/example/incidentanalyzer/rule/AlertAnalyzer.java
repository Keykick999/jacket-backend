package org.example.incidentanalyzer.rule;

import org.example.incidentanalyzer.alert.AlertType;
import org.example.incidentanalyzer.alert.AlertRequest;
import org.example.incidentanalyzer.analysis.AlertAnalysisResult;

public interface AlertAnalyzer {

    boolean supports(AlertType alertType);

    AlertAnalysisResult analyze(AlertRequest.Alert alert);
}