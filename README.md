# Riskily SME

Standalone, Kenya-localised Risk Intelligence product for small businesses. Sister product to corporate Riskily (`app.riskily.io`), built as a **fully separate stack** with its own database and its own copy of the PRISM scoring logic.

Canonical product docs (scope, scoring methodology, backlog, ADRs, data model) live in the Notion workspace **"Riskily SME — Product Workspace"**.

## Stack

- **Backend:** Kotlin + Spring Boot 3 + Maven + Spring Data JPA + Flyway + PostgreSQL (JDK 17+)
- **Frontend:** React + TypeScript + Vite + Tailwind CSS + shadcn/ui + TanStack Query
- **DevOps:** Docker Compose

## Quick start

The standard dev loop runs Postgres in Docker while you run the backend and frontend on your host:

```bash
# 1. Start Postgres on Docker (localhost:5433)
docker compose up db -d

# 2. Backend (separate terminal) — connects to the Dockerised DB
cd backend
mvn spring-boot:run

# 3. Frontend (separate terminal)
cd frontend
npm install
npm run dev
```

Prefer everything in Docker? Run the **entire stack** (Postgres DB, backend API, and compiled frontend proxy) together:

```bash
docker compose up --build -d
```

When running the full Docker stack:
- Frontend defaults to `http://localhost` (port 80) and proxies API requests.
- Backend API defaults to `http://localhost:8080`.
- Health check: `GET http://localhost:8080/api/health`.
Requires Docker, JDK 17+, Maven, and Node 20+.

## Repository layout

```
backend/    Spring Boot / Kotlin API (Maven)
frontend/   Vite / React / TypeScript SPA
docker-compose.yml
```

## Conventions

- UI design language: see `DESIGN.md` at the repo root (themed onto shadcn/ui).
- Migrations are **append-only** — never edit a shipped Flyway file; add `V{n+1}__description.sql`.
- All SME-facing strings come from the `frontend/src/lang` dictionaries — no hardcoded copy.
- Scoring constants live in the `scoring_config` table, never inlined — see the Scoring Methodology doc.
- Patterns mirror the parent Riskily product where known; assumed patterns are marked `TODO-confirm`.
