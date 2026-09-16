using System.Text.Json;
using PlantOS.Telemetry.Contracts;

namespace PlantOS.MachineSimulator.Tests;

public sealed class MachineTelemetryContractTests
{
    [Fact]
    public void Serialize_UsesStableVersionedFieldNames()
    {
        var timestamp = new DateTimeOffset(2026, 9, 11, 12, 30, 0, TimeSpan.Zero);
        var telemetry = new MachineTelemetryV1
        {
            EventId = Guid.Parse("11111111-1111-1111-1111-111111111111"),
            OccurredAt = timestamp,
            ProducedAt = timestamp.AddMilliseconds(10),
            MachineCode = "PRESS-001",
            CorrelationId = Guid.Parse("22222222-2222-2222-2222-222222222222"),
            CausationId = Guid.Parse("33333333-3333-3333-3333-333333333333"),
            SequenceNumber = 7,
            TemperatureCelsius = 61.25,
            VibrationMillimetersPerSecond = 2.15,
            PowerKilowatts = 18.75,
            Rpm = 955,
            UnitsProduced = 14
        };

        using var document = JsonDocument.Parse(JsonSerializer.Serialize(telemetry));
        var root = document.RootElement;

        Assert.Equal(MachineTelemetryV1.CurrentSchemaVersion, root.GetProperty("schemaVersion").GetInt32());
        Assert.Equal(MachineTelemetryV1.CurrentEventType, root.GetProperty("eventType").GetString());
        Assert.Equal("PRESS-001", root.GetProperty("machineCode").GetString());
        Assert.Equal(7, root.GetProperty("sequenceNumber").GetInt64());
        Assert.Equal(61.25, root.GetProperty("temperatureCelsius").GetDouble());
        Assert.Equal(2.15, root.GetProperty("vibrationMillimetersPerSecond").GetDouble());
        Assert.Equal(18.75, root.GetProperty("powerKilowatts").GetDouble());
        Assert.Equal(955, root.GetProperty("rpm").GetInt32());
        Assert.Equal(14, root.GetProperty("unitsProduced").GetInt64());
    }
}
