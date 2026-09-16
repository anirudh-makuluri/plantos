using System.Text.Json.Serialization;

namespace PlantOS.Telemetry.Contracts;

public sealed record MachineTelemetryV1
{
    public const int CurrentSchemaVersion = 1;
    public const string CurrentEventType = "MachineTelemetry";

    [JsonPropertyName("eventId")]
    public required Guid EventId { get; init; }

    [JsonPropertyName("schemaVersion")]
    public int SchemaVersion { get; init; } = CurrentSchemaVersion;

    [JsonPropertyName("eventType")]
    public string EventType { get; init; } = CurrentEventType;

    [JsonPropertyName("occurredAt")]
    public required DateTimeOffset OccurredAt { get; init; }

    [JsonPropertyName("producedAt")]
    public required DateTimeOffset ProducedAt { get; init; }

    [JsonPropertyName("machineCode")]
    public required string MachineCode { get; init; }

    [JsonPropertyName("correlationId")]
    public required Guid CorrelationId { get; init; }

    [JsonPropertyName("causationId")]
    public required Guid CausationId { get; init; }

    [JsonPropertyName("sequenceNumber")]
    public required long SequenceNumber { get; init; }

    [JsonPropertyName("temperatureCelsius")]
    public required double TemperatureCelsius { get; init; }

    [JsonPropertyName("vibrationMillimetersPerSecond")]
    public required double VibrationMillimetersPerSecond { get; init; }

    [JsonPropertyName("powerKilowatts")]
    public required double PowerKilowatts { get; init; }

    [JsonPropertyName("rpm")]
    public required int Rpm { get; init; }

    [JsonPropertyName("unitsProduced")]
    public required long UnitsProduced { get; init; }
}
