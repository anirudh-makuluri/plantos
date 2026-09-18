using System.Text.Json;
using PlantOS.Telemetry.Contracts;
using PlantOS.TelemetryProcessor.Health;
using PlantOS.TelemetryProcessor.Validation;

namespace PlantOS.TelemetryProcessor.Processing;

public sealed class TelemetryMessageProcessor(
    MachineTelemetryValidator validator,
    MachineHealthEvaluator evaluator,
    TimeProvider timeProvider)
{
    public TelemetryProcessingResult Process(string? messageKey, string? payload)
    {
        if (string.IsNullOrWhiteSpace(payload))
        {
            return TelemetryProcessingResult.Rejected("Kafka message value is required.");
        }

        MachineTelemetryV1? telemetry;
        try
        {
            telemetry = JsonSerializer.Deserialize<MachineTelemetryV1>(
                payload,
                PlantOSJson.SerializerOptions);
        }
        catch (JsonException exception)
        {
            return TelemetryProcessingResult.Rejected($"Invalid telemetry JSON: {exception.Message}");
        }

        if (telemetry is null)
        {
            return TelemetryProcessingResult.Rejected("Telemetry JSON cannot be null.");
        }

        var validation = validator.Validate(messageKey, telemetry);
        if (!validation.IsValid)
        {
            return TelemetryProcessingResult.Rejected(validation.Errors);
        }

        var evaluation = evaluator.Evaluate(telemetry);
        var producedAt = timeProvider.GetUtcNow();
        var health = CreateHealthEvent(telemetry, evaluation, producedAt);
        var anomalies = evaluation.Violations
            .Select(violation => CreateAnomalyEvent(telemetry, evaluation, violation, producedAt))
            .ToArray();

        return TelemetryProcessingResult.Accepted(health, anomalies);
    }

    private static MachineHealthV1 CreateHealthEvent(
        MachineTelemetryV1 telemetry,
        HealthEvaluation evaluation,
        DateTimeOffset producedAt) =>
        new()
        {
            EventId = DerivedEventId.Create(telemetry.EventId, "health"),
            OccurredAt = telemetry.OccurredAt,
            ProducedAt = producedAt,
            MachineCode = telemetry.MachineCode,
            CorrelationId = telemetry.CorrelationId,
            CausationId = telemetry.EventId,
            SourceTelemetryEventId = telemetry.EventId,
            HealthStatus = evaluation.Status,
            HealthScore = evaluation.Score,
            TemperatureCelsius = telemetry.TemperatureCelsius,
            VibrationMillimetersPerSecond = telemetry.VibrationMillimetersPerSecond,
            PowerKilowatts = telemetry.PowerKilowatts,
            Rpm = telemetry.Rpm,
            UnitsProduced = telemetry.UnitsProduced
        };

    private static MachineAnomalyV1 CreateAnomalyEvent(
        MachineTelemetryV1 telemetry,
        HealthEvaluation evaluation,
        ThresholdViolation violation,
        DateTimeOffset producedAt) =>
        new()
        {
            EventId = DerivedEventId.Create(
                telemetry.EventId,
                $"anomaly:{violation.AnomalyType}"),
            OccurredAt = telemetry.OccurredAt,
            ProducedAt = producedAt,
            MachineCode = telemetry.MachineCode,
            CorrelationId = telemetry.CorrelationId,
            CausationId = telemetry.EventId,
            SourceTelemetryEventId = telemetry.EventId,
            AnomalyType = violation.AnomalyType,
            Severity = violation.Severity,
            ObservedValue = violation.ObservedValue,
            ThresholdValue = violation.ThresholdValue,
            Unit = violation.Unit,
            HealthScore = evaluation.Score
        };
}
