package org.example.incidentanalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "incident")
public class IncidentProperties {

    private final Llm llm = new Llm();
    private final Prometheus prometheus = new Prometheus();
    private final Jfr jfr = new Jfr();
    private final ThreadDump threaddump = new ThreadDump();

    public Llm getLlm() {
        return llm;
    }

    public Prometheus getPrometheus() {
        return prometheus;
    }

    public Jfr getJfr() {
        return jfr;
    }

    public ThreadDump getThreaddump() {
        return threaddump;
    }

    public static class Llm {
        private boolean enabled = true;
        private int maxToolCalls = 4;
        private Duration timeout = Duration.ofSeconds(180);
        private Duration cacheTtl = Duration.ofMinutes(10);
        private int maxEvidenceBytes = 4096;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxToolCalls() {
            return maxToolCalls;
        }

        public void setMaxToolCalls(int maxToolCalls) {
            this.maxToolCalls = maxToolCalls;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public Duration getCacheTtl() {
            return cacheTtl;
        }

        public void setCacheTtl(Duration cacheTtl) {
            this.cacheTtl = cacheTtl;
        }

        public int getMaxEvidenceBytes() {
            return maxEvidenceBytes;
        }

        public void setMaxEvidenceBytes(int maxEvidenceBytes) {
            this.maxEvidenceBytes = maxEvidenceBytes;
        }
    }

    public static class Prometheus {
        private String baseUrl = "http://localhost:9090";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Jfr {
        private String directory = System.getProperty("user.dir");
        private String fileName = "jacket-cpu-test.jfr";

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }

    public static class ThreadDump {
        private String url = "http://localhost:8080/actuator/threaddump";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
