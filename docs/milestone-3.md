# Milestone 3: durable alert workflow

This slice turns a .NET anomaly into a durable Spring-owned incident. Read the steps in order; each introduces one responsibility.

```text
Controlled raw reading / .NET simulator
  -> Kafka: plantos.machine.telemetry.v1
  -> .NET telemetry processor
  -> Kafka: plantos.machine.anomaly.v1
  -> AnomalyListener -> AnomalyParser -> AlertService
  -> PostgreSQL: processed_events + alerts + alert_audit
  -> AlertController -> REST response
```

## 1. Agree on the event contract

Start with `telemetry/src/PlantOS.Telemetry.Contracts/MachineAnomalyV1.cs`, then compare it with `backend/src/main/java/com/example/plantos/backend/alert/MachineAnomalyV1.java`.

The Java record is an immutable data carrier, like a TypeScript object with a fixed set of properties. It mirrors the JSON names and enum values emitted by .NET, including `HIGH_TEMPERATURE`, `HIGH_VIBRATION`, `WARNING`, and `CRITICAL`.

`AnomalyParser` converts the string payload to that record and validates the version, required fields, identifiers, reading, unit, and Kafka key. Boxed numbers such as `Integer` let us distinguish a missing value (`null`) from an actual zero. The Kafka key must equal `machineCode` so one machine's messages use the same partition. Java validates again at the service boundary, protecting callers that do not use Kafka.

Machines must already be registered in Spring. An unknown machine goes to the dead-letter topic; register the machine and republish the original anomaly to retry it.

## 2. Separate events from incidents

Read `backend/src/main/resources/db/migration/V2__create_alert_workflow.sql`.

| Table | Responsibility |
| --- | --- |
| `processed_events` | Remembers each successfully handled event ID. |
| `alerts` | Summarizes an incident and its current workflow state. |
| `alert_audit` | Records creation, updates, acknowledgement, resolution, and ignored late events. |

There are two different uniqueness rules:

- **Same event again:** `processed_events.event_id` is a primary key. Replaying it does not change the occurrence count or add audit records.
- **New event, same unresolved fault:** a partial unique index permits only one non-resolved alert per `(machine_code, anomaly_type)`. New readings update that alert.

Flyway applies V2 once. Hibernate's `ddl-auto=validate` checks the entity against the schema; it does not create the tables. Alerts reference registered machines, so deleting a machine with retained alert history returns HTTP 409.

## 3. Make processing atomic

Read `AlertService.process`, then `AlertPersistence`.

`@Transactional` means that a single database transaction covers:

1. Claim the event ID with `INSERT ... ON CONFLICT DO NOTHING`.
2. Lock the machine row and find its active incident.
3. Create or update the alert.
4. Append its audit entry.
5. Commit everything together.

If the insert changes zero rows, the event was already handled and the method returns `DUPLICATE`. A separate `exists()` check followed by an insert would leave a race: two requests could both observe that the ID was absent. PostgreSQL resolves the conflict atomically instead.

If any later operation fails, the event claim rolls back too. Otherwise a retry could incorrectly skip an event whose alert was never saved. The integration test deliberately makes the audit write fail and verifies that retrying the same event works.

The machine row lock serializes incident creation and workflow commands for the same machine. We need a machine lock because the first event has no alert row to lock yet. The unique index is an additional database guarantee. Different machines can still be processed independently.

`AlertRepository` uses JPA/Hibernate for the alert entity. `AlertPersistence` uses small SQL operations where PostgreSQL's atomic insert and locking are useful. `JdbcTemplate` and Hibernate share the Spring-managed datasource transaction. Updating a managed entity is persisted by Hibernate dirty checking at commit.

## 4. Commit the database before advancing Kafka

Read `AnomalyListener`, `AlertKafkaConfiguration`, and `application.properties`.

`@KafkaListener` asks Spring to invoke the method for each record. The listener calls a **separate** `AlertService` bean; Spring's transaction proxy wraps that call and commits before returning. Calling a transactional method from another method on the same object would bypass that proxy.

Kafka auto-commit is disabled and the container uses `ack-mode=record`. The successful listener return lets Spring advance the offset after database work finishes.

There is still a possible crash window:

```text
Database commit succeeds -> process crashes -> Kafka offset was not committed
                                          -> same event is delivered again
                                          -> event ID already exists, so no repeated effects
```

This provides idempotent business effects with at-least-once delivery. Kafka and PostgreSQL are not one distributed transaction.

Invalid input goes directly to `plantos.machine.anomaly.v1.DLT`. Other failures get two retries, one second apart, and then go to that topic too. This includes a prolonged database outage: inspect and replay those records after fixing the cause. The dead-letter record retains the original key/value and failure metadata. If dead-letter publication fails, the error handler must not advance past the record.

Reference: [Spring Kafka offset commits](https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/message-listener-container.html) and [error handling](https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html).

## 5. Expose the workflow through REST

