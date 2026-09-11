package org.example.incidentanalyzer.alert;

import java.util.Arrays;

public enum AlertType {

    HIGH_CPU("JacketHighCPU");

    private final String alertName;

    AlertType(String alertName) {
        this.alertName = alertName;
    }

    public static AlertType from(String alertName) {
        return Arrays.stream(values())
                .filter(type -> type.alertName.equals(alertName))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown alert: " + alertName
                        )
                );
    }
}