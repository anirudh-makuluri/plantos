using System.Text.Json;
using Confluent.Kafka;
using Microsoft.Extensions.Options;
using PlantOS.MachineSimulator.Configuration;
using PlantOS.Telemetry.Contracts;

namespace PlantOS.MachineSimulator.Publishing;

public sealed class KafkaTelemetryPublisher : ITelemetryPublisher, IDisposable
{
    private static readonly JsonSerializerOptions SerializerOptions = new(JsonSerializerDefaults.Web);

    private readonly KafkaOptions _options;
    private readonly TimeProvider _timeProvider;
    private readonly ILogger<KafkaTelemetryPublisher> _logger;
    private readonly IProducer<string, string> _producer;

    public KafkaTelemetryPublisher(
        IOptions<KafkaOptions> options,
        TimeProvider timeProvider,
        ILogger<KafkaTelemetryPublisher> logger)
    {
        _options = options.Value;
        _timeProvider = timeProvider;
        _logger = logger;
        _producer = new ProducerBuilder<string, string>(new ProducerConfig
        {
            BootstrapServers = _options.BootstrapServers,
            ClientId = _options.ClientId,
            Acks = Acks.All,
            EnableIdempotence = true,
            MessageTimeoutMs = _options.MessageTimeoutMilliseconds
        }).Build();
    }

    public async Task PublishAsync(
        MachineTelemetryV1 telemetry,
        CancellationToken cancellationToken)
    {
        var publishedTelemetry = telemetry with { ProducedAt = _timeProvider.GetUtcNow() };
        var payload = JsonSerializer.Serialize(publishedTelemetry, SerializerOptions);

        var delivery = await _producer.ProduceAsync(
            _options.Topic,
            new Message<string, string>
            {
                Key = publishedTelemetry.MachineCode,
                Value = payload,
                Timestamp = new Timestamp(publishedTelemetry.ProducedAt.UtcDateTime)
            },
            cancellationToken);

        _logger.LogInformation(
            "Published {EventType} for {MachineCode} sequence {SequenceNumber} to partition {Partition} offset {Offset}",
            publishedTelemetry.EventType,
            publishedTelemetry.MachineCode,
            publishedTelemetry.SequenceNumber,
            delivery.Partition.Value,
            delivery.Offset.Value);
    }

    public int Flush(TimeSpan timeout) => _producer.Flush(timeout);

    public void Dispose() => _producer.Dispose();
}
