using PlantOS.TelemetryProcessor;
using PlantOS.TelemetryProcessor.Configuration;
using PlantOS.TelemetryProcessor.Health;
using PlantOS.TelemetryProcessor.Processing;
using PlantOS.TelemetryProcessor.Publishing;
using PlantOS.TelemetryProcessor.Validation;

var builder = Host.CreateApplicationBuilder(args);

builder.Services
    .AddOptions<KafkaProcessorOptions>()
    .Bind(builder.Configuration.GetSection(KafkaProcessorOptions.SectionName))
    .Validate(
        options => !string.IsNullOrWhiteSpace(options.BootstrapServers),
        "Kafka:BootstrapServers is required.")
    .Validate(
        options => !string.IsNullOrWhiteSpace(options.ConsumerGroupId),
        "Kafka:ConsumerGroupId is required.")
    .Validate(
        options => new[] { options.TelemetryTopic, options.HealthTopic, options.AnomalyTopic }
            .All(topic => !string.IsNullOrWhiteSpace(topic)),
        "All Kafka topics are required.")
    .ValidateOnStart();

builder.Services
    .AddOptions<HealthRuleOptions>()
    .Bind(builder.Configuration.GetSection(HealthRuleOptions.SectionName))
    .Validate(
        options => options.TemperatureWarningCelsius < options.TemperatureCriticalCelsius,
        "The temperature warning threshold must be below the critical threshold.")
    .Validate(
        options => options.VibrationWarningMillimetersPerSecond <
            options.VibrationCriticalMillimetersPerSecond,
        "The vibration warning threshold must be below the critical threshold.")
    .ValidateOnStart();

builder.Services.AddSingleton(TimeProvider.System);
builder.Services.AddSingleton<MachineTelemetryValidator>();
builder.Services.AddSingleton<MachineHealthEvaluator>();
builder.Services.AddSingleton<TelemetryMessageProcessor>();
builder.Services.AddSingleton<IDerivedEventPublisher, KafkaDerivedEventPublisher>();
builder.Services.AddHostedService<TelemetryProcessorWorker>();

var host = builder.Build();
await host.RunAsync();
