# PlantOS Product Requirements Document

**Product positioning:** A simulated manufacturing operations platform for monitoring machines, detecting equipment problems, and coordinating maintenance in real time.

**Working concept name:** FactoryPulse  
**Document status:** Initial product definition  
**Last updated:** September 11, 2026

## 1. Executive summary

PlantOS is a learning-first manufacturing operations application that behaves like a credible internal factory system rather than a collection of unrelated CRUD screens.

The product simulates factory machines that continuously emit operational telemetry. PlantOS processes that telemetry, calculates machine health, detects abnormal conditions, creates actionable alerts and maintenance tickets, and updates an Angular operations dashboard without requiring a page refresh.

The defining demonstration is an end-to-end fault workflow:

1. An operator injects an overheating fault into a simulated machine.
2. The .NET simulator begins emitting abnormal telemetry.
3. The .NET telemetry processor identifies the threshold violation and publishes a health or anomaly event through Kafka.
4. Spring Boot idempotently records the alert and maintenance workflow in PostgreSQL.
5. Angular receives the committed update from Spring Boot and immediately displays the machine's critical health, alert, and ticket.

PlantOS will be developed one small vertical slice at a time. Each slice must be explained, automatically tested, and manually verified before the next slice begins.

## 2. Problem statement

Factory operators and maintenance teams need a shared view of equipment health and production risk. Raw telemetry alone is not actionable: teams need abnormal readings converted into operational context, durable alerts, assigned work, and a traceable resolution history.

PlantOS demonstrates how an event-driven system can bridge that gap using realistic but fully simulated factory data.

## 3. Product principles

1. **Operationally credible:** Features should represent recognizable manufacturing workflows, not technology demonstrations disguised as product features.
2. **Learning first:** Implement one complete, verified workflow at a time instead of constructing the whole architecture upfront.
3. **One browser boundary:** Angular communicates only with Spring Boot through REST and server-pushed updates. It never connects directly to Kafka, PostgreSQL, or the .NET services.
4. **Clear ownership:** .NET owns simulation and telemetry calculations. Spring Boot owns users, machines, alerts, maintenance workflows, authorization, persistence, and browser-facing APIs.
5. **Truthful simulation:** The UI must clearly identify the environment as a simulation. PlantOS does not connect to or control real industrial equipment or PLCs.
6. **Measured complexity:** Redis, TimescaleDB, predictive models, and additional services are introduced only when a demonstrated requirement justifies them.

## 4. Goals

### 4.1 Product goals

- Give operators a real-time overview of machine operating state and health.
- Turn telemetry anomalies into durable, actionable alerts.
- Create and track maintenance work from detection through verification.
- Make production risk understandable through clear factory, line, and machine views.
- Provide a fault-injection experience that makes the complete event flow easy to demonstrate.

### 4.2 Engineering and learning goals

- Demonstrate Angular signals, RxJS, typed HTTP clients, routing, reactive forms, route guards, and reusable UI components.
- Demonstrate Spring Boot REST APIs, validation, JPA/Hibernate, Spring Security, Kafka consumption, transactions, and integration testing.
- Demonstrate .NET workers, `BackgroundService`, async processing, Kafka production and consumption, concurrency, and rule-based telemetry analysis.
- Practice event contracts, idempotency, ordering, retries, dead-letter handling, structured logging, and observable service boundaries.
- Preserve a browser-safe architecture with one authoritative backend API.

## 5. Non-goals

The first release will not:

- Control real machines, production lines, PLCs, or safety systems.
- Claim production-grade predictive maintenance or machine-learning capabilities.
- Support multiple tenants or multiple independent customer organizations.
- Retain every raw telemetry reading indefinitely.
- Require Redis, TimescaleDB, Kubernetes, or a cloud deployment.
- Model inventory, supply-chain planning, worker scheduling, or full ERP/MES behavior.
- Split every domain into its own microservice.

## 6. Users and roles

