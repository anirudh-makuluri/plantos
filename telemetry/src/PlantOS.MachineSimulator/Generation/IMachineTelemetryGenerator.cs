using PlantOS.MachineSimulator.Configuration;
using PlantOS.Telemetry.Contracts;

namespace PlantOS.MachineSimulator.Generation;

public interface IMachineTelemetryGenerator
{
    MachineTelemetryV1 Next(MachineProfile machine, Guid simulationRunId);
}
