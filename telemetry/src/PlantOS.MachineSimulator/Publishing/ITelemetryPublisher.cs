using PlantOS.Telemetry.Contracts;

namespace PlantOS.MachineSimulator.Publishing;

public interface ITelemetryPublisher
{
    Task PublishAsync(MachineTelemetryV1 telemetry, CancellationToken cancellationToken);
    int Flush(TimeSpan timeout);
}
