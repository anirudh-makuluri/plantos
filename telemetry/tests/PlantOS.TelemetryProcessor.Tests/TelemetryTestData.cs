using PlantOS.Telemetry.Contracts;

namespace PlantOS.TelemetryProcessor.Tests;

internal static class TelemetryTestData
{
    public static MachineTelemetryV1 Create(
        double temperatureCelsius = 60,
        double vibrationMillimetersPerSecond = 2,
        int schemaVersion = MachineTelemetryV1.CurrentSchemaVersion)
    {
        var occurredAt = new DateTimeOffset(2026, 9, 15, 12, 0, 0, TimeSpan.Zero);

        return new MachineTelemetryV1
        {
            EventId = Guid.Parse("11111111-1111-1111-1111-111111111111"),
            SchemaVersion = schemaVersion,
            OccurredAt = occurredAt,
            ProducedAt = occurredAt.AddMilliseconds(5),
            MachineCode = "PRESS-001",
            CorrelationId = Guid.Parse("22222222-2222-2222-2222-222222222222"),
            CausationId = Guid.Parse("33333333-3333-3333-3333-333333333333"),
            SequenceNumber = 1,
            TemperatureCelsius = temperatureCelsius,
            VibrationMillimetersPerSecond = vibrationMillimetersPerSecond,
            PowerKilowatts = 18.5,
            Rpm = 950,
            UnitsProduced = 2
        };
    }
}
