package org.example.incidentanalyzer.alert;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AlertRequest(
        // Alertmanager가 이 Alert를 전달할 때 사용한 Receiver 이름
        String receiver,

        // 이번 Webhook 요청에 포함된 Alert들의 전체 상태
        String status,

        // Alertmanager가 전달한 개별 Alert 목록
        List<Alert> alerts
) {
    public record Alert(
            // 개별 Alert의 현재 상태 (firing, resolved 등)
            String status,

            // Alert를 식별하고 분류하기 위한 정보
            // 예: alertname, severity, service
            Map<String, String> labels,

            // Alert에 대한 설명 및 부가 정보
            // 예: summary, description
            Map<String, String> annotations,

            Instant startsAt,

            Instant endsAt,

            String fingerprint
    ) {
        public String label(String key) {
            if (labels == null) {
                return null;
            }
            return labels.get(key);
        }

        public String annotation(String key) {
            if (annotations == null) {
                return null;
            }
            return annotations.get(key);
        }
    }
}
