package org.example.incidentanalyzer.diagnostic;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class JfrCpuAnalyzerTest {

    @Test
    void analyzesCheckedInJfrRecording() throws Exception {
        Path jfr = resolveJfr();
        assumeTrue(Files.exists(jfr), "jacket-cpu-test.jfr이 필요합니다.");

        List<MethodCpuAnalysis> methods = new JfrCpuAnalyzer().analyze(jfr.toString());

        assertThat(methods).isNotNull();
        methods.forEach(method -> {
            assertThat(method.methodName()).isNotBlank();
            assertThat(method.sampleCount()).isGreaterThan(0);
        });
    }

    private Path resolveJfr() {
        Path cwd = Path.of(System.getProperty("user.dir"));
        Path direct = cwd.resolve("jacket-cpu-test.jfr");
        if (Files.exists(direct)) {
            return direct;
        }
        Path parent = cwd.getParent() == null
                ? null
                : cwd.getParent().resolve("jacket-cpu-test.jfr");
        return parent == null ? direct : parent;
    }
}
