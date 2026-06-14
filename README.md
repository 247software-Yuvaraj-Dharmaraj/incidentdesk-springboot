# IncidentDesk — Spring Boot backend

A **Java / Spring Boot** reimplementation of the [IncidentDesk](https://github.com/247software-Yuvaraj-Dharmaraj/incidentdesk) backend (originally Node + Express + Prisma). It preserves the same REST API contract, so the existing React frontend works unchanged.

> **Live demo:** https://incidentdesk-java.vercel.app — the React frontend running entirely on this Spring Boot backend (Render + Neon Postgres)
> **Live API:** https://incidentdesk-springboot.onrender.com/api · health check: [`/api/health`](https://incidentdesk-springboot.onrender.com/api/health)
> **Demo logins:** `admin@incidentdesk.dev` / `Admin123!` · `reporter@incidentdesk.dev` / `Reporter123!`
>
> _Hosted on a free tier that sleeps when idle — the first request after a pause may take ~30–60s to wake._

## Tech stack

- **Java 21**, **Spring Boot 4**
- **Spring Web** (REST), **Spring Security** — JWT in an httpOnly cookie + role-based access control
- **Spring Data JPA / Hibernate** + **PostgreSQL**
- **Flyway** schema migrations (+ demo data seeded on first run)
- **netty-socketio** for realtime updates, **Gemini** for AI triage
- **JUnit** tests, Maven wrapper, Dockerfile

## Feature parity with the Node backend

- Auth: signup / login / logout / me — JWT stored in an httpOnly cookie
- RBAC (ADMIN / REPORTER); reporters only ever see their own incidents
- Incidents CRUD, filters + **cursor pagination**, **optimistic concurrency** (`expectedUpdatedAt`)
- Status-transition rules; an **audit log row per changed field**, written in a transaction
- Comments, with internal notes gated to admins
- Dashboard **stats** + 14-day **metrics** (MTTR, created/resolved trend)
- Bulk update / delete (admin)
- **AI triage** via Gemini (optional — set `GEMINI_API_KEY`)
- **Realtime** `incidents:changed` broadcasts via Socket.IO

## API

```
POST   /api/auth/signup | /login | /logout      GET /api/auth/me
GET    /api/incidents            (filters + cursor pagination)
GET    /api/incidents/stats | /metrics
GET    /api/incidents/triage/status     POST /api/incidents/triage
POST   /api/incidents            (create)
POST   /api/incidents/bulk-update | /bulk-delete   (ADMIN)
GET    /api/incidents/{id}        PATCH /{id} (ADMIN)   DELETE /{id} (ADMIN)
GET    /api/incidents/{id}/comments     POST /api/incidents/{id}/comments
GET    /api/users                (ADMIN)
GET    /api/health
```

## Run locally

1. Copy `.env.example` → `.env` and set a PostgreSQL `DATABASE_URL` (JDBC form) + `JWT_SECRET`.
2. Start it:
   ```bash
   ./mvnw spring-boot:run
   ```
   API → `http://localhost:4000`, Socket.IO → `:9092`. Flyway creates the schema and demo data is seeded on first run.
3. Demo logins:
   - `admin@incidentdesk.dev` / `Admin123!`
   - `reporter@incidentdesk.dev` / `Reporter123!`

## Build & test

```bash
./mvnw test
./mvnw clean package
```

## Docker

```bash
docker build -t incidentdesk-springboot .
docker run -p 4000:4000 -p 9092:9092 --env-file .env incidentdesk-springboot
```

## Using with the React frontend

Point the client's API base URL at `http://localhost:4000`. The realtime Socket.IO server runs on `SOCKET_PORT` (default `9092`) — set the client's socket URL accordingly.
