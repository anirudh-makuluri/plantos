using PlantOS.Telemetry.Contracts;

namespace PlantOS.TelemetryProcessor.Health;

public sealed record HealthEvaluation(
    MachineHealthStatus Status,
    int Score,
    IReadOnlyList<ThresholdViolation> Violations);
