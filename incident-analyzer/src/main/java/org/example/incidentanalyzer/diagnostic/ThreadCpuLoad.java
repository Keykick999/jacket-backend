package org.example.incidentanalyzer.diagnostic;

import java.time.Instant;

/**
 * Thread별 CPU 사용량
 */
public record ThreadCpuLoad(
        String threadName,   // Thread 이름
        float userLoad,      // 사용자 영역 CPU 사용량
        float systemLoad,    // 시스템 영역 CPU 사용량
        Instant timestamp    // CPU 사용량 측정 시각
) {

    public float totalLoad() {
        return userLoad + systemLoad;
    }
}