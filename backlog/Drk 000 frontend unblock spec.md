# DRK-000: Frontend Unblock Spec — Kotlin/Ktor/Compose Multiplatform Upgrade + Test Discovery Fix

**Status:** Ready for agent
**Blocks:** DRK-108, DRK-109, DRK-110, DRK-111, DRK-112, DRK-113
**Goal:** Get `androidApp` and `webApp` building against the shared KMP module so frontend screen work (DRK-108→113) can start, and get the existing backend test suite (DRK-114) actually running.

This is a two-part spec. Do Part A first — it's small and unblocks verifying everything else you've already built.

---

## Part A — Fix test discovery (do this first)

**Symptom:** `./gradlew :backend:test` finds no tests. `find backend/src/test -type f` returns nothing.

**Root cause to verify:** the test file was not written into the Gradle-expected source set path for a Kotlin JVM module.

### Steps

1. Locate the file wherever it currently lives:
   ```bash
   find . -iname "*Tests.kt" -not -path "*/build/*"
   ```

2. It must end up at exactly:
   ```
   backend/src/test/kotlin/app/drinkin/DrinkinApplicationTests.kt
   ```
   Note: `kotlin`, not `java`, as the source root folder name — this is the default for the `kotlin("jvm")` plugin used in `backend/build.gradle.kts`.

3. Confirm package declaration at the top of the file is `package app.drinkin` (or a sub-package matching its folder, e.g. `package app.drinkin.auth` if it lives under `.../kotlin/app/drinkin/auth/`).

4. Confirm `backend/build.gradle.kts` has this at the top level (not nested inside `dependencies { }`):
   ```kotlin
   tasks.withType<Test> {
       useJUnitPlatform()
   }
   ```

5. Confirm test dependencies are declared as `testImplementation`, not `implementation`:
   ```kotlin
   testImplementation("org.springframework.boot:spring-boot-starter-test")
   testImplementation("org.springframework.security:spring-security-test")
   testImplementation("com.h2database:h2")
   ```

6. Re-run with info logging to confirm discovery:
   ```bash
   ./gradlew :backend:test --tests "*DrinkinApplicationTests*" -i
   ```

**Acceptance criteria:**
- [ ] `./gradlew :backend:test` runs and reports the integration test suite (pass or fail, but *discovered*)
- [ ] Test uses `application-test.yml` / H2 profile, no real Postgres required
- [ ] CI workflow (`.github/workflows/ci.yml`) runs this test target on every PR

---

## Part B — Upgrade Kotlin / Ktor / Compose Multiplatform for `wasmJs` support

### Why this is needed

The current scaffold pins `Kotlin 1.9.24` + `Ktor 2.3.11`. Ktor's client only gained proper `wasmJs` target support in **Ktor 3.0**, which requires **Kotlin 2.0+**. This is why `io.ktor:ktor-client-js:2.3.11` fails to resolve for the `wasmJs` target in `shared` and `webApp`.

### Versions to use

**Do not hardcode versions from an old blog post or this document blindly** — Kotlin/Wasm and Compose Multiplatform for web are still evolving quickly. Before applying version numbers, agent should:

1. Check the current `gradle/libs.versions.toml` in **https://github.com/Kotlin/kotlin-wasm-compose-template** (JetBrains' official reference template, kept in sync with current compatible releases) for the current known-good Kotlin/Compose Multiplatform pairing.
2. Cross-check the Ktor version against **https://ktor.io/docs/releases.html** for the latest 3.x release.

As a starting baseline (known to work together as of recent releases — verify against the sources above before locking in):
- Kotlin: `2.0.20` or later 2.0.x/2.1.x
- Ktor: `3.0.3` or later 3.x
- Compose Multiplatform (`org.jetbrains.compose` plugin): `1.7.0` or later, matched to the Kotlin version per JetBrains' compatibility table (`kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html`)
- Android Gradle Plugin: keep at a version compatible with the Kotlin version chosen (check AGP release notes if bumping)

