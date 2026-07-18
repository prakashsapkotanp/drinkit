# AGENT.md — Drinkin' Project Scaffold Guide

This file orients an AI coding agent (e.g. Google Jules) working in this repo. Read this first before picking up any ticket.

---

## What this project is

**Drinkin'** — a LinkedIn-style social platform where users post about drink experiences (alcoholic + non-alcoholic: cocktails, beer, wine, coffee, tea, etc.), rate them, tag scenarios, and follow/engage with each other.

**Stack:** Kotlin end-to-end.
- Backend: Kotlin + Spring Boot
- Shared logic: Kotlin Multiplatform (KMP) module
- Android: Jetpack Compose (consumes shared module)
- Web: Compose Multiplatform for Web/Wasm (consumes shared module)

**Project owner:** Shareholder/PM role only — does not write code directly. All implementation is done by AI agents against tickets. Keep PRs scoped to one ticket at a time and self-contained.

---

## Repo layout

```
backend/     Spring Boot Kotlin API — auth, users, posts, feed, social, engagement
shared/      KMP module — data models + Ktor API client (source of truth, used by both frontends)
androidApp/  Compose Android app
webApp/      Compose Multiplatform Web app (Kotlin/Wasm)
migrations/  Raw copies of SQL migrations (also live in backend/src/main/resources/db/migration)
api/         openapi.yaml — the API contract; implement backend endpoints to match this exactly
backlog/     Phase0-Phase1-Tickets.md — full ticket list with acceptance criteria
```

---

## Golden rules for agents working in this repo

1. **`shared/` is the source of truth.** Any change to a data model or API contract must be made here first, then consumed by `backend`, `androidApp`, and `webApp`. Never duplicate a model definition in a frontend module.
2. **Match `api/openapi.yaml` exactly.** Endpoint paths, request/response shapes, and status codes in the backend must conform to this spec. If a ticket requires changing the contract, update `openapi.yaml` in the same PR.
3. **One ticket, one PR.** Reference the ticket ID (e.g. `DRK-103`) in the PR title and commit messages.
4. **Schema changes go through Flyway.** Never edit an existing migration file that's already been applied. Add a new `V{n}__description.sql` file in `backend/src/main/resources/db/migration/`.
5. **Follow the existing pattern.** `AuthController.kt` + `JwtService.kt` + `UserEntity.kt`/`UserRepository.kt` is the reference pattern for controller / entity / repository structure. Mirror it for Post, Comment, Follow, Like, Notification.
6. **Auth:** protected endpoints require a valid JWT (`Authorization: Bearer <token>`). Use `JwtService.validateAndGetUserId()` to resolve the current user — this still needs to be wired into a Spring Security filter chain (see Known Gaps below).
7. **Age/compliance logic stays server-side.** Do not remove or bypass the age-verification check in `AuthController.register()`.
8. **Tests:** every new endpoint needs at least one integration test (backend) before a PR is considered done, per DRK-114.

---

## Build & run commands

```bash
# start local Postgres
docker compose up -d

# run backend (applies Flyway migrations automatically)
./gradlew :backend:bootRun

# build shared module
./gradlew :shared:build

# build Android debug APK
./gradlew :androidApp:assembleDebug

# run Web app in dev mode
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# run full CI build locally
./gradlew build
```

Backend runs at `http://localhost:8080`. Health check: `GET /actuator/health`.

---

## Current state (what's already implemented)

- ✅ Monorepo/Gradle multiplatform setup (`backend`, `shared`, `androidApp`, `webApp`)
- ✅ Core Postgres schema (`V1__init_core_schema.sql`, `V2__post_counters_triggers.sql`)
- ✅ `POST /api/auth/register` and `POST /api/auth/login` — working, includes 18+ age check
- ✅ JWT generation (`JwtService`)
- ✅ Shared data models + Ktor API client (`shared/src/commonMain`)
- ✅ CI pipeline (GitHub Actions) building all 4 modules

## Known gaps (pick these up first — see backlog for full list)

- ❌ Spring Security filter chain doesn't yet enforce JWT on protected routes — auth token is generated but not validated on incoming requests. **Do this before anything else touches protected endpoints.**
- ❌ Post/Feed/Social/Engagement controllers not implemented (DRK-101 → DRK-107)
- ❌ Android and Web screens are placeholder stubs only (DRK-108 → DRK-113)
- ❌ No media upload service yet (Phase 2)
- ❌ No search/discovery (Phase 2)

---

## Ticket workflow

1. Open `backlog/Phase0-Phase1-Tickets.md`
2. Pick the next unblocked ticket in sprint order (see "Suggested Sprint Grouping" at the bottom of that file)
3. Implement against the acceptance criteria listed for that ticket
4. Confirm it matches `api/openapi.yaml` if it's an API-facing change
5. Add/update tests
6. Open PR referencing the ticket ID

---

## Reference docs in this repo

- `README.md` — setup instructions
- `api/openapi.yaml` — API contract
- `migrations/*.sql` — schema reference
- `backlog/Phase0-Phase1-Tickets.md` — full ticket backlog with acceptance criteria
