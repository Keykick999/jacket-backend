package org.example.incidentanalyzer.slack;

import org.example.incidentanalyzer.analysis.AlertAnalysisResult;
import org.example.incidentanalyzer.analysis.IncidentReport;
import org.example.incidentanalyzer.analysis.LlmStatus;
import org.example.incidentanalyzer.llm.CauseCandidate;
import org.example.incidentanalyzer.llm.LlmIncidentReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SlackNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SlackNotificationService.class);
    private static final int MAX_MESSAGE_LENGTH = 3000;

    private final String slackWebhookUrl;
    private final RestTemplate restTemplate;

    public SlackNotificationService(
            @Value("${slack.webhook.url:}") String slackWebhookUrl
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
                "Jacket Incident\n\n" +
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

    public void sendIncidentReport(IncidentReport report) {
        sendNotification(formatIncidentReport(report));
    }

    public String formatIncidentReport(IncidentReport report) {
        AlertAnalysisResult rule = report.ruleResult();
        StringBuilder message = new StringBuilder();
        message.append("[Alert]\n")
                .append("Alert: ").append(rule.alertName()).append('\n')
                .append("Severity: ").append(rule.severity()).append('\n')
                .append("Instance: ").append(rule.instance()).append("\n\n")
                .append("[Rule 기반 분석 (정량)]\n")
                .append(nullToEmpty(rule.analysis())).append("\n\n");

        if (report.llmStatus() == LlmStatus.SUCCESS && report.llmReport() != null) {
            appendLlmSection(message, report.llmReport());
        } else {
            message.append("[LLM 분석 (추론, 검증 필요)]\n")
                    .append("상태: ").append(report.llmStatus());
            if (report.llmFailureReason() != null && !report.llmFailureReason().isBlank()) {
                message.append("\n사유: ").append(report.llmFailureReason());
            }
        }

        return truncate(message.toString());
    }

    private void appendLlmSection(StringBuilder message, LlmIncidentReport llmReport) {
        message.append("[LLM 분석 (추론, 검증 필요)]\n")
                .append("요약: ").append(nullToEmpty(llmReport.summary())).append("\n\n")
                .append("원인 후보:\n")
                .append(formatCandidates(llmReport.causeCandidates())).append('\n');

        if (llmReport.recommendedActions() != null && !llmReport.recommendedActions().isEmpty()) {
            message.append("권장 대응:\n")
                    .append(joinLines(llmReport.recommendedActions())).append('\n');
        }
        if (llmReport.uncertainties() != null && !llmReport.uncertainties().isEmpty()) {
            message.append("한계/불확실성:\n")
                    .append(joinLines(llmReport.uncertainties())).append('\n');
        }
        if (llmReport.followUpChecks() != null && !llmReport.followUpChecks().isEmpty()) {
            message.append("추가 확인:\n")
                    .append(joinLines(llmReport.followUpChecks())).append('\n');
        }
        if (llmReport.ruleComparison() != null && !llmReport.ruleComparison().isBlank()) {
            message.append("Rule 대비: ").append(llmReport.ruleComparison()).append('\n');
        }
    }

    private String formatCandidates(List<CauseCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "- 없음\n";
        }
        return candidates.stream()
                .limit(3)
                .map(candidate -> {
                    String evidence = candidate.evidenceIds() == null || candidate.evidenceIds().isEmpty()
                            ? "없음"
                            : String.join(", ", candidate.evidenceIds());
                    return "- %s (%s)%s\n  근거: %s\n  설명: %s".formatted(
                            nullToEmpty(candidate.title()),
                            candidate.confidence(),
                            candidate.isInference() ? " [추론]" : "",
                            evidence,
                            nullToEmpty(candidate.reasoning())
                    );
                })
                .collect(Collectors.joining("\n"));
    }

    private String joinLines(List<String> values) {
        return values.stream()
                .map(value -> "- " + value)
                .collect(Collectors.joining("\n"));
    }

    public void sendNotification(String message) {
        if (slackWebhookUrl == null || slackWebhookUrl.isBlank()) {
            log.warn("Slack webhook URL이 없어 알림을 건너뜁니다.");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("text", message);

        try {
            restTemplate.postForEntity(
                    slackWebhookUrl,
                    payload,
                    String.class
            );
            log.info("Slack 장애 분석 알림을 전송했습니다.");
        } catch (Exception e) {
            log.warn("Slack 알림 전송에 실패했습니다.", e);
        }
    }

    private String truncate(String message) {
        if (message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_LENGTH - 15) + "\n...[truncated]";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
