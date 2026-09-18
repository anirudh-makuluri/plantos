using System.Text.Json.Serialization;

namespace PlantOS.Telemetry.Contracts;

public sealed record MachineAnomalyV1
{
    public const int CurrentSchemaVersion = 1;
    public const string CurrentEventType = "MachineAnomaly";

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

    [JsonPropertyName("sourceTelemetryEventId")]
    public required Guid SourceTelemetryEventId { get; init; }

    [JsonPropertyName("anomalyType")]
    public required MachineAnomalyType AnomalyType { get; init; }

    [JsonPropertyName("severity")]
    public required MachineHealthStatus Severity { get; init; }

    [JsonPropertyName("observedValue")]
    public required double ObservedValue { get; init; }

    [JsonPropertyName("thresholdValue")]
    public required double ThresholdValue { get; init; }

    [JsonPropertyName("unit")]
    public required string Unit { get; init; }

    [JsonPropertyName("healthScore")]
    public required int HealthScore { get; init; }
}
