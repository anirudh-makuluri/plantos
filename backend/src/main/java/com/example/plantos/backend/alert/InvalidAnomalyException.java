package com.example.plantos.backend.alert;

public class InvalidAnomalyException extends RuntimeException {
    public InvalidAnomalyException(String message) {
        super(message);
    }
}
