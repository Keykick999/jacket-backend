package org.example.incidentanalyzer.diagnostic;

import org.example.incidentanalyzer.config.IncidentProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 서버에서 장애 원인 파악에 필요한 데이터를 수집한다.
 *
 * 현재는 Thread Dump를 수집하며,
 * 추후 Heap Dump, Metric, Log 등의 데이터 수집을 담당한다.
 */
@Component
public class ServerDataCollector {

    private final RestTemplate restTemplate;
    private final IncidentProperties properties;

    public ServerDataCollector(IncidentProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }

    public String collectThreadDump() {
        String url = properties.getThreaddump().getUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.TEXT_PLAIN));

        String threadDump = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        ).getBody();

        System.out.println("===== THREAD DUMP RESPONSE =====");
        System.out.println(threadDump);

        return threadDump;
    }
}
