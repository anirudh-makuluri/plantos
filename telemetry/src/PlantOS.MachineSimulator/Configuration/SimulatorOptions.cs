namespace PlantOS.MachineSimulator.Configuration;

public sealed class SimulatorOptions
{
    public const string SectionName = "Simulator";

    public int IntervalMilliseconds { get; init; } = 1_000;
    public IReadOnlyList<MachineProfile> Machines { get; init; } = [];
}

public sealed class MachineProfile
{
    public string MachineCode { get; init; } = string.Empty;
    public double BaseTemperatureCelsius { get; init; }
    public double BaseVibrationMillimetersPerSecond { get; init; }
    public double BasePowerKilowatts { get; init; }
    public int BaseRpm { get; init; }
    public int UnitsPerReading { get; init; }
}
