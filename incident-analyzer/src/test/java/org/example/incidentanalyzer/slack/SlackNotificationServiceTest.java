package org.example.incidentanalyzer.slack;

import org.example.incidentanalyzer.analysis.AlertAnalysisResult;
import org.example.incidentanalyzer.analysis.IncidentReport;
import org.example.incidentanalyzer.analysis.LlmStatus;
import org.example.incidentanalyzer.llm.CauseCandidate;
import org.example.incidentanalyzer.llm.Confidence;
import org.example.incidentanalyzer.llm.LlmIncidentReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SlackNotificationServiceTest {

    private final SlackNotificationService service =
            new SlackNotificationService("http://localhost/dummy");

    @Test
    void formatsSuccessfulLlmReportSeparatelyFromRuleResult() {
        String message = service.formatIncidentReport(new IncidentReport(
                new AlertAnalysisResult("JacketHighCPU", "warning", "localhost", "정량 분석"),
                new LlmIncidentReport(
                        "CPU 병목 추정",
                        List.of(new CauseCandidate(
                                "busy loop",
                                Confidence.HIGH,
                                "JFR 상위 메서드",
                                List.of("ev-analyze_jfr-1"),
                                false
                        )),
                        List.of("로그 없음"),
                        List.of("해당 코드 확인"),
                        List.of("부하 테스트 중단"),
                        "Rule 상위 메서드와 일치"
                ),
                LlmStatus.SUCCESS,
                null
        ));

        assertThat(message).contains("[Rule 기반 분석 (정량)]");
        assertThat(message).contains("정량 분석");
        assertThat(message).contains("[LLM 분석 (추론, 검증 필요)]");
        assertThat(message).contains("busy loop");
        assertThat(message).contains("ev-analyze_jfr-1");
    }

    @Test
    void formatsFailedLlmWithRuleResultOnly() {
        String message = service.formatIncidentReport(new IncidentReport(
                new AlertAnalysisResult("JacketHighCPU", "warning", "localhost", "정량 분석"),
                null,
                LlmStatus.FAILED,
                "timeout"
        ));

        assertThat(message).contains("정량 분석");
        assertThat(message).contains("FAILED");
        assertThat(message).contains("timeout");
        assertThat(message).doesNotContain("원인 후보");
    }
}