### 6.1 Operator

An operator monitors current factory conditions and responds to visible problems.

Key capabilities:

- View factory, line, and machine state.
- View live machine health and recent telemetry.
- Report a machine problem.
- Acknowledge an alert.

### 6.2 Maintenance engineer

A maintenance engineer investigates and resolves equipment problems.

Key capabilities:

- View and filter the maintenance queue.
- Assign a ticket to themselves or another engineer.
- Move a ticket through its supported lifecycle.
- Record findings, repairs, and verification notes.

### 6.3 Supervisor

A supervisor coordinates operations and monitors production risk.

Key capabilities:

- View factory-wide status and active critical alerts.
- Review machine and line health.
- Manage alert escalation and ticket assignment.
- View production progress when work orders are introduced.

### 6.4 Administrator

An administrator manages the simulated environment.

Key capabilities:

- Manage users and roles.
- Register factories, lines, and machines.
- Configure telemetry thresholds.
- Start, stop, and configure the simulator.
- Inject simulated faults.

## 7. Domain model

PlantOS must not combine physical operating state, calculated health, and workflow state into one status field.

### 7.1 Machine operating status

Describes what the machine is doing:

- `OFFLINE`
- `IDLE`
- `RUNNING`
- `FAULTED`
- `MAINTENANCE`

### 7.2 Machine health status

Describes the latest calculated condition of the machine:

- `UNKNOWN`
- `HEALTHY`
- `WARNING`
- `CRITICAL`

A machine may be `RUNNING` while its health is `CRITICAL`. A telemetry anomaly does not by itself prove that a machine has physically stopped.

### 7.3 Alert status

- `OPEN`
- `ACKNOWLEDGED`
- `RESOLVED`

### 7.4 Maintenance ticket status

- `OPEN`
- `ASSIGNED`
- `IN_PROGRESS`
- `RESOLVED`
- `VERIFIED`

### 7.5 Primary entities

- Factory
- Production line
- Machine
- Machine health snapshot
- Alert
- Alert acknowledgement
- Maintenance ticket
- Maintenance log
- User
- Role
- Audit entry
- Processed event

Work orders, production runs, products, and maintenance schedules are post-MVP entities.

## 8. Current baseline

The repository already contains the first completed vertical slice:

- An Angular 17 machine registry.
- A Spring Boot 4 REST API running on Java 21.
- PostgreSQL persistence through JPA/Hibernate and Flyway.
- Machine creation, listing, status updates, and deletion.
- DTO-based API contracts and automated backend tests.

Existing machine endpoints:

- `GET /api/machines`
- `POST /api/machines`
- `PATCH /api/machines/{code}/status`
- `DELETE /api/machines/{code}`

Future work must extend this baseline rather than replace it with a parallel implementation.

## 9. MVP scope

The MVP is the smallest release that demonstrates the complete fault-to-maintenance workflow.

### 9.1 Factory and machine registry

- Administrators can create and view machines.
- Every machine belongs to a production line.
- Every production line belongs to one simulated factory.
- Machine codes are unique and remain stable event identifiers.
- The UI shows operating status and health status separately.

### 9.2 Machine simulator

- A .NET worker simulates configurable machines at a configurable event frequency.
- The default local environment starts with a small dataset suitable for manual inspection.
- The simulator can scale to hundreds of machines for load experiments without changing application code.
- Generated telemetry includes event ID, machine code, event time, temperature, vibration, power consumption, RPM, and cumulative units produced.
- The simulator can inject at least overheating and excessive-vibration faults.
- Simulation controls are explicitly labeled and cannot interact with real equipment.

### 9.3 Telemetry processing

- A .NET telemetry processor consumes raw readings from Kafka.
- It validates required fields and rejects malformed or unsupported event versions.
- It calculates a deterministic health score and health status using documented rules.
- It detects threshold violations and publishes anomaly events.
- It emits machine health updates for live presentation.
- Rule-based detection is sufficient for the MVP; no ML model is required.

