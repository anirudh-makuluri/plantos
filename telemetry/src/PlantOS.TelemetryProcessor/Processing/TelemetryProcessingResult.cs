using PlantOS.Telemetry.Contracts;

namespace PlantOS.TelemetryProcessor.Processing;

public sealed record TelemetryProcessingResult(
    MachineHealthV1? Health,
    IReadOnlyList<MachineAnomalyV1> Anomalies,
    IReadOnlyList<string> Errors)
{
    public bool IsAccepted => Health is not null && Errors.Count == 0;

    public static TelemetryProcessingResult Rejected(params IReadOnlyList<string> errors) =>
        new(null, [], errors);

    public static TelemetryProcessingResult Accepted(
        MachineHealthV1 health,
        IReadOnlyList<MachineAnomalyV1> anomalies) =>
        new(health, anomalies, []);
}
