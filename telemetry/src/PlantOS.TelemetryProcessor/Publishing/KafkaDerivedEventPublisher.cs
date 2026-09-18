using System.Text.Json;
using Confluent.Kafka;
using Microsoft.Extensions.Options;
using PlantOS.Telemetry.Contracts;
using PlantOS.TelemetryProcessor.Configuration;

namespace PlantOS.TelemetryProcessor.Publishing;

public sealed class KafkaDerivedEventPublisher : IDerivedEventPublisher, IDisposable
{
    private readonly KafkaProcessorOptions _options;
    private readonly ILogger<KafkaDerivedEventPublisher> _logger;
    private readonly IProducer<string, string> _producer;

    public KafkaDerivedEventPublisher(
        IOptions<KafkaProcessorOptions> options,
        ILogger<KafkaDerivedEventPublisher> logger)
    {
        _options = options.Value;
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
        MachineHealthV1 health,
        IReadOnlyList<MachineAnomalyV1> anomalies,
        CancellationToken cancellationToken)
    {
        await ProduceAsync(
            _options.HealthTopic,
            health.MachineCode,
            health.EventType,
            health.ProducedAt,
            health,
            cancellationToken);

        foreach (var anomaly in anomalies)
        {
            await ProduceAsync(
                _options.AnomalyTopic,
                anomaly.MachineCode,
                anomaly.EventType,
                anomaly.ProducedAt,
                anomaly,
                cancellationToken);
        }
    }

    public int Flush(TimeSpan timeout) => _producer.Flush(timeout);

    public void Dispose() => _producer.Dispose();

    private async Task ProduceAsync<TEvent>(
        string topic,
        string machineCode,
        string eventType,
        DateTimeOffset producedAt,
        TEvent @event,
        CancellationToken cancellationToken)
    {
        var payload = JsonSerializer.Serialize(@event, PlantOSJson.SerializerOptions);
        var delivery = await _producer.ProduceAsync(
            topic,
            new Message<string, string>
            {
                Key = machineCode,
                Value = payload,
                Timestamp = new Timestamp(producedAt.UtcDateTime)
            },
            cancellationToken);

        _logger.LogInformation(
            "Published {EventType} for {MachineCode} to {Topic} partition {Partition} offset {Offset}",
            eventType,
            machineCode,
            topic,
            delivery.Partition.Value,
            delivery.Offset.Value);
    }
}