Initial example rules:

- Temperature above the configured warning threshold produces `WARNING` health.
- Temperature above the configured critical threshold produces `CRITICAL` health.
- Excessive vibration can independently produce `WARNING` or `CRITICAL` health.
- Multiple critical measurements may increase severity but must not create duplicate active incidents.

Final threshold values will be configurable test/demo values rather than claims about safe real-world equipment limits.

### 9.4 Alerts

- Spring Boot consumes anomaly events from Kafka.
- A valid anomaly creates or updates an active alert for the machine and fault type.
- Duplicate delivery of the same event must not create duplicate alerts.
- An unresolved alert for the same machine and fault type is updated instead of producing alert spam.
- Operators can acknowledge alerts.
- Authorized users can resolve alerts with a note.
- Alert creation, acknowledgement, and resolution are auditable.

### 9.5 Maintenance tickets

- A critical anomaly can create one maintenance ticket according to a documented business rule.
- The event record, alert, ticket, and applicable machine-state update are committed atomically.
- Duplicate or replayed events cannot create duplicate tickets.
- Engineers can assign, start, resolve, and verify a ticket.
- Invalid status transitions are rejected by the backend.
- Each transition records the actor, time, and optional note.

### 9.6 Operations dashboard

The Angular dashboard displays:

- Overall simulated factory health.
- Counts of running, idle, offline, faulted, and maintenance machines.
- Counts of healthy, warning, critical, and unknown machine health states.
- Production-line summaries.
- Active alerts ordered by severity and detection time.
- Maintenance queue summary.

### 9.7 Machine detail

The machine detail page displays:

- Identity and production-line membership.
- Operating status and calculated health status.
- Latest temperature, vibration, power, RPM, and production count.
- A bounded rolling telemetry chart for the current session.
- Active and recent alerts.
- Open maintenance tickets.
- Last recorded maintenance activity when available.

### 9.8 Live updates

- Spring Boot is the only browser-facing real-time provider.
- Spring consumes health and anomaly events, maintains the browser-facing projection, and pushes updates to Angular.
- Server-Sent Events are the preferred MVP transport because updates are server-to-client; WebSockets may replace them if bidirectional real-time behavior becomes necessary.
- Angular reconnects after a temporary connection loss and refreshes its snapshot through REST.
- The UI distinguishes disconnected/stale telemetry from healthy telemetry.

### 9.9 Simulation control panel

The Angular application provides an administrator-only simulation panel with:

- Start and stop controls.
- Machine count.
- Telemetry frequency.
- Configurable fault rate.
- Targeted machine and fault selection.
- An inject-fault action.

Angular submits commands to Spring Boot. Spring publishes versioned simulator command events through Kafka. Angular does not call the simulator directly.

## 10. Primary user journeys

### 10.1 Observe factory health

1. An operator opens the dashboard.
2. Angular loads an authoritative snapshot from Spring Boot.
3. The operator sees machines grouped by production line and health.
4. Live updates change health indicators without a page refresh.
5. If the live connection becomes stale, the UI clearly reports that condition.

### 10.2 Detect and investigate a machine anomaly

1. The simulator emits abnormal telemetry for a machine.
2. The telemetry processor calculates critical health and publishes an anomaly.
3. Spring Boot persists or updates the active alert idempotently.
4. Spring pushes the committed result to Angular.
5. The operator opens the affected machine and inspects recent readings and the alert.

### 10.3 Resolve a maintenance incident

1. A critical alert creates one maintenance ticket.
2. A maintenance engineer accepts the ticket.
3. The engineer moves it to `IN_PROGRESS` and records findings.
4. The engineer resolves the ticket with repair notes.
5. A supervisor or authorized engineer verifies the repair.
6. PlantOS retains the complete status history and audit trail.

### 10.4 Demonstrate fault injection

