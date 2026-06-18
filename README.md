# Riskily SME

Standalone, Kenya-localised Risk Intelligence product for small businesses. Sister product to corporate Riskily (`app.riskily.io`), built as a **fully separate stack** with its own database and its own copy of the PRISM scoring logic.

Canonical product docs (scope, scoring methodology, backlog, ADRs, data model) live in the Notion workspace **"Riskily SME — Product Workspace"**.

## Stack

- **Backend:** Kotlin + Spring Boot 3 + Maven + Spring Data JPA + Flyway + PostgreSQL (JDK 17+)
- **Frontend:** React + TypeScript + Vite + Tailwind CSS + shadcn/ui + TanStack Query
- **DevOps:** Docker Compose

## Quick start

```bash
# 1. Bring up Postgres + backend
docker compose up --build

# 2. Frontend (separate terminal)
cd frontend
npm install
npm run dev
```

Backend defaults to `http://localhost:8080`, frontend to `http://localhost:5173`.
Health check: `GET http://localhost:8080/api/health`.

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
