package org.example.incidentanalyzer.diagnostic;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 서버에서 장애 원인 파악에 필요한 데이터를 수집한다.
 *
 * 현재는 Thread Dump를 수집하며,
 * 추후 Heap Dump, Metric, Log 등의 데이터 수집을 담당한다.
 */
@Component
public class ServerDataCollector {

    private final RestTemplate restTemplate;

    public ServerDataCollector() {
        this.restTemplate = new RestTemplate();
    }

    public String collectThreadDump() {
        String url = "http://localhost:8080/actuator/threaddump";

        return restTemplate.getForObject(url, String.class);
    }
}