1. An administrator selects a machine and an overheating fault.
2. Spring sends the command to the simulator through Kafka.
3. The machine's telemetry graph rises visibly.
4. Health changes to `CRITICAL`.
5. An alert and one maintenance ticket appear.
6. Replaying the same anomaly does not create duplicates.

## 11. System architecture

```text
                          PostgreSQL
                              ^
                              |
Angular <--- REST + SSE ---> Spring Boot
                              |
             publish commands | consume health and anomalies
                              v
                            Kafka
                 _____________|_____________
                |                           |
 .NET Machine Simulator          .NET Telemetry Processor
 consumes simulation commands    consumes raw telemetry
 publishes raw telemetry         publishes health and anomalies
```

### 11.1 Service ownership

#### Angular

- User interface and client-side presentation state.
- REST snapshot loading.
- Live event subscription and reconnection.
- No business-rule authority and no infrastructure credentials.

#### Spring Boot

- Authentication and authorization.
- Machine and factory registry.
- Alert and maintenance workflows.
- Persistent business state and audit trail.
- Idempotent Kafka event handling.
- Browser-facing REST and SSE interfaces.
- Simulation command API.

#### .NET machine simulator

- Deterministic and configurable fake-machine behavior.
- Raw telemetry publication.
- Controlled fault injection.
- No ownership of business alerts or maintenance tickets.

#### .NET telemetry processor

- Telemetry validation.
- Rolling calculations and deterministic health scoring.
- Threshold and trend evaluation.
- Health and anomaly event publication.
- No browser API and no ownership of business workflows.

## 12. Event contracts

All events must include:

- `eventId`: globally unique identifier.
- `schemaVersion`: explicit contract version.
- `eventType`: stable event name.
- `occurredAt`: source event time in UTC.
- `producedAt`: publication time in UTC.
- `machineCode`: stable machine identifier when applicable.
- `correlationId`: identifier connecting a simulation command, telemetry, anomaly, alert, and ticket.
- `causationId`: identifier of the event or command that directly caused the current event.

### 12.1 MVP Kafka topics

- `plantos.machine.telemetry.v1`
- `plantos.machine.health.v1`
- `plantos.machine.anomaly.v1`
- `plantos.simulation.commands.v1`

Machine-related events must use `machineCode` as the Kafka message key so events for one machine remain ordered within a partition.

### 12.2 Idempotency requirement

Spring Boot must process each anomaly event at most once from the perspective of business side effects. Within one database transaction it must:

1. Insert the event ID into `processed_events`, protected by a unique constraint.
2. Create or update the relevant alert.
3. Create a maintenance ticket if the rule requires one and no active matching ticket exists.
4. Record audit entries.
5. Commit all changes together.

If the event ID already exists, processing succeeds without duplicating side effects.

## 13. Data strategy

### 13.1 PostgreSQL

PostgreSQL stores durable business state:

- Factories, lines, and machines.
- Users and roles.
- Current persisted machine metadata.
- Alerts and acknowledgements.
- Maintenance tickets and logs.
- Audit entries.
- Processed event IDs.

### 13.2 Telemetry

The MVP does not persist every raw reading indefinitely.

- The telemetry processor calculates a bounded rolling window.
- Spring keeps the latest browser-facing health projection.
- Angular may display a bounded current-session chart.
- Important readings attached to an anomaly are retained with the alert or ticket.

Historical telemetry storage is considered only after retention and query requirements are defined. TimescaleDB is a possible later option, not an MVP dependency.

### 13.3 Cache

Redis is not required for the MVP. It may be added later if measured read volume, cross-instance live-state sharing, or scaling requirements justify it.

## 14. API requirements

In addition to the existing machine API, the MVP will eventually require resources such as:

