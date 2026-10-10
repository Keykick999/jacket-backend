package org.example.incidentanalyzer.diagnostic.tool;

public class InvalidToolParameterException extends RuntimeException {

    public InvalidToolParameterException(String message) {
        super(message);
    }
}
