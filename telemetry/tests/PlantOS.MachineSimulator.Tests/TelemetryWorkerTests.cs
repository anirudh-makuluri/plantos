using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;
using PlantOS.MachineSimulator.Configuration;
using PlantOS.MachineSimulator.Generation;
using PlantOS.MachineSimulator.Publishing;
using PlantOS.Telemetry.Contracts;

namespace PlantOS.MachineSimulator.Tests;

public sealed class TelemetryWorkerTests
{
    [Fact]
    public async Task StopAsync_CancelsTheLoopAndFlushesQueuedMessages()
    {
        var machine = new MachineProfile
        {
            MachineCode = "PRESS-001",
            UnitsPerReading = 1
        };
        var simulatorOptions = Options.Create(new SimulatorOptions
        {
            IntervalMilliseconds = 10_000,
            Machines = [machine]
        });
        var publisher = new RecordingPublisher();
        var worker = new TelemetryWorker(
            new StubGenerator(),
            publisher,
            simulatorOptions,
            Options.Create(new KafkaOptions { FlushTimeoutSeconds = 1 }),
            NullLogger<TelemetryWorker>.Instance);

        await worker.StartAsync(CancellationToken.None);
        await publisher.FirstPublish.Task.WaitAsync(TimeSpan.FromSeconds(2));
        await worker.StopAsync(CancellationToken.None);

        Assert.Equal("PRESS-001", Assert.Single(publisher.Published).MachineCode);
        Assert.True(publisher.FlushCalled);
    }

    private sealed class StubGenerator : IMachineTelemetryGenerator
    {
        public MachineTelemetryV1 Next(MachineProfile machine, Guid simulationRunId)
        {
            var timestamp = DateTimeOffset.UtcNow;
            return new MachineTelemetryV1
            {
                EventId = Guid.NewGuid(),
                OccurredAt = timestamp,
                ProducedAt = timestamp,
                MachineCode = machine.MachineCode,
                CorrelationId = simulationRunId,
                CausationId = simulationRunId,
                SequenceNumber = 1,
                TemperatureCelsius = 60,
                VibrationMillimetersPerSecond = 2,
                PowerKilowatts = 18,
                Rpm = 950,
                UnitsProduced = 1
            };
        }
    }

    private sealed class RecordingPublisher : ITelemetryPublisher
    {
        public TaskCompletionSource FirstPublish { get; } =
            new(TaskCreationOptions.RunContinuationsAsynchronously);

        public List<MachineTelemetryV1> Published { get; } = [];
        public bool FlushCalled { get; private set; }

        public Task PublishAsync(MachineTelemetryV1 telemetry, CancellationToken cancellationToken)
        {
            Published.Add(telemetry);
            FirstPublish.TrySetResult();
            return Task.CompletedTask;
        }

        public int Flush(TimeSpan timeout)
        {
            FlushCalled = true;
            return 0;
        }
    }
}
