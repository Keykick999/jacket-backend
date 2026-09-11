package org.example.incidentanalyzer.controller;

import org.example.incidentanalyzer.alert.AlertRequest;
import org.example.incidentanalyzer.analysis.AlertAnalysisResult;
import org.example.incidentanalyzer.analysis.RuleBasedAnalyzer;
import org.example.incidentanalyzer.slack.SlackNotificationService;
import org.springframework.web.bind.annotation.*;

import static org.example.incidentanalyzer.alert.AlertRequest.Alert;

@RestController
@RequestMapping("/api/incidents")
public class AlertController {
    private final RuleBasedAnalyzer ruleBasedAnalyzer;
    private final SlackNotificationService slackNotificationService;

    public AlertController(
            RuleBasedAnalyzer ruleBasedAnalyzer,
            SlackNotificationService slackNotificationService
    ) {
        this.ruleBasedAnalyzer = ruleBasedAnalyzer;
        this.slackNotificationService = slackNotificationService;
    }

    @PostMapping("/alerts")
    public void receiveAlert(@RequestBody AlertRequest alertRequest) {

        System.out.println("===== ALERT RECEIVED =====");

        for (Alert alert : alertRequest.alerts()) {

            AlertAnalysisResult analysisResult = ruleBasedAnalyzer.analyze(alert);

            System.out.println("===== ANALYSIS RESULT =====");
            System.out.println(analysisResult);

            slackNotificationService.sendAnalysisResult(analysisResult);
        }
    }
}