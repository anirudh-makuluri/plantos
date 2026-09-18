using PlantOS.Telemetry.Contracts;

namespace PlantOS.TelemetryProcessor.Health;

public sealed record ThresholdViolation(
    MachineAnomalyType AnomalyType,
    MachineHealthStatus Severity,
    double ObservedValue,
    double ThresholdValue,
    string Unit);
