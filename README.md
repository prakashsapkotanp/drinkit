# Drinkin' — Project Scaffold

Kotlin end-to-end monorepo: Spring Boot backend + Kotlin Multiplatform shared module + Compose Android app + Compose Web (Wasm) app.

## Structure
```
backend/    Spring Boot Kotlin API (auth, users, posts, feed, social, engagement)
shared/     KMP module — data models + Ktor API client, shared by androidApp & webApp
androidApp/ Compose Android app
webApp/     Compose Multiplatform Web app (Kotlin/Wasm)
```

## Local development

1. Start Postgres:
   ```
   docker compose up -d
   ```

2. Run the backend (applies Flyway migrations automatically on startup):
   ```
   ./gradlew :backend:bootRun
   ```
   Backend runs on `http://localhost:8080`. Health check: `GET /actuator/health`.

3. Run the Android app: open in Android Studio, run `androidApp` configuration.
   (Emulator reaches host machine via `10.0.2.2`, already set in `MainActivity.kt`.)

4. Run the Web app:
   ```
   ./gradlew :webApp:wasmJsBrowserDevelopmentRun
   ```

## What's already wired up

- `POST /api/auth/register` and `POST /api/auth/login` — working end-to-end, including age-verification check (18+)
- JWT generation/validation (`JwtService`)
- Core Postgres schema via Flyway (`backend/src/main/resources/db/migration`)
- Shared data models + API client (`shared/src/commonMain`) ready to be called from both frontends
- CI pipeline building all four modules on every PR

## What's next (see `backlog/Phase0-Phase1-Tickets.md`)

- Wire Spring Security filter chain to validate JWT on protected endpoints
- Implement Post/Feed/Social/Engagement controllers (mirroring `AuthController` pattern)
- Build out Compose screens for Android (`androidApp`) and Web (`webApp`) — auth, feed, create-post
- Add `openapi.yaml` (in `api/`) to Swagger UI or Redoc for interactive API docs

## Reference docs

- `api/openapi.yaml` — full API contract
- `migrations/` — raw SQL (also copied into `backend/src/main/resources/db/migration`)
- `backlog/Phase0-Phase1-Tickets.md` — ticket-by-ticket breakdown for agents to pick up
