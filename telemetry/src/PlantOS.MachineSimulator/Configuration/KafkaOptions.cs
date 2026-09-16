namespace PlantOS.MachineSimulator.Configuration;

public sealed class KafkaOptions
{
    public const string SectionName = "Kafka";

    public string BootstrapServers { get; init; } = "localhost:9092";
    public string Topic { get; init; } = "plantos.machine.telemetry.v1";
    public string ClientId { get; init; } = "plantos-machine-simulator";
    public int MessageTimeoutMilliseconds { get; init; } = 5_000;
    public int FlushTimeoutSeconds { get; init; } = 10;
}
