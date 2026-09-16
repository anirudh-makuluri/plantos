using PlantOS.MachineSimulator.Configuration;
using PlantOS.Telemetry.Contracts;

namespace PlantOS.MachineSimulator.Generation;

public sealed class MachineTelemetryGenerator(TimeProvider timeProvider) : IMachineTelemetryGenerator
{
    private readonly Dictionary<string, MachineState> _states =
        new(StringComparer.OrdinalIgnoreCase);

    public MachineTelemetryV1 Next(MachineProfile machine, Guid simulationRunId)
    {
        var state = GetNextState(machine);
        var occurredAt = timeProvider.GetUtcNow();
        var phase = state.SequenceNumber + StablePhase(machine.MachineCode);

        return new MachineTelemetryV1
        {
            EventId = Guid.NewGuid(),
            OccurredAt = occurredAt,
            ProducedAt = occurredAt,
            MachineCode = machine.MachineCode,
            CorrelationId = simulationRunId,
            CausationId = simulationRunId,
            SequenceNumber = state.SequenceNumber,
            TemperatureCelsius = Round(machine.BaseTemperatureCelsius + Math.Sin(phase / 6d) * 1.5d),
            VibrationMillimetersPerSecond = Round(
                machine.BaseVibrationMillimetersPerSecond + Math.Sin(phase / 4d) * 0.2d),
            PowerKilowatts = Round(machine.BasePowerKilowatts + Math.Cos(phase / 5d) * 0.8d),
            Rpm = machine.BaseRpm + (int)Math.Round(Math.Sin(phase / 7d) * 12d),
            UnitsProduced = state.UnitsProduced
        };
    }

    private MachineState GetNextState(MachineProfile machine)
    {
        _states.TryGetValue(machine.MachineCode, out var previous);

        var next = new MachineState(
            previous.SequenceNumber + 1,
            previous.UnitsProduced + machine.UnitsPerReading);

        _states[machine.MachineCode] = next;
        return next;
    }

    private static int StablePhase(string machineCode) =>
        machineCode.Aggregate(0, (value, character) => (value + character) % 24);

    private static double Round(double value) => Math.Round(value, 2, MidpointRounding.AwayFromZero);

    private readonly record struct MachineState(long SequenceNumber, long UnitsProduced);
}
