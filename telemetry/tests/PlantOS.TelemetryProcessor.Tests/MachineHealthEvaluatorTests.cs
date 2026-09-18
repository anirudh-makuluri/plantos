using Microsoft.Extensions.Options;
using PlantOS.Telemetry.Contracts;
using PlantOS.TelemetryProcessor.Configuration;
using PlantOS.TelemetryProcessor.Health;

namespace PlantOS.TelemetryProcessor.Tests;

public sealed class MachineHealthEvaluatorTests
{
    private readonly MachineHealthEvaluator _evaluator = new(Options.Create(new HealthRuleOptions()));

    [Fact]
    public void Evaluate_BelowWarningThresholds_ReturnsHealthyWithFullScore()
    {
        var result = _evaluator.Evaluate(TelemetryTestData.Create(74.99, 3.99));

        Assert.Equal(MachineHealthStatus.Healthy, result.Status);
        Assert.Equal(100, result.Score);
        Assert.Empty(result.Violations);
    }

    [Fact]
    public void Evaluate_AtTemperatureWarningThreshold_ReturnsWarning()
    {
        var result = _evaluator.Evaluate(TelemetryTestData.Create(75, 2));

        Assert.Equal(MachineHealthStatus.Warning, result.Status);
        Assert.Equal(80, result.Score);
        var violation = Assert.Single(result.Violations);
        Assert.Equal(MachineAnomalyType.HighTemperature, violation.AnomalyType);
        Assert.Equal(75, violation.ThresholdValue);
    }

    [Fact]
    public void Evaluate_AtVibrationCriticalThreshold_ReturnsCritical()
    {
        var result = _evaluator.Evaluate(TelemetryTestData.Create(60, 7));

        Assert.Equal(MachineHealthStatus.Critical, result.Status);
        Assert.Equal(40, result.Score);
        var violation = Assert.Single(result.Violations);
        Assert.Equal(MachineAnomalyType.HighVibration, violation.AnomalyType);
        Assert.Equal(MachineHealthStatus.Critical, violation.Severity);
    }

    [Fact]
    public void Evaluate_TwoCriticalMeasurements_CombinesPenalties()
    {
        var result = _evaluator.Evaluate(TelemetryTestData.Create(90, 7));

        Assert.Equal(MachineHealthStatus.Critical, result.Status);
        Assert.Equal(0, result.Score);
        Assert.Equal(2, result.Violations.Count);
    }
}
