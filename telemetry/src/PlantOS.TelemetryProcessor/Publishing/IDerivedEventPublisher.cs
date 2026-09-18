using PlantOS.Telemetry.Contracts;

namespace PlantOS.TelemetryProcessor.Publishing;

public interface IDerivedEventPublisher
{
    Task PublishAsync(
        MachineHealthV1 health,
        IReadOnlyList<MachineAnomalyV1> anomalies,
        CancellationToken cancellationToken);

    int Flush(TimeSpan timeout);
}
