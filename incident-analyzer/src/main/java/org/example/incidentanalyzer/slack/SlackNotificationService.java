package org.example.incidentanalyzer.slack;

import org.example.incidentanalyzer.analysis.AlertAnalysisResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class SlackNotificationService {

    private final String slackWebhookUrl;
    private final RestTemplate restTemplate;

    public SlackNotificationService(
            @Value("${slack.webhook.url}") String slackWebhookUrl
    ) {
        this.slackWebhookUrl = slackWebhookUrl;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 장애 분석 결과를 Slack 메시지로 만들어 전송한다.
     */
    public void sendAnalysisResult(
            AlertAnalysisResult analysisResult
    ) {
        String message = String.format(
                "🚨 Jacket Incident\n\n" +
                        "Alert: %s\n" +
                        "Severity: %s\n" +
                        "Instance: %s\n\n" +
                        "Analysis:\n%s",
                analysisResult.alertName(),
                analysisResult.severity(),
                analysisResult.instance(),
                analysisResult.analysis()
        );

        sendNotification(message);
    }

    public void sendNotification(String message) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("text", message);

        try {
            restTemplate.postForEntity(
                    slackWebhookUrl,
                    payload,
                    String.class
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}