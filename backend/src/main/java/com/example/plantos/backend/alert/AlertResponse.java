package com.example.plantos.backend.alert;

import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        Long id, String machineCode, AnomalyType anomalyType, AlertSeverity severity, AlertStatus status,
        double observedValue, double thresholdValue, String unit, int healthScore,
        Instant firstSeenAt, Instant lastSeenAt, long occurrenceCount, UUID latestEventId,
        Instant acknowledgedAt, Instant resolvedAt, String resolutionNote
) {
    public static AlertResponse from(Alert alert) {
        return new AlertResponse(alert.getId(), alert.getMachineCode(), alert.getAnomalyType(),
                alert.getSeverity(), alert.getStatus(), alert.getObservedValue(), alert.getThresholdValue(),
                alert.getUnit(), alert.getHealthScore(), alert.getFirstSeenAt(), alert.getLastSeenAt(),
                alert.getOccurrenceCount(), alert.getLatestEventId(), alert.getAcknowledgedAt(),
                alert.getResolvedAt(), alert.getResolutionNote());
    }
}
