# PlantOS

PlantOS is a learning-first factory operations application. The current vertical slices combine an Angular machine registry, a Spring Boot REST API with PostgreSQL persistence, and .NET services that simulate and process machine telemetry through Kafka.

## Applications

- `frontend/` — Angular 17 interface for registering machines and managing their status.
- `backend/` — Spring Boot 4 API using Java 21, JPA/Hibernate, Flyway, and PostgreSQL.
- `telemetry/` — .NET 10 event contracts, machine simulator, telemetry processor, and automated tests.

The frontend calls the backend through `/api`. During local development, Angular proxies those requests to `http://localhost:8081`.

## Run locally

### Backend

Create a PostgreSQL database named `plantos`, provide the database user's password through `PLANTOS_DB_PASSWORD`, and then run:

```powershell
cd backend
$env:PLANTOS_DB_PASSWORD = "your-local-password"
.\mvnw.cmd spring-boot:run
```

The API starts at `http://localhost:8081`.

### Frontend

In another terminal:

```powershell
cd frontend
npm install
npm start
```

Open `http://localhost:4200`.

### Kafka and telemetry simulator

Kafka runs in Docker inside WSL, while the simulator runs from PowerShell on Windows. Start Kafka in its own PowerShell terminal and leave this command attached so WSL remains running:

```powershell
wsl sh -lc 'cd /mnt/d/own/plantos && sh scripts/start-kafka.sh'
```

After Kafka finishes starting, create the telemetry, health, and anomaly topics from a second terminal:

```powershell
wsl sh -lc 'cd /mnt/d/own/plantos && sh scripts/create-kafka-topics.sh'
```

Start a console consumer that prints the Kafka key, partition, offset, and JSON payload:

```powershell
wsl sh -lc 'cd /mnt/d/own/plantos && docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic plantos.machine.telemetry.v1 --formatter-property print.key=true --formatter-property print.partition=true --formatter-property print.offset=true --from-beginning'
```

Run the telemetry processor in another PowerShell terminal:

```powershell
dotnet run --project telemetry/src/PlantOS.TelemetryProcessor
```

Then run the simulator in another terminal:

```powershell
dotnet run --project telemetry/src/PlantOS.MachineSimulator
```

The fixed local machine profiles and event interval are configured in `telemetry/src/PlantOS.MachineSimulator/appsettings.json`. Health thresholds are configured in `telemetry/src/PlantOS.TelemetryProcessor/appsettings.json`. Stop each .NET worker with `Ctrl+C`; it closes or flushes its Kafka client before exiting.
Stop Kafka with `Ctrl+C` in its attached terminal.

## Verify

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm run build
.\node_modules\.bin\ng.cmd test --watch=false --browsers=ChromeHeadless

cd ..
dotnet test telemetry\PlantOS.Telemetry.sln
```

## Machine API

- `GET /api/machines`
- `POST /api/machines`
- `PATCH /api/machines/{code}/status`
- `DELETE /api/machines/{code}`
