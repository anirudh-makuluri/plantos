namespace PlantOS.TelemetryProcessor.Validation;

public sealed record TelemetryValidationResult(IReadOnlyList<string> Errors)
{
    public bool IsValid => Errors.Count == 0;
}