Read `AlertController` and `AlertResponse`. As with the machine API, the controller returns DTOs, not Hibernate entities.

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/api/alerts?machineCode=...&status=OPEN` | List/filter alerts; both filters are optional. Critical incidents sort first. |
| GET | `/api/machines/{code}/alerts` | Read one machine's alert history; optional `status` filter. |
| GET | `/api/alerts/{id}` | Read one alert. |
| GET | `/api/alerts/{id}/history` | Read its ordered audit entries. |
| POST | `/api/alerts/{id}/acknowledgements` | Move OPEN to ACKNOWLEDGED. No body required. |
| POST | `/api/alerts/{id}/resolve` | Resolve OPEN or ACKNOWLEDGED with `{"note":"Inspected simulated fault"}`. |

Repeated acknowledgement or resolution is harmless and adds no duplicate audit entry. A repeated resolution retains the original note. A resolved alert cannot be acknowledged (409). A missing alert returns 404; an invalid resolution note or status filter returns 400.

New readings preserve an acknowledgement. Alert severity retains the highest observed severity for that incident, while measurement fields contain the latest reading; an older event cannot overwrite a newer reading. A healthy reading does not automatically resolve an incident.

An unseen anomaly whose `occurredAt` is at or before the latest resolution is recorded as processed and audited as `STALE_IGNORED`. It does not reopen the incident. A genuinely later occurrence can create a new alert. This comparison assumes reasonably synchronized source and backend clocks.

Machine operating status stays independent: a RUNNING machine can have a CRITICAL alert. Health snapshots and live Angular updates belong to milestone 4. Maintenance tickets belong to milestone 5.

Authentication is milestone 7. Current workflow calls are unauthenticated and their audit actor is explicitly `local-api`, not a verified person's identity. Event actions use `telemetry-processor`.

## Run and verify locally

Use Java 21 and the existing PostgreSQL database/password. If your terminal still selects Java 17, refresh its user-level Java configuration:

```powershell
$env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:PLANTOS_DB_PASSWORD = [Environment]::GetEnvironmentVariable('PLANTOS_DB_PASSWORD', 'User')
```

From `D:\own\plantos`, use separate terminals:

1. Start Kafka and keep the terminal attached:

   ```powershell
   wsl sh -lc 'cd /mnt/d/own/plantos && sh scripts/start-kafka.sh'
   ```

2. Create all four topics, including the dead-letter topic, then start the processor:

   ```powershell
   wsl sh -lc 'cd /mnt/d/own/plantos && sh scripts/create-kafka-topics.sh'
   dotnet run --project telemetry/src/PlantOS.TelemetryProcessor
   ```

3. Enable the Spring consumer and start the backend:

   ```powershell
   $env:PLANTOS_KAFKA_ENABLED = 'true'
   cd backend
   .\mvnw.cmd spring-boot:run
   ```

4. Run the controlled demonstration from the repository root:

   ```powershell
   .\scripts\verify-alert-workflow.ps1
   ```

The script registers a unique `M3-DEMO-...` machine, publishes a controlled 95 C raw reading, replays the exact reading twice, and publishes a new 96 C reading. The .NET processor creates real derived events. The expected result is **one alert, two occurrences, and two event audit entries**. It then acknowledges and resolves the alert, bringing the total to four audit entries. A later event on the same partition acts as a marker proving the earlier duplicates were consumed.

This is simulated data. The script retains its named machine and resolved alert for inspection and prints the detail URL. It creates a new fixture on each run. It does not require the continuously running simulator.

Inspect the dead-letter topic when diagnosing malformed input or unknown machine codes:

```powershell
wsl sh -lc 'cd /mnt/d/own/plantos && docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic plantos.machine.anomaly.v1.DLT --from-beginning --formatter-property print.key=true --formatter-property print.headers=true'
```

Correct the cause and republish the original key/value to the anomaly topic. Its event ID is only claimed if processing commits successfully. Stop consumers and the backend with Ctrl+C, then stop Kafka in its terminal.

Kafka consumption is opt-in (`PLANTOS_KAFKA_ENABLED` defaults to false), so machine CRUD and database tests still work without a running broker. `PLANTOS_KAFKA_BOOTSTRAP_SERVERS` and `PLANTOS_KAFKA_GROUP_ID` can override the local connection and group.

Run automated checks:

```powershell
cd backend
.\mvnw.cmd test
cd ..
dotnet test telemetry/PlantOS.Telemetry.sln
```

Backend tests use the local PostgreSQL database. New workflow tests create unique `M3-TEST-...` machines and remove only their own rows. They cover contract validation, replay, concurrent events, rollback/retry, unknown machines, late events, recurrence, lifecycle APIs, and history retention. Kafka transport itself is exercised by the live demonstration above.

## Verification recorded on September 18, 2026

- Backend: 47 tests passed; .NET: 15 tests passed.
- Live controlled raw telemetry went through Kafka, the .NET processor, Spring, PostgreSQL, and the alert REST API. Four raw deliveries (two unique readings and two replays) produced one alert with two occurrences and two event audit entries. Acknowledgement and resolution brought the audit count to four.
- After restarting Spring and Kafka, replaying an original .NET anomaly preserved the resolved alert, its two occurrences, and all four audit entries.
- A malformed anomaly was retained in the dead-letter topic with its original key/value and exception metadata. The following record on the same partition was consumed successfully. Both populated anomaly partitions reached zero consumer lag.

The successful script fixture was `M3-DEMO-642c2eddfe8e`, alert ID `19` in this local database. IDs and measurements are local verification evidence, not production data or performance claims.
