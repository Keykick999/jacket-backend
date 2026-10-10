package org.example.incidentanalyzer.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmReportParserTest {

    private final LlmReportParser parser = new LlmReportParser(new ObjectMapper());

    @Test
    void parsesPlainJson() {
        LlmIncidentReport report = parser.parse("""
                {
                  "summary": "CPU가 높습니다",
                  "causeCandidates": [
                    {
                      "title": "busy loop",
                      "confidence": "HIGH",
                      "reasoning": "JFR 상위 메서드",
                      "evidenceIds": ["ev-analyze_jfr-1"],
                      "isInference": false
                    }
                  ],
                  "uncertainties": [],
                  "followUpChecks": ["로그 확인"],
                  "recommendedActions": ["해당 메서드 점검"],
                  "ruleComparison": "Rule 결과와 일치"
                }
                """, Set.of("ev-analyze_jfr-1"));

        assertThat(report.summary()).isEqualTo("CPU가 높습니다");
        assertThat(report.causeCandidates()).hasSize(1);
        assertThat(report.causeCandidates().get(0).confidence()).isEqualTo(Confidence.HIGH);
        assertThat(report.causeCandidates().get(0).reasoning()).doesNotContain("미확인");
    }

    @Test
    void parsesFencedJson() {
        LlmIncidentReport report = parser.parse("""
                ```json
                {"summary":"요약","causeCandidates":[]}
                ```
                """, Set.of());

        assertThat(report.summary()).isEqualTo("요약");
        assertThat(report.causeCandidates()).isEmpty();
    }

    @Test
    void marksUnknownEvidenceIds() {
        LlmIncidentReport report = parser.parse("""
                {
                  "summary": "요약",
                  "causeCandidates": [
                    {
                      "title": "guess",
                      "confidence": "LOW",
                      "reasoning": "추측",
                      "evidenceIds": ["ev-missing-1"],
                      "isInference": true
                    }
                  ]
                }
                """, Set.of("ev-analyze_jfr-1"));

        assertThat(report.causeCandidates().get(0).reasoning()).contains("근거 ID 미확인: ev-missing-1");
    }

    @Test
    void rejectsMissingSummary() {
        assertThatThrownBy(() -> parser.parse(
                "{\"causeCandidates\":[]}",
                Set.of()
        )).isInstanceOf(LlmAnalysisException.class)
                .hasMessageContaining("summary");
    }

    @Test
    void rejectsMissingCauseCandidates() {
        assertThatThrownBy(() -> parser.parse(
                "{\"summary\":\"요약\"}",
                Set.of()
        )).isInstanceOf(LlmAnalysisException.class)
                .hasMessageContaining("causeCandidates");
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> parser.parse("not json", Set.of()))
                .isInstanceOf(LlmAnalysisException.class);
    }

    @Test
    void parsesToolCallsFromFirstPassJson() {
        List<RequestedToolCall> toolCalls = parser.parseToolCalls("""
                {
                  "toolCalls": [
                    {"name":"analyze_jfr","arguments":{"topN":5}},
                    {"name":"collect_thread_dump","arguments":"{\\"topN\\":8}"}
                  ]
                }
                """);

        assertThat(toolCalls).hasSize(2);
        assertThat(toolCalls.get(0).name()).isEqualTo("analyze_jfr");
        assertThat(toolCalls.get(0).argumentsJson()).contains("topN");
        assertThat(toolCalls.get(1).name()).isEqualTo("collect_thread_dump");
        assertThat(toolCalls.get(1).argumentsJson()).contains("topN");
    }

    @Test
    void treatsMissingToolCallsAsEmpty() {
        assertThat(parser.parseToolCalls("""
                {"summary":"충분합니다","causeCandidates":[]}
                """)).isEmpty();
    }
}
