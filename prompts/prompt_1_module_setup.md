# Prompt 1 — Module Setup (Build / Scaffold)

> **Before starting**: Read `CLAUDE.md` for full project context, stack, version pins, and rules.

---

## Role

You are a senior Android build engineer on AGP 9.0 / Gradle 9 / Kotlin 2.0. You are working inside an
existing Android Studio template project. Preserve the existing `:app` module; add the library module.

---

## Goal

`./gradlew assembleDebug` succeeds with a new empty `:heapmonitor` Android library module that `:app`
depends on, using pinned versions that work with AGP 9.0.0.

---

## Context Snapshot

- **Stack**: AGP 9.0.0 (hard cap), Gradle 9.1.0 wrapper, Kotlin 2.0.21 built-in (no `kotlin-android` plugin), Compose BOM 2024.09.00, JDK 17, Java 11 bytecode
- **Relevant existing files**:
  - `settings.gradle.kts` — includes only `:app`
  - `build.gradle.kts` — root, applies `android.application` + `kotlin.compose` with `apply false`
  - `gradle/libs.versions.toml` — has coreKtx **1.19.0** and lifecycleRuntimeKtx **2.11.0** (both BREAK on AGP 9.0 — must pin down)
  - `app/build.gradle.kts` — template app, namespace `com.fahim.heapgarbagecollectionlibrary`, minSdk 26, uses `compileSdk { version = release(36) }` DSL
- **Upstream**: none
- **Downstream**: every other prompt builds inside `:heapmonitor`

---

## Task Breakdown

1. **Inspect first** — read the four files above. Note the exact `compileSdk` DSL form used in `app/build.gradle.kts` and mirror it.
2. **Fix version pins in toml**: `coreKtx = "1.18.0"`, `lifecycleRuntimeKtx = "2.10.0"`. Add versions `coroutines = "1.10.2"`.
3. **Add toml entries**:
   - plugin `android-library = { id = "com.android.library", version.ref = "agp" }`
   - libs: `androidx-compose-foundation` (`androidx.compose.foundation:foundation`, BOM-managed), `androidx-lifecycle-runtime-compose` (`androidx.lifecycle:lifecycle-runtime-compose`, ref lifecycleRuntimeKtx), `androidx-activity` (`androidx.activity:activity-ktx`, ref activityCompose), `kotlinx-coroutines-android`, `kotlinx-coroutines-test` (ref coroutines)
4. **Root `build.gradle.kts`**: add `alias(libs.plugins.android.library) apply false`.
5. **`settings.gradle.kts`**: `include(":app", ":heapmonitor")`.
6. **Create `heapmonitor/build.gradle.kts`**:
   - plugins: `android.library`, `kotlin.compose`
   - `namespace = "com.fahim.heapmonitor"`, same compileSdk DSL as app, `minSdk = 23`, `consumerProguardFiles("consumer-rules.pro")`
   - `buildFeatures { compose = true }`, `compileOptions` Java 11 like app
   - deps: `implementation(platform(compose.bom))`, `ui`, `ui-graphics`, `foundation`, `material3`, `activity`, `lifecycle-runtime-compose`, `coroutines-android`, `core-ktx`; tests: `junit`, `coroutines-test`; androidTest: `androidx-junit`, `compose-ui-test-junit4`, `platform(bom)`; `debugImplementation(ui-test-manifest)`
   - Do NOT add `maven-publish` yet (prompt 7)
7. **Create** `heapmonitor/src/main/AndroidManifest.xml` (empty `<manifest/>`), `heapmonitor/consumer-rules.pro` (empty with comment), `heapmonitor/proguard-rules.pro`, `heapmonitor/.gitignore` (`/build`).
8. **Create placeholder** `heapmonitor/src/main/java/com/fahim/heapmonitor/HeapMonitor.kt` containing only `object HeapMonitor` with a KDoc TODO — prompt 4 fills it.
9. **`app/build.gradle.kts`**: add `implementation(project(":heapmonitor"))`.
10. **Validate**: run `./gradlew assembleDebug --console=plain` (Windows: `gradlew.bat`). Fix any AGP 9 DSL errors. If `checkDebugAarMetadata` fails, the pins in step 2 were not applied.

---

## Output Specification

| Action | File path | Contents |
|--------|-----------|----------|
| MODIFY | `gradle/libs.versions.toml` | pins + new aliases |
| MODIFY | `build.gradle.kts` | library plugin alias |
| MODIFY | `settings.gradle.kts` | include `:heapmonitor` |
| MODIFY | `app/build.gradle.kts` | project dep |
| CREATE | `heapmonitor/build.gradle.kts` | library config |
| CREATE | `heapmonitor/src/main/AndroidManifest.xml` | empty manifest |
| CREATE | `heapmonitor/consumer-rules.pro`, `proguard-rules.pro`, `.gitignore` | stubs |
| CREATE | `heapmonitor/src/main/java/com/fahim/heapmonitor/HeapMonitor.kt` | placeholder object |

Show **only these files**. Do not show unchanged files.

---

## Constraints

- Do NOT apply `org.jetbrains.kotlin.android` or `kotlin("android")` — AGP 9 fails with built-in Kotlin enabled
- Do NOT touch `gradle-wrapper.properties`, `gradle.properties`, or `:app` source files
- Do NOT bump AGP, Kotlin, or Compose BOM
- Do NOT add androidx.startup, Hilt, or any DI
- Explanations 3 sentences max

---

## Success Criteria

- [ ] `./gradlew assembleDebug` passes
- [ ] `./gradlew :heapmonitor:testDebugUnitTest` passes (zero tests is fine)
- [ ] `heapmonitor` has minSdk 23, namespace `com.fahim.heapmonitor`, Compose enabled
- [ ] toml pins: core-ktx 1.18.0, lifecycle 2.10.0
- [ ] Update `CLAUDE.md` Prompt File Index row 1 to `[x] done`
