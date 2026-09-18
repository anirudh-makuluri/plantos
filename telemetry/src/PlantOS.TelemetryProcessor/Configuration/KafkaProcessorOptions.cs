namespace PlantOS.TelemetryProcessor.Configuration;

public sealed class KafkaProcessorOptions
{
    public const string SectionName = "Kafka";

    public string BootstrapServers { get; init; } = "localhost:9092";
    public string ConsumerGroupId { get; init; } = "plantos-telemetry-processor-v1";
    public string ClientId { get; init; } = "plantos-telemetry-processor";
    public string TelemetryTopic { get; init; } = "plantos.machine.telemetry.v1";
    public string HealthTopic { get; init; } = "plantos.machine.health.v1";
    public string AnomalyTopic { get; init; } = "plantos.machine.anomaly.v1";
    public int MessageTimeoutMilliseconds { get; init; } = 5_000;
    public int FlushTimeoutSeconds { get; init; } = 10;
}
