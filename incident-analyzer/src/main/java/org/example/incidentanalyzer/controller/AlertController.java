package org.example.incidentanalyzer.controller;

import org.example.incidentanalyzer.alert.AlertRequest;
import org.example.incidentanalyzer.analysis.IncidentAnalysisService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class AlertController {

    private final IncidentAnalysisService incidentAnalysisService;

    public AlertController(IncidentAnalysisService incidentAnalysisService) {
        this.incidentAnalysisService = incidentAnalysisService;
    }

    @PostMapping("/alerts")
    public void receiveAlert(@RequestBody AlertRequest alertRequest) {
        incidentAnalysisService.analyzeAsync(alertRequest);
    }
}
