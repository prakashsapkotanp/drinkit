# Drinkin' — Development Ticket Backlog

**Scope:** Phase 0 (Foundation) + Phase 1 (MVP Core)
**Format:** Each ticket has an ID, title, description, acceptance criteria, and suggested labels — ready to paste into GitHub Issues, Jira, or Linear.

---

## PHASE 0 — FOUNDATION

### DRK-001: Initialize monorepo structure
**Description:** Set up the base Gradle multiplatform monorepo with `backend/`, `shared/`, `androidApp/`, `webApp/` modules.
**Acceptance criteria:**
- [ ] `settings.gradle.kts` includes all 4 modules
- [ ] Each module builds successfully with an empty/hello-world entry point
- [ ] `.gitignore` covers Gradle, IDE, and build artifacts
  **Labels:** `setup`, `infra`

---

### DRK-002: Configure CI pipeline (GitHub Actions)
**Description:** Add a workflow that builds backend + shared + androidApp + webApp on every PR, and runs tests.
**Acceptance criteria:**
- [ ] `.github/workflows/ci.yml` builds all modules
- [ ] Unit tests run and fail the build on failure
- [ ] Build status badge added to README
  **Labels:** `setup`, `ci/cd`

---

### DRK-003: Backend skeleton — Spring Boot Kotlin app
**Description:** Create the base Spring Boot application with health-check endpoint.
**Acceptance criteria:**
- [ ] `GET /actuator/health` returns 200
- [ ] Application starts locally via `./gradlew bootRun`
- [ ] Dockerfile builds a runnable container image
  **Labels:** `backend`, `setup`

---

### DRK-004: PostgreSQL setup + Flyway migrations
**Description:** Wire up PostgreSQL connection and Flyway migration tooling.
**Acceptance criteria:**
- [ ] `application.yml` supports local + prod DB config via env vars
- [ ] Flyway runs on startup and applies baseline migration
- [ ] `docker-compose.yml` provided for local Postgres
  **Labels:** `backend`, `database`

---

### DRK-005: Core database schema (users, posts, follows, likes, comments)
**Description:** Implement the initial schema as Flyway migrations (see schema doc).
**Acceptance criteria:**
- [ ] All core tables created with proper FKs and indexes
- [ ] Migration runs cleanly on empty DB
  **Labels:** `backend`, `database`

---

### DRK-006: Shared KMP module — core data models
**Description:** Define shared Kotlin data classes for User, Post, Comment, Like, Follow, Notification in `shared/src/commonMain`.
**Acceptance criteria:**
- [ ] Models are `@Serializable` (kotlinx.serialization)
- [ ] Models compile for JVM, Android, and Wasm targets
  **Labels:** `shared`, `setup`

---

### DRK-007: Shared KMP module — API client (Ktor)
**Description:** Set up a shared Ktor HTTP client wrapper for calling the backend from Android and Web.
**Acceptance criteria:**
- [ ] Base client with configurable base URL, JSON serialization, auth header injection
- [ ] Works from `androidMain` and `wasmJsMain` source sets
  **Labels:** `shared`, `networking`

---

### DRK-008: Authentication — JWT issuing & validation (backend)
**Description:** Implement Spring Security + JWT-based auth (register/login endpoints).
**Acceptance criteria:**
- [ ] `POST /api/auth/register` creates a user, returns JWT
- [ ] `POST /api/auth/login` validates credentials, returns JWT
- [ ] Protected endpoints reject requests without a valid JWT
  **Labels:** `backend`, `auth`, `security`

---

## PHASE 1 — MVP CORE

### DRK-101: User profile endpoints
**Description:** CRUD endpoints for user profile (bio, display name, avatar, drink preferences).
**Acceptance criteria:**
- [ ] `GET /api/users/{id}` returns public profile
- [ ] `PUT /api/users/me` updates own profile
- [ ] Validation on field lengths and allowed drink-preference values
  **Labels:** `backend`, `user`

---

### DRK-102: Age verification at signup
**Description:** Add mandatory date-of-birth field at registration; block access to alcohol-tagged content for underage accounts.
**Acceptance criteria:**
- [ ] Registration rejects users under regional legal drinking age
- [ ] `ageVerified` flag stored and enforced on relevant endpoints
  **Labels:** `backend`, `compliance`