- `GET /api/dashboard`
- `GET /api/machines/{code}`
- `GET /api/machines/{code}/alerts`
- `GET /api/alerts`
- `POST /api/alerts/{id}/acknowledgements`
- `POST /api/alerts/{id}/resolve`
- `GET /api/maintenance-tickets`
- `PATCH /api/maintenance-tickets/{id}/status`
- `POST /api/simulation/start`
- `POST /api/simulation/stop`
- `POST /api/simulation/faults`
- `GET /api/events/stream`

Exact request and response DTOs will be specified within the vertical slice that introduces each endpoint. Entities must not be serialized directly as public API contracts.

## 15. Authorization requirements

- All operational data requires an authenticated user once authentication is introduced.
- Backend authorization is authoritative; hiding a button in Angular is not access control.
- Operators can view machines and acknowledge alerts.
- Maintenance engineers can update assigned maintenance tickets.
- Supervisors can assign work and resolve escalated alerts.
- Administrators can manage configuration and simulation controls.
- Every privileged state change records the authenticated actor.

Authentication and RBAC are introduced after the first complete telemetry-to-alert slice so security work does not block validation of the core event flow.

## 16. Non-functional requirements

### 16.1 Reliability

- Kafka consumers must tolerate duplicate delivery.
- Invalid messages must not crash the consumer loop.
- Retryable and non-retryable failures must be distinguishable.
- Poison messages must eventually move to a dead-letter path with enough context to diagnose them.
- Restarting a consumer must not create duplicate alerts or tickets.

### 16.2 Performance targets

For the local portfolio environment:

- Support 500 simulated machines producing one reading per second during a load demonstration.
- Show a committed critical alert in Angular within two seconds of the corresponding anomaly event under normal local conditions.
- Load the default dashboard snapshot within two seconds on the documented development machine.

These are project targets and must be measured before being presented as achieved results.

### 16.3 Observability

- Every service emits structured logs.
- Logs include event ID, correlation ID, machine code, and service name where relevant.
- Each service exposes a health endpoint or equivalent health signal.
- Metrics include processed-event count, rejected-event count, consumer lag, anomaly count, and event-processing latency.
- Logs and UI labels must distinguish simulated data from production data.

### 16.4 Security

- Credentials and connection strings are supplied through environment variables or local secret management, never committed source files.
- Angular contains no Kafka, database, or service credentials.
- Spring validates every state-changing request.
- Simulator control endpoints require administrator authorization when RBAC is present.
- Production-like safety language must not imply that PlantOS is certified to control real machinery.

### 16.5 Accessibility and responsive behavior

- Health and alert severity cannot be communicated by color alone.
- Core dashboard and maintenance actions are keyboard accessible.
- Tables and charts provide meaningful text labels.
- The primary dashboard remains usable on tablet-sized screens; desktop is the main operations target.

## 17. Delivery plan

Each milestone is a separately demonstrable vertical slice. A milestone is complete only when its data flow is documented, automated tests pass, and the behavior is manually verified.

### Milestone 0: Machine registry — complete baseline

- Angular machine list and controls.
- Spring Boot machine API.
- PostgreSQL persistence and Flyway migration.
- Backend unit, controller, and integration tests.

### Milestone 1: Raw telemetry

- Define `MachineTelemetryV1`.
- Add Kafka to the local development environment.
- Create a .NET simulator for a small fixed machine set.
- Publish and inspect valid telemetry events.
- Verify ordering keys, event timestamps, and graceful shutdown.

### Milestone 2: Health calculation

- Create the .NET telemetry processor.
- Validate incoming contracts.
- Calculate deterministic health status.
- Publish health and anomaly events.
- Unit-test boundary values and malformed events.

### Milestone 3: Durable alert workflow

- Add Spring Kafka consumption.
- Add alert, processed-event, and audit persistence.
- Implement transactional idempotency.
- Expose alert REST endpoints.
- Prove duplicate replay produces one business result.

### Milestone 4: Live Angular operations view

- Add Spring SSE streaming.
- Add dashboard snapshot endpoint.
- Show machine operating and health states separately.
- Add live telemetry and alert updates.
- Verify disconnect, reconnect, and stale-state behavior.

