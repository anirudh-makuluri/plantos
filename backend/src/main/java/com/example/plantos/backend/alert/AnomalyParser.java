package com.example.plantos.backend.alert;

import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;

@Component
public class AnomalyParser {
    private final JsonMapper json;
    private final Validator validator;

    public AnomalyParser(JsonMapper json, Validator validator) {
        this.json = json;
        this.validator = validator;
    }

    public MachineAnomalyV1 parse(String key, String payload) {
        if (payload == null || payload.isBlank()) {
            throw new InvalidAnomalyException("Anomaly payload is required");
        }
        MachineAnomalyV1 event;
        try {
            event = json.readValue(payload, MachineAnomalyV1.class);
        } catch (JacksonException ex) {
            throw new InvalidAnomalyException("Malformed anomaly JSON or unsupported field value");
        }
        validate(key, event);
        return event;
    }

    public void validate(String key, MachineAnomalyV1 event) {
        if (event == null || !validator.validate(event).isEmpty()) {
            throw new InvalidAnomalyException("Missing or invalid anomaly fields");
        }
        if (!event.machineCode().equals(key) || !key.equals(key.strip())) {
            throw new InvalidAnomalyException("Kafka key must match machineCode without surrounding whitespace");
        }
        if (isEmpty(event.eventId()) || isEmpty(event.correlationId()) || isEmpty(event.causationId())
                || isEmpty(event.sourceTelemetryEventId())) {
            throw new InvalidAnomalyException("Event identifiers cannot be empty UUIDs");
        }
        if (!event.causationId().equals(event.sourceTelemetryEventId())) {
            throw new InvalidAnomalyException("Anomaly causation must identify its source telemetry event");
        }
        if (!Double.isFinite(event.observedValue()) || !Double.isFinite(event.thresholdValue())
                || event.observedValue() < event.thresholdValue()) {
            throw new InvalidAnomalyException("Anomaly must contain a finite reading at or above its threshold");
        }
        String expectedUnit = event.anomalyType() == AnomalyType.HIGH_TEMPERATURE ? "C" : "mm/s";
        if (!expectedUnit.equals(event.unit())) {
            throw new InvalidAnomalyException("Unit does not match anomaly type");
        }
    }

    private boolean isEmpty(UUID value) {
        return value.getMostSignificantBits() == 0 && value.getLeastSignificantBits() == 0;
    }
}
