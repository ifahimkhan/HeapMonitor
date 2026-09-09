# Prompt 7 — Publishing (maven-publish, JitPack, README, no-op artifact)

> **Before starting**: Read `CLAUDE.md` for full project context and rules.

---

## Role

You are a senior Android library maintainer. Make the library consumable from any Compose project
with two Gradle lines, document it, and provide a zero-footprint release variant.

---

## Goal

A tagged GitHub release resolves via JitPack as `com.github.ifahimkhan:heapmonitor:<tag>` (and
`heapmonitor-noop`), README shows a copy-paste integration, and a GitHub Actions workflow runs unit
tests + assemble on push.

---

## Context Snapshot

- **Stack**: AGP 9.0.0, Gradle 9.1.0, JDK 17, `maven-publish` plugin, JitPack
- **Relevant existing files**:
  - `heapmonitor/build.gradle.kts` — no publishing block yet
  - `settings.gradle.kts` — includes `:app`, `:heapmonitor`
  - `heapmonitor/.../HeapMonitor.kt`, `HeapMonitorConfig.kt`, `ui/HeapMonitorOverlay.kt` — public API to mirror in noop
  - `app/src/main/java/.../DemoApplication.kt` — manual-install snippet for README
- **Upstream**: prompts 5 and 6
- **Downstream**: none

---

## Task Breakdown

1. **Inspect first** — read the files above. Do not write yet.
2. **Publishing in `heapmonitor/build.gradle.kts`**: apply `` `maven-publish` ``; `android { publishing { singleVariant("release") { withSourcesJar() } } }`; `afterEvaluate { publishing { publications { create<MavenPublication>("release") { from(components["release"]); groupId = "com.github.ifahimkhan"; artifactId = "heapmonitor"; version = project.findProperty("VERSION_NAME") as String? ?: "0.1.0" } } } }`.
3. **Create `:heapmonitor-noop`** module: same namespace suffix `com.fahim.heapmonitor` (same package so imports are identical), same public symbols (`HeapMonitor` object, `HeapMonitorConfig`, `OverlayPosition`, `HeapSnapshot`, `HeapMonitorOverlay` composable) with empty bodies / `EMPTY` returns; no ContentProvider; deps only Compose runtime + coroutines-core. Publish as `heapmonitor-noop`. Add to `settings.gradle.kts`.
4. **`gradle.properties`**: add `VERSION_NAME=0.1.0`.
5. **Create `jitpack.yml`**: `jdk: - openjdk17`, `install: - ./gradlew :heapmonitor:publishToMavenLocal :heapmonitor-noop:publishToMavenLocal`.
6. **Create `.github/workflows/ci.yml`**: on push/PR; `actions/setup-java` 17; `./gradlew :heapmonitor:testDebugUnitTest :heapmonitor:assembleRelease :heapmonitor-noop:assembleRelease :app:assembleDebug`.
7. **Create `README.md`** (see Requirements).
8. **Create `CHANGELOG.md`** with `0.1.0` entry.
9. **Validate**: `./gradlew :heapmonitor:publishToMavenLocal :heapmonitor-noop:publishToMavenLocal` succeeds; inspect `~/.m2/repository/com/github/ifahimkhan/`.

---

## Detailed Requirements

### README sections (in order)
1. One-line pitch + screenshot placeholder (`docs/overlay.png`)
2. What it shows (bullet list of metrics)
3. Install — settings `maven { url = uri("https://jitpack.io") }`; then:
   ```kotlin
   // simplest: runtime-gated, no-op in non-debuggable builds
   implementation("com.github.ifahimkhan:heapmonitor:0.1.0")

   // zero-footprint release (LeakCanary style)
   debugImplementation("com.github.ifahimkhan:heapmonitor:0.1.0")
   releaseImplementation("com.github.ifahimkhan:heapmonitor-noop:0.1.0")
   ```
4. Usage — "That's it. Auto-installs via ContentProvider." Then manual config: opt-out provider snippet + `HeapMonitor.install(this, HeapMonitorConfig(...))`
5. Compose-only usage: `HeapMonitorOverlay()` inside your own `Box` if you want to place it yourself
6. Config table (every `HeapMonitorConfig` field, default, meaning)
7. How gating works (FLAG_DEBUGGABLE; `enabled = true` to force in a debuggable-false QA build)
8. Reading the numbers (Max vs Allocated vs Used vs Free vs Available; GC count source `art.gc.gc-count`; note that `largeHeap` is intentionally not used)
9. Requirements: minSdk 23, Compose BOM ≥ 2024.09, AGP ≥ 8.x for consumers (library is built with 9.0)
10. License (MIT) — create `LICENSE`

### No-op module
- Must compile against the same Compose BOM; `HeapMonitorOverlay()` renders nothing (`Spacer(Modifier.size(0.dp))` or empty)
- `HeapMonitor.snapshots` returns a shared `StateFlow(HeapSnapshot.EMPTY)`
- Copy `HeapSnapshot` and `HeapMonitorConfig` verbatim so the types are source-compatible

---

## Output Specification

| Action | File path | Contents |
|--------|-----------|----------|
| MODIFY | `heapmonitor/build.gradle.kts` | maven-publish |
| CREATE | `heapmonitor-noop/build.gradle.kts`, `src/main/AndroidManifest.xml`, `src/main/java/com/fahim/heapmonitor/*.kt` | noop API |
| MODIFY | `settings.gradle.kts`, `gradle.properties` | include noop, VERSION_NAME |
| CREATE | `jitpack.yml`, `.github/workflows/ci.yml`, `README.md`, `CHANGELOG.md`, `LICENSE` | docs + CI |

Show **only these files**.

---

## Constraints

- Do NOT change any public API in `:heapmonitor`
- Do NOT add signing / Maven Central config (out of scope for 0.1.0)
- Do NOT bump AGP or Gradle
- Do NOT publish to any remote from this prompt — local only; tagging/pushing is the user's call

---

## Success Criteria

- [ ] `publishToMavenLocal` produces AAR + sources jar for both artifacts
- [ ] A fresh Compose project can add the two Gradle lines and see the overlay (describe the manual check)
- [ ] README integration snippet compiles as written
- [ ] Update `CLAUDE.md` Prompt File Index row 7 to `[x] done` and add a Change Log row
