package org.example.incidentanalyzer.analysis;

import org.example.incidentanalyzer.alert.AlertType;
import org.example.incidentanalyzer.alert.AlertRequest;
import org.example.incidentanalyzer.rule.AlertAnalyzer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Alert 유형에 맞는 Rule을 선택하여 규칙 기반으로 장애를 분석한다.
 */
@Component
public class RuleBasedAnalyzer {

    private final List<AlertAnalyzer> analyzers;

    public RuleBasedAnalyzer(List<AlertAnalyzer> analyzers) {
        this.analyzers = analyzers;
    }

    public AlertAnalysisResult analyze(AlertRequest.Alert alert) {
        String alertName = alert.labels().get("alertname");
        AlertType alertType = AlertType.from(alertName);

        return analyzers.stream()
                .filter(analyzer -> analyzer.supports(alertType))
                .findFirst()
                .map(analyzer -> analyzer.analyze(alert))
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "처리할 수 없는 Alert입니다."
                        )
                );
    }
}