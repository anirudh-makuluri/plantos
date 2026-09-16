using Microsoft.Extensions.Options;
using PlantOS.MachineSimulator.Configuration;
using PlantOS.MachineSimulator.Generation;
using PlantOS.MachineSimulator.Publishing;

namespace PlantOS.MachineSimulator;

public sealed class TelemetryWorker(
    IMachineTelemetryGenerator generator,
    ITelemetryPublisher publisher,
    IOptions<SimulatorOptions> simulatorOptions,
    IOptions<KafkaOptions> kafkaOptions,
    ILogger<TelemetryWorker> logger) : BackgroundService
{
    private readonly SimulatorOptions _simulatorOptions = simulatorOptions.Value;
    private readonly KafkaOptions _kafkaOptions = kafkaOptions.Value;
    private readonly Guid _simulationRunId = Guid.NewGuid();

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        logger.LogInformation(
            "Starting simulated telemetry run {SimulationRunId} for {MachineCount} machines",
            _simulationRunId,
            _simulatorOptions.Machines.Count);

        try
        {
            while (!stoppingToken.IsCancellationRequested)
            {
                foreach (var machine in _simulatorOptions.Machines)
                {
                    var telemetry = generator.Next(machine, _simulationRunId);
                    await publisher.PublishAsync(telemetry, stoppingToken);
                }

                await Task.Delay(_simulatorOptions.IntervalMilliseconds, stoppingToken);
            }
        }
        catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
        {
            logger.LogInformation("Telemetry cancellation requested");
        }
        finally
        {
            var remainingMessages = publisher.Flush(TimeSpan.FromSeconds(_kafkaOptions.FlushTimeoutSeconds));
            logger.LogInformation(
                "Stopped simulated telemetry run {SimulationRunId}; {RemainingMessages} messages remain queued",
                _simulationRunId,
                remainingMessages);
        }
    }
}
