using PlantOS.MachineSimulator;
using PlantOS.MachineSimulator.Configuration;
using PlantOS.MachineSimulator.Generation;
using PlantOS.MachineSimulator.Publishing;

var builder = Host.CreateApplicationBuilder(args);

builder.Services
    .AddOptions<KafkaOptions>()
    .Bind(builder.Configuration.GetSection(KafkaOptions.SectionName))
    .Validate(
        options => !string.IsNullOrWhiteSpace(options.BootstrapServers),
        "Kafka:BootstrapServers is required.")
    .Validate(
        options => !string.IsNullOrWhiteSpace(options.Topic),
        "Kafka:Topic is required.")
    .ValidateOnStart();

builder.Services
    .AddOptions<SimulatorOptions>()
    .Bind(builder.Configuration.GetSection(SimulatorOptions.SectionName))
    .Validate(
        options => options.IntervalMilliseconds > 0,
        "Simulator:IntervalMilliseconds must be greater than zero.")
    .Validate(
        options => options.Machines.Count > 0,
        "At least one simulated machine is required.")
    .Validate(
        options => options.Machines.All(machine =>
            !string.IsNullOrWhiteSpace(machine.MachineCode) &&
            machine.UnitsPerReading >= 0),
        "Every simulated machine needs a code and a non-negative UnitsPerReading value.")
    .Validate(
        options => options.Machines
            .Select(machine => machine.MachineCode)
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .Count() == options.Machines.Count,
        "Simulated machine codes must be unique.")
    .ValidateOnStart();

builder.Services.AddSingleton(TimeProvider.System);
builder.Services.AddSingleton<IMachineTelemetryGenerator, MachineTelemetryGenerator>();
builder.Services.AddSingleton<ITelemetryPublisher, KafkaTelemetryPublisher>();
builder.Services.AddHostedService<TelemetryWorker>();

var host = builder.Build();
await host.RunAsync();
