namespace PlantOS.TelemetryProcessor.Configuration;

public sealed class HealthRuleOptions
{
    public const string SectionName = "HealthRules";

    public double TemperatureWarningCelsius { get; init; } = 75;
    public double TemperatureCriticalCelsius { get; init; } = 90;
    public double VibrationWarningMillimetersPerSecond { get; init; } = 4;
    public double VibrationCriticalMillimetersPerSecond { get; init; } = 7;
}
