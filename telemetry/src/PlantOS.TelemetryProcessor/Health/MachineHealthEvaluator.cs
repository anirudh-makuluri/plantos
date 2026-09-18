using Microsoft.Extensions.Options;
using PlantOS.Telemetry.Contracts;
using PlantOS.TelemetryProcessor.Configuration;

namespace PlantOS.TelemetryProcessor.Health;

public sealed class MachineHealthEvaluator(IOptions<HealthRuleOptions> options)
{
    private readonly HealthRuleOptions _rules = options.Value;

    public HealthEvaluation Evaluate(MachineTelemetryV1 telemetry)
    {
        var violations = new List<ThresholdViolation>();

        var temperaturePenalty = EvaluateMeasurement(
            telemetry.TemperatureCelsius,
            _rules.TemperatureWarningCelsius,
            _rules.TemperatureCriticalCelsius,
            MachineAnomalyType.HighTemperature,
            "C",
            violations);
        var vibrationPenalty = EvaluateMeasurement(
            telemetry.VibrationMillimetersPerSecond,
            _rules.VibrationWarningMillimetersPerSecond,
            _rules.VibrationCriticalMillimetersPerSecond,
            MachineAnomalyType.HighVibration,
            "mm/s",
            violations);

        var status = violations.Any(violation => violation.Severity == MachineHealthStatus.Critical)
            ? MachineHealthStatus.Critical
            : violations.Count > 0
                ? MachineHealthStatus.Warning
                : MachineHealthStatus.Healthy;
        var score = Math.Clamp(100 - temperaturePenalty - vibrationPenalty, 0, 100);

        return new HealthEvaluation(status, score, violations);
    }

    private static int EvaluateMeasurement(
        double value,
        double warningThreshold,
        double criticalThreshold,
        MachineAnomalyType anomalyType,
        string unit,
        ICollection<ThresholdViolation> violations)
    {
        if (value >= criticalThreshold)
        {
            violations.Add(new ThresholdViolation(
                anomalyType,
                MachineHealthStatus.Critical,
                value,
                criticalThreshold,
                unit));

            var criticalOverage = (value - criticalThreshold) / (criticalThreshold - warningThreshold);
            return 60 + Math.Min(20, (int)Math.Round(criticalOverage * 20));
        }

        if (value >= warningThreshold)
        {
            violations.Add(new ThresholdViolation(
                anomalyType,
                MachineHealthStatus.Warning,
                value,
                warningThreshold,
                unit));

            var warningProgress = (value - warningThreshold) / (criticalThreshold - warningThreshold);
            return 20 + (int)Math.Round(warningProgress * 20);
        }

        return 0;
    }
}
