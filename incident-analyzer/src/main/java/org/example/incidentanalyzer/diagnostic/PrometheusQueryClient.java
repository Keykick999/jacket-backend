package org.example.incidentanalyzer.diagnostic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.incidentanalyzer.config.IncidentProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PrometheusQueryClient {

    private static final int MAX_SAMPLES = 40;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final IncidentProperties properties;

    public PrometheusQueryClient(
            IncidentProperties properties,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public List<String> listMetricNames() {
        URI uri = UriComponentsBuilder
                .fromUriString(properties.getPrometheus().getBaseUrl())
                .path("/api/v1/label/__name__/values")
                .build()
                .toUri();

        String body = restTemplate.getForObject(uri, String.class);
        JsonNode root = readTree(body);
        if (!isSuccess(root) || root.get("data") == null || !root.get("data").isArray()) {
            return List.of();
        }

        List<String> names = new ArrayList<>();
        for (JsonNode name : root.get("data")) {
            names.add(name.asText());
        }
        return List.copyOf(names);
    }

    public List<MetricSeries> queryRange(
            String query,
            Instant start,
            Instant end,
            int stepSeconds
    ) {
        URI uri = UriComponentsBuilder
                .fromUriString(properties.getPrometheus().getBaseUrl())
                .path("/api/v1/query_range")
                .queryParam("query", query)
                .queryParam("start", start.getEpochSecond())
                .queryParam("end", end.getEpochSecond())
                .queryParam("step", stepSeconds)
                .build()
                .encode()
                .toUri();

        String body = restTemplate.getForObject(uri, String.class);
        JsonNode root = readTree(body);
        if (!isSuccess(root)) {
            String error = root != null && root.has("error")
                    ? root.get("error").asText()
                    : "Prometheus query failed";
            throw new IllegalStateException(error);
        }

        JsonNode results = root.path("data").path("result");
        if (!results.isArray()) {
            return List.of();
        }

        List<MetricSeries> series = new ArrayList<>();
        for (JsonNode result : results) {
            Map<String, String> labels = new LinkedHashMap<>();
            JsonNode metric = result.get("metric");
            if (metric != null && metric.isObject()) {
                metric.fields().forEachRemaining(entry ->
                        labels.put(entry.getKey(), entry.getValue().asText()));
            }

            List<MetricSample> samples = downsample(parseSamples(result.get("values")));
            series.add(new MetricSeries(
                    labels.getOrDefault("__name__", query),
                    Map.copyOf(labels),
                    samples
            ));
        }
        return List.copyOf(series);
    }

    private List<MetricSample> parseSamples(JsonNode values) {
        List<MetricSample> samples = new ArrayList<>();
        if (values == null || !values.isArray()) {
            return samples;
        }
        for (JsonNode value : values) {
            if (!value.isArray() || value.size() < 2) {
                continue;
            }
            long epochSeconds = value.get(0).asLong();
            double sampleValue = value.get(1).asDouble();
            samples.add(new MetricSample(Instant.ofEpochSecond(epochSeconds), sampleValue));
        }
        return samples;
    }

    private List<MetricSample> downsample(List<MetricSample> samples) {
        if (samples.size() <= MAX_SAMPLES) {
            return List.copyOf(samples);
        }
        int step = (int) Math.ceil(samples.size() / (double) MAX_SAMPLES);
        List<MetricSample> reduced = new ArrayList<>();
        for (int i = 0; i < samples.size(); i += step) {
            reduced.add(samples.get(i));
        }
        MetricSample last = samples.get(samples.size() - 1);
        if (!reduced.get(reduced.size() - 1).equals(last)) {
            reduced.add(last);
        }
        return List.copyOf(reduced);
    }

    private JsonNode readTree(String body) {
        try {
            return objectMapper.readTree(body == null ? "{}" : body);
        } catch (Exception ex) {
            throw new IllegalStateException("Prometheus 응답을 파싱하지 못했습니다.", ex);
        }
    }

    private boolean isSuccess(JsonNode root) {
        return root != null && "success".equals(root.path("status").asText());
    }

    public record MetricSeries(
            String metric,
            Map<String, String> labels,
            List<MetricSample> samples
    ) {
    }

    public record MetricSample(
            Instant timestamp,
            double value
    ) {
    }
}
