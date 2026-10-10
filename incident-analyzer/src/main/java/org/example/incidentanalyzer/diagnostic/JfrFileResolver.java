package org.example.incidentanalyzer.diagnostic;

import org.example.incidentanalyzer.config.IncidentProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Component
public class JfrFileResolver {

    private final IncidentProperties properties;

    public JfrFileResolver(IncidentProperties properties) {
        this.properties = properties;
    }

    public Path resolve() {
        Path configuredDir = Path.of(properties.getJfr().getDirectory())
                .toAbsolutePath()
                .normalize();
        String fileName = properties.getJfr().getFileName();

        Path configuredFile = configuredDir.resolve(fileName).normalize();
        if (isAllowedExistingFile(configuredDir, configuredFile)) {
            return configuredFile;
        }

        Path parent = configuredDir.getParent();
        if (parent != null) {
            Path fallback = parent.resolve(fileName).normalize();
            if (Files.exists(fallback) && Files.isRegularFile(fallback)) {
                return fallback;
            }
        }

        return configuredFile;
    }

    public Instant lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant();
        } catch (IOException ex) {
            return Instant.now();
        }
    }

    private boolean isAllowedExistingFile(Path directory, Path file) {
        return file.startsWith(directory)
                && Files.exists(file)
                && Files.isRegularFile(file);
    }
}
