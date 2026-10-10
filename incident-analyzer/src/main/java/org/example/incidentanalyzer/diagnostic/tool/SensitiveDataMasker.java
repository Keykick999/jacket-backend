package org.example.incidentanalyzer.diagnostic.tool;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SensitiveDataMasker {

    private static final Pattern BEARER_TOKEN =
            Pattern.compile("(?i)bearer\\s+[A-Za-z0-9\\-._~+/]+=*");
    private static final Pattern SECRET_ASSIGNMENT =
            Pattern.compile("(?i)(password|token|api[_-]?key|secret|authorization)\\s*[=:]\\s*[^\\s,;\"']+");
    private static final Pattern GOOGLE_API_KEY =
            Pattern.compile("AIza[0-9A-Za-z_\\-]{10,}");
    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    public String mask(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String masked = BEARER_TOKEN.matcher(raw).replaceAll("Bearer ***");
        masked = SECRET_ASSIGNMENT.matcher(masked).replaceAll("$1=***");
        masked = GOOGLE_API_KEY.matcher(masked).replaceAll("***");
        return EMAIL.matcher(masked).replaceAll("***@***");
    }
}
