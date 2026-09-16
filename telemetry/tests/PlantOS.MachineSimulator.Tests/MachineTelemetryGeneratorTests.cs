using Microsoft.Extensions.Time.Testing;
using PlantOS.MachineSimulator.Configuration;
using PlantOS.MachineSimulator.Generation;

namespace PlantOS.MachineSimulator.Tests;

public sealed class MachineTelemetryGeneratorTests
{
    private static readonly MachineProfile Press = new()
    {
        MachineCode = "PRESS-001",
        BaseTemperatureCelsius = 61,
        BaseVibrationMillimetersPerSecond = 2.1,
        BasePowerKilowatts = 18.5,
        BaseRpm = 950,
        UnitsPerReading = 2
    };

    [Fact]
    public void Next_IncrementsSequenceAndCumulativeProductionPerMachine()
    {
        var timeProvider = new FakeTimeProvider(
            new DateTimeOffset(2026, 9, 11, 12, 0, 0, TimeSpan.Zero));
        var generator = new MachineTelemetryGenerator(timeProvider);
        var runId = Guid.NewGuid();

        var first = generator.Next(Press, runId);
        timeProvider.Advance(TimeSpan.FromSeconds(1));
        var second = generator.Next(Press, runId);

        Assert.Equal(1, first.SequenceNumber);
        Assert.Equal(2, second.SequenceNumber);
        Assert.Equal(2, first.UnitsProduced);
        Assert.Equal(4, second.UnitsProduced);
        Assert.True(second.OccurredAt > first.OccurredAt);
        Assert.Equal(runId, first.CorrelationId);
        Assert.Equal(runId, first.CausationId);
    }

    [Fact]
    public void Next_GeneratesTheSameMeasurementsForTheSameMachineSequence()
    {
        var timestamp = new DateTimeOffset(2026, 9, 11, 12, 0, 0, TimeSpan.Zero);
        var firstGenerator = new MachineTelemetryGenerator(new FakeTimeProvider(timestamp));
        var secondGenerator = new MachineTelemetryGenerator(new FakeTimeProvider(timestamp));

        var first = firstGenerator.Next(Press, Guid.NewGuid());
        var second = secondGenerator.Next(Press, Guid.NewGuid());

        Assert.Equal(first.TemperatureCelsius, second.TemperatureCelsius);
        Assert.Equal(first.VibrationMillimetersPerSecond, second.VibrationMillimetersPerSecond);
        Assert.Equal(first.PowerKilowatts, second.PowerKilowatts);
        Assert.Equal(first.Rpm, second.Rpm);
    }
}
