using System.Text.Json;
using Microsoft.Extensions.Options;
using Microsoft.Extensions.Time.Testing;
using PlantOS.Telemetry.Contracts;
using PlantOS.TelemetryProcessor.Configuration;
using PlantOS.TelemetryProcessor.Health;
using PlantOS.TelemetryProcessor.Processing;
using PlantOS.TelemetryProcessor.Validation;

namespace PlantOS.TelemetryProcessor.Tests;

public sealed class TelemetryMessageProcessorTests
{
    private static readonly DateTimeOffset ProcessedAt =
        new(2026, 9, 15, 12, 1, 0, TimeSpan.Zero);

    private readonly TelemetryMessageProcessor _processor = new(
        new MachineTelemetryValidator(),
        new MachineHealthEvaluator(Options.Create(new HealthRuleOptions())),
        new FakeTimeProvider(ProcessedAt));

    [Fact]
    public void Process_HealthyTelemetry_CreatesHealthEventOnly()
    {
        var telemetry = TelemetryTestData.Create();

        var result = _processor.Process(
            telemetry.MachineCode,
            JsonSerializer.Serialize(telemetry, PlantOSJson.SerializerOptions));

        Assert.True(result.IsAccepted);
        Assert.Empty(result.Errors);
        Assert.Empty(result.Anomalies);
        Assert.NotNull(result.Health);
        Assert.Equal(MachineHealthStatus.Healthy, result.Health.HealthStatus);
        Assert.Equal(100, result.Health.HealthScore);
        Assert.Equal(telemetry.EventId, result.Health.CausationId);
        Assert.Equal(ProcessedAt, result.Health.ProducedAt);
    }

    [Fact]
    public void Process_CriticalTelemetry_CreatesDeterministicHealthAndAnomalyEvents()
    {
        var telemetry = TelemetryTestData.Create(temperatureCelsius: 95);
        var payload = JsonSerializer.Serialize(telemetry, PlantOSJson.SerializerOptions);

        var first = _processor.Process(telemetry.MachineCode, payload);
        var replay = _processor.Process(telemetry.MachineCode, payload);

        Assert.True(first.IsAccepted);
        Assert.Equal(MachineHealthStatus.Critical, first.Health!.HealthStatus);
        var anomaly = Assert.Single(first.Anomalies);
        Assert.Equal(MachineAnomalyType.HighTemperature, anomaly.AnomalyType);
        Assert.Equal(MachineHealthStatus.Critical, anomaly.Severity);
        Assert.Equal(90, anomaly.ThresholdValue);
        Assert.Equal(first.Health.EventId, replay.Health!.EventId);
        Assert.Equal(anomaly.EventId, Assert.Single(replay.Anomalies).EventId);
    }

    [Fact]
    public void Process_MalformedJson_IsRejectedWithoutThrowing()
    {
        var result = _processor.Process("PRESS-001", "{not-json}");

        Assert.False(result.IsAccepted);
        Assert.Null(result.Health);
        Assert.Empty(result.Anomalies);
        Assert.Contains(result.Errors, error => error.StartsWith("Invalid telemetry JSON:"));
    }

    [Fact]
    public void Process_MissingRequiredFields_IsRejectedWithoutThrowing()
    {
        var result = _processor.Process("PRESS-001", "{}");

        Assert.False(result.IsAccepted);
        Assert.Contains(result.Errors, error => error.StartsWith("Invalid telemetry JSON:"));
    }

    [Fact]
    public void Serialize_DerivedEvents_UsesUppercaseContractEnums()
    {
        var telemetry = TelemetryTestData.Create(temperatureCelsius: 95);
        var result = _processor.Process(
            telemetry.MachineCode,
            JsonSerializer.Serialize(telemetry, PlantOSJson.SerializerOptions));

        var healthJson = JsonSerializer.Serialize(result.Health, PlantOSJson.SerializerOptions);
        var anomalyJson = JsonSerializer.Serialize(result.Anomalies[0], PlantOSJson.SerializerOptions);

        Assert.Contains("\"healthStatus\":\"CRITICAL\"", healthJson);
        Assert.Contains("\"anomalyType\":\"HIGH_TEMPERATURE\"", anomalyJson);
        Assert.Contains("\"severity\":\"CRITICAL\"", anomalyJson);
    }
}
