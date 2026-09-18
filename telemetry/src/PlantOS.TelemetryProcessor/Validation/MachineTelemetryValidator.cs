using PlantOS.Telemetry.Contracts;

namespace PlantOS.TelemetryProcessor.Validation;

public sealed class MachineTelemetryValidator
{
    public TelemetryValidationResult Validate(string? messageKey, MachineTelemetryV1 telemetry)
    {
        var errors = new List<string>();

        Require(messageKey is not null && messageKey.Length > 0, "Kafka message key is required.", errors);
        Require(telemetry.EventId != Guid.Empty, "eventId is required.", errors);
        Require(
            telemetry.SchemaVersion == MachineTelemetryV1.CurrentSchemaVersion,
            $"schemaVersion must be {MachineTelemetryV1.CurrentSchemaVersion}.",
            errors);
        Require(
            string.Equals(telemetry.EventType, MachineTelemetryV1.CurrentEventType, StringComparison.Ordinal),
            $"eventType must be {MachineTelemetryV1.CurrentEventType}.",
            errors);
        Require(!string.IsNullOrWhiteSpace(telemetry.MachineCode), "machineCode is required.", errors);
        Require(
            string.Equals(messageKey, telemetry.MachineCode, StringComparison.Ordinal),
            "Kafka message key must match machineCode.",
            errors);
        Require(telemetry.CorrelationId != Guid.Empty, "correlationId is required.", errors);
        Require(telemetry.CausationId != Guid.Empty, "causationId is required.", errors);
        Require(telemetry.SequenceNumber > 0, "sequenceNumber must be greater than zero.", errors);
        Require(IsUtc(telemetry.OccurredAt), "occurredAt must be a non-default UTC timestamp.", errors);
        Require(IsUtc(telemetry.ProducedAt), "producedAt must be a non-default UTC timestamp.", errors);
        Require(telemetry.ProducedAt >= telemetry.OccurredAt, "producedAt cannot precede occurredAt.", errors);
        Require(
            double.IsFinite(telemetry.TemperatureCelsius) && telemetry.TemperatureCelsius >= -273.15,
            "temperatureCelsius must be finite and physically possible.",
            errors);
        Require(
            double.IsFinite(telemetry.VibrationMillimetersPerSecond) &&
            telemetry.VibrationMillimetersPerSecond >= 0,
            "vibrationMillimetersPerSecond must be finite and non-negative.",
            errors);
        Require(
            double.IsFinite(telemetry.PowerKilowatts) && telemetry.PowerKilowatts >= 0,
            "powerKilowatts must be finite and non-negative.",
            errors);
        Require(telemetry.Rpm >= 0, "rpm must be non-negative.", errors);
        Require(telemetry.UnitsProduced >= 0, "unitsProduced must be non-negative.", errors);

        return new TelemetryValidationResult(errors);
    }

    private static bool IsUtc(DateTimeOffset value) =>
        value != default && value.Offset == TimeSpan.Zero;

    private static void Require(bool condition, string error, ICollection<string> errors)
    {
        if (!condition)
        {
            errors.Add(error);
        }
    }
}