---

### DRK-103: Create post endpoint
**Description:** `POST /api/posts` — create a post with text, drink category/type, optional rating, optional scenario tag.
**Acceptance criteria:**
- [ ] Validates required fields (text, drinkCategory)
- [ ] Returns created post with generated ID and timestamp
  **Labels:** `backend`, `post`

---

### DRK-104: Feed endpoint (reverse-chronological)
**Description:** `GET /api/feed` — returns posts from followed users, paginated, newest first.
**Acceptance criteria:**
- [ ] Cursor-based pagination
- [ ] Returns empty feed gracefully for new users (with suggestion to follow people)
  **Labels:** `backend`, `feed`

---

### DRK-105: Follow / unfollow endpoints
**Description:** `POST /api/users/{id}/follow`, `DELETE /api/users/{id}/follow`.
**Acceptance criteria:**
- [ ] Prevents duplicate follow entries
- [ ] Prevents self-follow
- [ ] Returns updated follower/following counts
  **Labels:** `backend`, `social-graph`

---

### DRK-106: Like endpoint
**Description:** `POST /api/posts/{id}/like`, `DELETE /api/posts/{id}/like`.
**Acceptance criteria:**
- [ ] Idempotent (double-like doesn't double count)
- [ ] Updates post's `likeCount`
  **Labels:** `backend`, `engagement`

---

### DRK-107: Comment endpoints
**Description:** `POST /api/posts/{id}/comments`, `GET /api/posts/{id}/comments`.
**Acceptance criteria:**
- [ ] Comments paginated
- [ ] Comment text length validated
  **Labels:** `backend`, `engagement`

---

### DRK-108: Android app — Auth screens
**Description:** Compose UI for register/login using shared KMP API client.
**Acceptance criteria:**
- [ ] Form validation matches backend rules
- [ ] JWT stored securely (EncryptedSharedPreferences)
- [ ] Error states handled (invalid credentials, network failure)
  **Labels:** `android`, `auth`

---

### DRK-109: Android app — Feed screen
**Description:** Compose UI displaying paginated feed with infinite scroll.
**Acceptance criteria:**
- [ ] Feed loads and paginates correctly
- [ ] Loading/error/empty states handled
- [ ] Like button reflects optimistic UI update
  **Labels:** `android`, `feed`

---

### DRK-110: Android app — Create post screen
**Description:** Compose UI for composing a new post (text, drink category picker, optional rating).
**Acceptance criteria:**
- [ ] Form matches backend validation
- [ ] Successful post navigates back to feed with new post visible
  **Labels:** `android`, `post`

---

### DRK-111: Web app — Auth screens (Compose/Wasm)
**Description:** Mirror of DRK-108 for Web target, reusing shared module.
**Acceptance criteria:**
- [ ] Same validation and error handling as Android
- [ ] JWT stored in browser (secure storage strategy documented)
  **Labels:** `web`, `auth`

---

### DRK-112: Web app — Feed screen
**Description:** Mirror of DRK-109 for Web target.
**Acceptance criteria:**
- [ ] Visual parity with Android feed (responsive layout for wider viewport)
- [ ] Infinite scroll or "load more" pagination
  **Labels:** `web`, `feed`

---

### DRK-113: Web app — Create post screen
**Description:** Mirror of DRK-110 for Web target.
**Acceptance criteria:**
- [ ] Same field validation as Android
- [ ] Image upload UI stubbed (media service arrives Phase 2)
  **Labels:** `web`, `post`

---

### DRK-114: End-to-end smoke test suite
**Description:** Basic E2E tests covering register → login → post → feed → like → comment.
**Acceptance criteria:**
- [ ] Automated test suite runs in CI
- [ ] Covers happy path for both Android (via Espresso/Compose test) and backend (via integration tests)
  **Labels:** `testing`, `qa`

---

## Suggested Sprint Grouping (2-week sprints, solo + agents)

| Sprint | Tickets |
|---|---|
| Sprint 1 | DRK-001 → DRK-005 |
| Sprint 2 | DRK-006 → DRK-008 |
| Sprint 3 | DRK-101 → DRK-104 |
| Sprint 4 | DRK-105 → DRK-107 |
| Sprint 5 | DRK-108 → DRK-110 |
| Sprint 6 | DRK-111 → DRK-114 |