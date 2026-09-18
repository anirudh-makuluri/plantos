using Confluent.Kafka;
using Microsoft.Extensions.Options;
using PlantOS.TelemetryProcessor.Configuration;
using PlantOS.TelemetryProcessor.Processing;
using PlantOS.TelemetryProcessor.Publishing;

namespace PlantOS.TelemetryProcessor;

public sealed class TelemetryProcessorWorker : BackgroundService
{
    private readonly TelemetryMessageProcessor _processor;
    private readonly IDerivedEventPublisher _publisher;
    private readonly KafkaProcessorOptions _options;
    private readonly ILogger<TelemetryProcessorWorker> _logger;
    private readonly IConsumer<string, string> _consumer;

    public TelemetryProcessorWorker(
        TelemetryMessageProcessor processor,
        IDerivedEventPublisher publisher,
        IOptions<KafkaProcessorOptions> options,
        ILogger<TelemetryProcessorWorker> logger)
    {
        _processor = processor;
        _publisher = publisher;
        _options = options.Value;
        _logger = logger;
        _consumer = new ConsumerBuilder<string, string>(new ConsumerConfig
        {
            BootstrapServers = _options.BootstrapServers,
            GroupId = _options.ConsumerGroupId,
            ClientId = _options.ClientId,
            AutoOffsetReset = AutoOffsetReset.Earliest,
            EnableAutoCommit = false
        }).Build();
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _consumer.Subscribe(_options.TelemetryTopic);
        _logger.LogInformation(
            "Consuming raw telemetry from {TelemetryTopic} as group {ConsumerGroupId}",
            _options.TelemetryTopic,
            _options.ConsumerGroupId);

        try
        {
            while (!stoppingToken.IsCancellationRequested)
            {
                var consumed = _consumer.Consume(stoppingToken);
                var result = _processor.Process(consumed.Message.Key, consumed.Message.Value);

                if (!result.IsAccepted)
                {
                    _logger.LogWarning(
                        "Rejected telemetry at {TopicPartitionOffset}: {Errors}",
                        consumed.TopicPartitionOffset,
                        string.Join(" ", result.Errors));
                    _consumer.Commit(consumed);
                    continue;
                }

                await _publisher.PublishAsync(result.Health!, result.Anomalies, stoppingToken);
                _consumer.Commit(consumed);

                _logger.LogInformation(
                    "Processed telemetry {SourceEventId} for {MachineCode} as {HealthStatus} score {HealthScore} with {AnomalyCount} anomalies",
                    result.Health!.SourceTelemetryEventId,
                    result.Health.MachineCode,
                    result.Health.HealthStatus,
                    result.Health.HealthScore,
                    result.Anomalies.Count);
            }
        }
        catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
        {
            _logger.LogInformation("Telemetry processor cancellation requested");
        }
        finally
        {
            _consumer.Close();
            var remainingMessages = _publisher.Flush(TimeSpan.FromSeconds(_options.FlushTimeoutSeconds));
            _logger.LogInformation(
                "Telemetry processor stopped; {RemainingMessages} derived messages remain queued",
                remainingMessages);
        }
    }

    public override void Dispose()
    {
        _consumer.Dispose();
        base.Dispose();
    }
}