### Milestone 5: Maintenance workflow

- Create maintenance tickets for qualifying critical alerts.
- Implement ticket lifecycle rules.
- Add queue and ticket detail screens.
- Add audit history and transition tests.

### Milestone 6: Fault-injection demo

- Add simulation command events.
- Add administrator control panel.
- Implement overheating and vibration injection.
- Verify the complete command-to-ticket demonstration.

### Milestone 7: Authentication and roles

- Add Spring Security authentication.
- Add operator, engineer, supervisor, and administrator authorization.
- Add Angular guards and authenticated HTTP behavior.
- Verify backend denial paths independently of the UI.

## 18. MVP acceptance criteria

The MVP is complete when all of the following are demonstrated locally:

1. A user can view registered machines and their separate operating and health states.
2. The simulator publishes valid telemetry for registered machines through Kafka.
3. The telemetry processor deterministically identifies an injected critical fault.
4. Spring Boot persists one alert and one qualifying maintenance ticket.
5. Replaying the same event does not create duplicate business records.
6. Angular displays the committed critical state and ticket without a page refresh.
7. A maintenance engineer can move the ticket through the valid lifecycle.
8. The fault-injection workflow can be demonstrated end to end in under two minutes.
9. Automated tests cover event validation, threshold boundaries, idempotency, API behavior, and maintenance transitions.
10. A documented manual verification proves the complete data flow.

## 19. Post-MVP roadmap

### Release 2: Production operations

- Work orders and production runs.
- Production targets and current/required rates.
- Line-level progress and at-risk completion estimates.
- Preventive maintenance based on operating hours.
- Downtime reason codes.
- Audit-log viewer.
- One carefully defined manufacturing KPI, such as OEE, with transparent calculation inputs.

### Release 3: Historical analytics

- Defined telemetry-retention policy.
- Historical telemetry storage if justified.
- Machine trends and comparative line analytics.
- MTBF and MTTR reporting.
- Energy-consumption reporting.

### Release 4: Advanced experimentation

- Trend-based maintenance risk.
- Explainable failure-risk experiments.
- Multiple simulated factories.
- Production optimization experiments.
- Cloud deployment and scaling evaluation.

Advanced analytics must remain clearly labeled as experimental unless their accuracy has been measured against an appropriate dataset.

## 20. Risks and mitigations

### Scope expansion

**Risk:** Building authentication, Kafka, telemetry, maintenance, analytics, and deployment simultaneously prevents any workflow from becoming complete.  
**Mitigation:** Enforce milestone gates and do not begin the next milestone until the current one is tested and manually verified.

### Conflicting sources of truth

**Risk:** .NET and Spring independently decide machine status, causing contradictory UI state.  
**Mitigation:** .NET publishes calculated observations; Spring owns persistent operational state and the browser-facing projection.

### Duplicate business effects

**Risk:** Kafka replay creates duplicate alerts or tickets.  
**Mitigation:** Use unique event IDs, transactional processed-event records, and an active-incident uniqueness rule.

### Misleading manufacturing claims

**Risk:** Arbitrary thresholds or generated data are presented as real industrial safety knowledge.  
**Mitigation:** Label all data as simulated and describe thresholds as configurable demo rules.

### Premature infrastructure

**Risk:** Redis, time-series storage, Kubernetes, or cloud deployment consume time without improving the core demonstration.  
**Mitigation:** Add infrastructure only after a documented requirement and measurement justify it.

## 21. Open product decisions

- Whether the external portfolio name remains PlantOS or becomes FactoryPulse.
- Which initial simulated machine types best support credible telemetry ranges.
- Whether critical alerts always create tickets or use configurable alert-to-ticket rules.
- How long recent telemetry should remain visible before historical storage is introduced.
- Which authentication mechanism best fits the later security milestone.

These decisions do not block Milestone 1. They should be resolved only when their corresponding vertical slice begins.
