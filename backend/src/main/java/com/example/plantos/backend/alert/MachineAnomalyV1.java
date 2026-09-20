package com.example.plantos.backend.alert;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

/** Matches the JSON published by PlantOS.Telemetry.Contracts.MachineAnomalyV1. */
public record MachineAnomalyV1(
        @NotNull UUID eventId,
        @NotNull @Min(1) @Max(1) Integer schemaVersion,
        @NotNull @Pattern(regexp = "MachineAnomaly") String eventType,
        @NotNull Instant occurredAt,
        @NotNull Instant producedAt,
        @NotBlank @Size(max = 50) String machineCode,
        @NotNull UUID correlationId,
        @NotNull UUID causationId,
        @NotNull UUID sourceTelemetryEventId,
        @NotNull AnomalyType anomalyType,
        @NotNull AlertSeverity severity,
        @NotNull Double observedValue,
        @NotNull Double thresholdValue,
        @NotBlank @Size(max = 10) String unit,
        @NotNull @Min(0) @Max(100) Integer healthScore
) {}
