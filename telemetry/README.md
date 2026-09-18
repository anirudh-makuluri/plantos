# PlantOS raw telemetry

This solution implements the .NET portions of Milestones 1 and 2 from the PlantOS PRD. It produces simulated raw machine readings, validates and evaluates them, and publishes health and anomaly events. It does not persist readings or expose a browser API.

## Data flow

```text
appsettings.json machine profiles
              |
              v
MachineTelemetryGenerator
  sequence and measurements
              |
              v
TelemetryWorker (BackgroundService)
  one reading per machine per interval
              |
              v
KafkaTelemetryPublisher
  key: machineCode
  topic: plantos.machine.telemetry.v1
```

Kafka hashes each `machineCode` to a partition. A machine's readings therefore stay in one ordered partition even when different machines happen to share that partition. `sequenceNumber` makes that ordering visible during inspection.

## MachineTelemetryV1

Every JSON event contains:

| Field | Meaning |
| --- | --- |
| `eventId` | Unique ID for this telemetry reading. |
| `schemaVersion` | Contract version, currently `1`. |
| `eventType` | Stable event name, `MachineTelemetry`. |
| `occurredAt` | UTC time at which the simulated reading was taken. |
| `producedAt` | UTC time immediately before Kafka publication. |
| `machineCode` | Stable machine identifier and Kafka message key. |
| `correlationId` | ID shared by every reading in one simulator run. |
| `causationId` | Local run/start ID that caused the reading; a future simulation command ID will fill this role. |
| `sequenceNumber` | Per-machine sequence beginning at `1` for each run. |
| `temperatureCelsius` | Simulated temperature in degrees Celsius. |
| `vibrationMillimetersPerSecond` | Simulated vibration velocity in mm/s. |
| `powerKilowatts` | Simulated instantaneous power in kW. |
| `rpm` | Simulated rotations per minute. |
| `unitsProduced` | Per-machine cumulative units for the current run. |

The configured measurement values are deterministic demo data. They are not real equipment limits or industrial safety guidance.

## Lifecycle

The worker uses the .NET Generic Host and `BackgroundService`. On `Ctrl+C`, its cancellation token stops the generation loop, the Kafka producer flushes pending messages for up to the configured timeout, and the host disposes the producer.

Kafka producer idempotence and acknowledgements from all in-sync replicas are enabled. This protects ordering during retries at the producer boundary; downstream business idempotency remains a later Spring Boot milestone.

## Health processing

```text
plantos.machine.telemetry.v1
              |
              v
TelemetryProcessorWorker
  deserialize and validate
              |
              v
MachineHealthEvaluator
  status, score, violations
          ____|____
         |         |
         v         v
 health.v1      anomaly.v1
```

The processor validates contract version, event type, IDs, UTC timestamps, sequence, measurements, and that the Kafka key matches `machineCode`. Invalid records are logged and committed so one poison record cannot block its partition. A dead-letter topic remains part of later reliability hardening.

Valid records always produce one `MachineHealthV1`. Each temperature or vibration threshold violation also produces one `MachineAnomalyV1`. Derived event IDs are deterministic from the source telemetry ID, so replaying the same input produces the same output IDs.

### Demo thresholds

| Measurement | Warning | Critical |
| --- | ---: | ---: |
| Temperature | 75 C | 90 C |
| Vibration | 4 mm/s | 7 mm/s |

Values below warning thresholds have no penalty. A warning subtracts 20 to 40 points and a critical reading subtracts 60 to 80 points. Temperature and vibration penalties are added and the result is clamped to a `0` to `100` health score. Status is the highest active severity. These are configurable demonstration rules, not industrial safety guidance.

The consumer disables automatic offset commits. It publishes the derived health and anomaly events first, then commits the raw event offset. A crash between those operations can replay an input, which is why the derived event IDs are deterministic.
