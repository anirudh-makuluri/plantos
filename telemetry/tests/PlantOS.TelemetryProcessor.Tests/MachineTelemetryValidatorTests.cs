using PlantOS.TelemetryProcessor.Validation;

namespace PlantOS.TelemetryProcessor.Tests;

public sealed class MachineTelemetryValidatorTests
{
    private readonly MachineTelemetryValidator _validator = new();

    [Fact]
    public void Validate_ValidTelemetry_ReturnsNoErrors()
    {
        var result = _validator.Validate("PRESS-001", TelemetryTestData.Create());

        Assert.True(result.IsValid);
        Assert.Empty(result.Errors);
    }

    [Fact]
    public void Validate_UnsupportedVersionAndMismatchedKey_ReturnsBothErrors()
    {
        var result = _validator.Validate(
            "OTHER-001",
            TelemetryTestData.Create(schemaVersion: 2));

        Assert.False(result.IsValid);
        Assert.Contains(result.Errors, error => error.Contains("schemaVersion"));
        Assert.Contains(result.Errors, error => error.Contains("must match machineCode"));
    }
}
