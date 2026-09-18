using System.Security.Cryptography;
using System.Text;

namespace PlantOS.TelemetryProcessor.Processing;

public static class DerivedEventId
{
    public static Guid Create(Guid sourceEventId, string discriminator)
    {
        var sourceBytes = sourceEventId.ToByteArray();
        var discriminatorBytes = Encoding.UTF8.GetBytes(discriminator);
        var input = new byte[sourceBytes.Length + discriminatorBytes.Length];

        sourceBytes.CopyTo(input, 0);
        discriminatorBytes.CopyTo(input, sourceBytes.Length);

        var hash = SHA256.HashData(input);
        return new Guid(hash.AsSpan(0, 16));
    }
}
