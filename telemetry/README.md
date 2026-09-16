# PlantOS raw telemetry

This solution implements Milestone 1 from the PlantOS PRD. It produces simulated raw machine readings; it does not calculate health, detect anomalies, persist readings, or expose a browser API.

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
