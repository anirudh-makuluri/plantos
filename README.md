# PlantOS

PlantOS is a learning-first factory operations application. The current vertical slice combines an Angular machine registry with a Spring Boot REST API and PostgreSQL persistence.

## Applications

- `frontend/` — Angular 17 interface for registering machines and managing their status.
- `backend/` — Spring Boot 4 API using Java 21, JPA/Hibernate, Flyway, and PostgreSQL.

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

## Verify

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm run build
.\node_modules\.bin\ng.cmd test --watch=false --browsers=ChromeHeadless
```

## Machine API

- `GET /api/machines`
- `POST /api/machines`
- `PATCH /api/machines/{code}/status`
- `DELETE /api/machines/{code}`