### Files to change

**Root `build.gradle.kts`:**
```kotlin
plugins {
    kotlin("multiplatform") version "<new-kotlin-version>" apply false
    kotlin("jvm") version "<new-kotlin-version>" apply false
    kotlin("plugin.spring") version "<new-kotlin-version>" apply false
    kotlin("plugin.serialization") version "<new-kotlin-version>" apply false
    id("org.springframework.boot") version "3.3.0" apply false
    id("io.spring.dependency-management") version "1.1.5" apply false
    id("com.android.application") version "<check-agp-compat>" apply false
    id("org.jetbrains.compose") version "<new-compose-mpp-version>" apply false
}
```

**`shared/build.gradle.kts`:**
- Bump `kotlinx-serialization-json`, `ktor-client-core`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json` to versions matching the new Ktor line.
- For the `wasmJsMain` source set, confirm whether `ktor-client-js` (Ktor 3.x) is the correct engine artifact for `wasmJs`, or whether Ktor 3.x resolves this automatically via `ktor-client-core` — check current Ktor client-engines docs (`ktor.io/docs/client-engines.html`) since this has changed across 3.x point releases.
- Add the JetBrains Wasm experimental repository **only if still required** by the Ktor version chosen:
  ```kotlin
  repositories {
      maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
  }
  ```
  (Recent Ktor 3.x releases may no longer need this — verify by attempting resolution without it first.)

**`webApp/build.gradle.kts`:**
- Bump `org.jetbrains.compose` plugin version to match.
- No structural changes expected beyond version bumps, assuming `wasmJs { browser { ... } }` block stays the same shape.

**`androidApp/build.gradle.kts`:**
- Bump Compose plugin version.
- Confirm AGP/Kotlin compatibility per the compatibility guide before bumping `com.android.application`.

### Migration steps for the agent

1. Create a new branch: `upgrade/kotlin2-ktor3-compose-mpp`
2. Bump versions in the 4 files above, one at a time, committing after each module builds:
   - `./gradlew :shared:build`
   - `./gradlew :backend:build` (should be unaffected, but verify — backend uses `kotlin("jvm")` + Spring, not Ktor client)
   - `./gradlew :androidApp:assembleDebug`
   - `./gradlew :webApp:wasmJsBrowserDistribution`
3. Fix any `@OptIn` package-path issues that resurface (same class as the `ExperimentalWasmDsl` issue already fixed — Kotlin Gradle plugin internals move packages between versions, so expect 1–2 more of these).
4. Run `./gradlew build` at the root to confirm all 4 modules build together.
5. Update CI (`.github/workflows/ci.yml`) — no structural changes should be needed since it already builds all 4 modules, but confirm the pipeline is still green.
6. Update `AGENTS.md` "Known gaps" section to remove this item once done.

### Acceptance criteria

- [ ] `./gradlew :shared:build` succeeds with the `wasmJs` target compiling `DrinkinApiClient`
- [ ] `./gradlew :webApp:wasmJsBrowserDistribution` succeeds and produces a runnable artifact
- [ ] `./gradlew :androidApp:assembleDebug` still succeeds after the bump
- [ ] `./gradlew build` (full root build, all 4 modules) is green
- [ ] CI pipeline passes on the upgrade branch
- [ ] No version numbers are left as placeholders (`<new-kotlin-version>` etc.) — real, verified numbers are committed

### Out of scope for this ticket

- Do not touch `openapi.yaml`, backend controllers, or the database schema in this PR — keep it a pure toolchain upgrade so it's easy to review and revert independently of feature work.
- Do not implement any actual UI screens yet (that's DRK-108→113) — a placeholder "Hello" screen compiling successfully on both targets is sufficient to close this ticket.

---

## Suggested order of work

1. Part A (test discovery fix) — small, unblocks verifying everything already built
2. Part B (toolchain upgrade) — isolated PR, no feature changes
3. Once both are merged and green: DRK-108 → DRK-113 (Android + Web screens) become unblocked and can start
