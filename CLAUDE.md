# CLAUDE.md — HeapMonitor

> Persistent memory for Claude Code. Read before any task. Do not skip.

---

## App Identity

- **Name**: HeapMonitor (`heapmonitor`)
- **Purpose**: Drop-in Android library that draws a small draggable overlay on every screen showing
  JVM heap (max / allocated / used / free), native heap, and ART GC counters. Visible only in
  debuggable builds; silent no-op in production.
- **Target users**: Android developers on any Jetpack Compose (or View) project who want live memory
  feedback without opening the profiler.
- **Platform**: Android, minSdk 23 (library), 26 (sample app)
- **Status**: 0.1.0 implemented (prompts 1-7 done); library, no-op artifact, sample app, tests, publishing config in place.

---

## Tech Stack

- Language: Kotlin 2.0.21 (AGP 9.0 built-in Kotlin — do NOT apply `org.jetbrains.kotlin.android`)
- Build: AGP **9.0.0** (hard cap — IDE is Android Studio Otter 3), Gradle 9.1.0, JDK 17, Java 11 bytecode
- UI: Jetpack Compose, BOM `2024.09.00`, Material 3, `kotlin.plugin.compose` 2.0.21
- Async: kotlinx-coroutines 1.10.2, `StateFlow` for snapshot stream
- Memory APIs: `Runtime.getRuntime()`, `android.os.Debug.getRuntimeStats()`, `ActivityManager.memoryClass`
- Tests: JUnit 4, kotlinx-coroutines-test, Compose UI test (`ui-test-junit4`), AndroidJUnitRunner
- Publishing: `maven-publish` + JitPack (`com.github.ifahimkhan.HeapMonitor:heapmonitor:<tag>`)
- No DI framework, no Hilt, no Room, no network. Zero non-AndroidX runtime deps.

### Version pins (do not bump without checking AAR min-AGP)
- `core-ktx` **1.18.0**, `lifecycle` **2.10.0**, `activity-compose` **1.13.0** — 1.19 / 2.11 need AGP >= 9.1 and fail in `checkDebugAarMetadata`.
- `espresso-core` **3.7.0** must be an explicit `androidTestImplementation` in `:heapmonitor`: Compose 1.7 pulls Espresso 3.5, which crashes on API 36 with `NoSuchMethodException: InputManager.getInstance`.

---

## Project Structure

```
HeapGarbageCollectionLibrary/
├── heapmonitor/                       # THE LIBRARY (com.android.library)
│   └── src/main/java/com/fahim/heapmonitor/
│       ├── HeapMonitor.kt             # public facade: install / uninstall / snapshots
│       ├── HeapMonitorConfig.kt       # immutable config data class + OverlayPosition
│       ├── core/                      # pure Kotlin, no Compose/View imports
│       │   ├── HeapSnapshot.kt        # immutable metrics model + derived values
│       │   ├── HeapStatsSource.kt     # interface + RuntimeHeapStatsSource (Runtime + Debug)
│       │   ├── GcStatsParser.kt       # parses Debug.getRuntimeStats() map safely
│       │   ├── HeapSampler.kt         # coroutine ticker -> StateFlow<HeapSnapshot>
│       │   └── ByteFormatter.kt       # "42.3 MB"
│       ├── install/                   # attach/detach + gating
│       │   ├── DebugGate.kt           # enabled? (FLAG_DEBUGGABLE or config override)
│       │   ├── HeapMonitorInstaller.kt# ContentProvider auto-init (opt-out via manifest)
│       │   └── OverlayAttacher.kt     # ActivityLifecycleCallbacks -> ComposeView in DecorView
│       └── ui/                        # Compose overlay
│           ├── HeapMonitorOverlay.kt  # public composable entry
│           ├── CompactChip.kt         # collapsed pill
│           ├── ExpandedCard.kt        # full metrics card
│           ├── UsageBar.kt            # colored % bar
│           ├── DraggableBox.kt        # drag + clamp-to-bounds
│           └── OverlayTheme.kt        # self-contained dark palette (host theme independent)
├── heapmonitor-noop/                  # optional: same API, empty bodies (releaseImplementation)
├── app/                               # SAMPLE app (com.android.application) — demonstrates usage
├── prompts/                           # Claude Code prompt files (see index below)
├── gradle/libs.versions.toml
├── settings.gradle.kts                # include(":app", ":heapmonitor")
└── CLAUDE.md
```

---

## Domain Ownership Map

| Domain | Module / Dir | Primary files |
|--------|--------------|---------------|
| Module setup / build | root, `heapmonitor/build.gradle.kts` | toml, settings |
| Metrics core | `heapmonitor/.../core/` | `HeapSampler.kt`, `HeapSnapshot.kt` |
| Overlay UI | `heapmonitor/.../ui/` | `HeapMonitorOverlay.kt` |
| Integration / gating | `heapmonitor/.../install/`, `HeapMonitor.kt` | `OverlayAttacher.kt`, `DebugGate.kt` |
| Sample app | `app/` | `MainActivity.kt` |
| Tests | `heapmonitor/src/test`, `src/androidTest` | `*Test.kt` |
| Publishing | `heapmonitor/build.gradle.kts`, `jitpack.yml`, `README.md` | — |

---

## Architecture Decisions

- **Single source of truth**: `HeapSampler` exposes `StateFlow<HeapSnapshot>`; UI only collects, never reads `Runtime` itself.
- **Core is UI-free**: `core/` may import `android.os.Debug` only behind `HeapStatsSource`; every class there is unit-testable with a fake source.
- **Immutability**: all models are `data class` with `val`; updates via `copy()`. Only mutable state is the facade's install state (`@Volatile`, guarded).
- **Gating is runtime, not compile-time**: `DebugGate` checks `ApplicationInfo.FLAG_DEBUGGABLE`. Release APK: `install()` returns immediately, nothing sampled, nothing drawn. Consumer may force via `HeapMonitorConfig(enabled = true/false)`.
- **Zero-footprint option**: consumers can use `debugImplementation(heapmonitor)` + `releaseImplementation(heapmonitor-noop)` (LeakCanary pattern).
- **Auto-install**: `HeapMonitorInstaller` ContentProvider (no androidx.startup dep). Opt-out: `<provider android:name="com.fahim.heapmonitor.install.HeapMonitorInstaller" tools:node="remove"/>` then call `HeapMonitor.install(app, config)` manually.
- **Overlay attachment**: `OverlayAttacher` registers `ActivityLifecycleCallbacks`; on `onActivityResumed` adds one tagged `ComposeView` to `window.decorView` (a FrameLayout) if absent. Only for `ComponentActivity` (needs ViewTree owners). Works for Compose AND View-based screens. ViewTree owners are set on the overlay view itself, and `initializeViewTreeOwners()` is re-run when the decor still carries a DESTROYED owner (Android reuses the decor on local relaunch / `recreate()`).
- **Sampling only in foreground**: sampler starts when first activity starts, stops when last activity stops. Default interval 1000 ms on `Dispatchers.Default`.
- **Overlay is draggable, compact by default**, expands on tap, has "Force GC" button (`Runtime.gc()`), respects `safeDrawing` insets, own dark palette (readable on any host theme).
- **GC counters**: `Debug.getRuntimeStats()` keys `art.gc.gc-count`, `art.gc.gc-time`, `art.gc.blocking-gc-count`, `art.gc.blocking-gc-time`, `art.gc.bytes-allocated`, `art.gc.bytes-freed`. Missing/unparseable -> `0`, never crash.
- **Public API surface is tiny**: `HeapMonitor` object, `HeapMonitorConfig`, `OverlayPosition`, `HeapSnapshot`, `HeapMonitorOverlay()` composable. Everything else `internal`.

---

## Public API (contract — keep stable)

```kotlin
object HeapMonitor {
    fun install(application: Application, config: HeapMonitorConfig = HeapMonitorConfig())
    fun uninstall()
    val isInstalled: Boolean
    val snapshots: StateFlow<HeapSnapshot>      // HeapSnapshot.EMPTY until first sample
    fun forceGc()
}
data class HeapMonitorConfig(
    val enabled: Boolean? = null,                // null = auto (debuggable flag)
    val sampleIntervalMs: Long = 1_000L,
    val initialPosition: OverlayPosition = OverlayPosition.TopEnd,
    val startExpanded: Boolean = false,
    val showForceGcButton: Boolean = true,
    val warnUsedPercent: Int = 75,
    val criticalUsedPercent: Int = 90,
)
enum class OverlayPosition { TopStart, TopEnd, BottomStart, BottomEnd }
@Composable fun HeapMonitorOverlay(modifier: Modifier = Modifier, config: HeapMonitorConfig = HeapMonitorConfig())
```

---

## Naming Conventions

| Entity | Convention | Example |
|--------|-----------|---------|
| Package root | `com.fahim.heapmonitor` | — |
| Composables | PascalCase noun | `ExpandedCard` |
| Data models | `data class`, `val` only | `HeapSnapshot` |
| Interfaces + impl | `[Name]` / `Runtime[Name]` / `Fake[Name]` | `HeapStatsSource`, `FakeHeapStatsSource` |
| Tests | `[Class]Test.kt`, backtick method names | `HeapSamplerTest` |
| Constants | `private const val` / `internal object Defaults` | `Defaults.INTERVAL_MS` |
| Gradle alias | kebab in toml, dot in kts | `androidx-compose-foundation` |

---

## Critical Rules for Claude Code

### Always
- Read the files listed in each prompt's Context Snapshot before writing
- Keep `core/` free of Compose/View imports
- Wrap every `Debug.*` / `Runtime.*` call so a failure yields a default, never a crash in the host app
- Run `./gradlew :heapmonitor:testDebugUnitTest` after core changes; `./gradlew assembleDebug` after build changes
- Show only changed/created files; explanations 3 sentences max
- Functions under 50 lines, files under 400 lines

### Never
- Apply `org.jetbrains.kotlin.android` plugin (AGP 9 built-in Kotlin) or bump AGP above 9.0.0
- Bump `core-ktx` / `lifecycle` above pinned versions
- Add Hilt, Koin, androidx.startup, Timber, or any non-AndroidX runtime dependency
- Make the library mutate host app state, register receivers, or request permissions (no `SYSTEM_ALERT_WINDOW` — overlay lives inside the activity window)
- Call `Runtime.getRuntime()` from a composable
- Add `largeHeap` to any manifest
- Rewrite `:app` template files not listed in a prompt

---

## Prompt File Index

| # | File | Domain | Status | Depends on |
|---|------|--------|--------|-----------|
| 1 | `prompts/prompt_1_module_setup.md` | Build / module scaffold | [x] done | — |
| 2 | `prompts/prompt_2_metrics_core.md` | Metrics core (sampler, model) | [x] done | 1 |
| 3 | `prompts/prompt_3_overlay_ui.md` | Compose overlay UI | [x] done | 2 |
| 4 | `prompts/prompt_4_integration_api.md` | Facade, gating, auto-attach | [x] done | 2, 3 |
| 5 | `prompts/prompt_5_sample_app.md` | Sample app demo | [x] done | 4 |
| 6 | `prompts/prompt_6_testing.md` | Unit + UI + instrumented tests | [x] done | 4 |
| 7 | `prompts/prompt_7_publishing.md` | maven-publish, JitPack, README, noop | [x] done | 5, 6 |

Mark `[x] done` as each completes.

---

## Change Log

| Date | Change | Prompt |
|------|--------|--------|
| 2026-09-09 | Initial architecture + prompt set | — |
| 2026-09-09 | Module scaffold, core sampler + model, Compose overlay, facade + auto-attach, sample app | 1-5 |
| 2026-09-09 | Unit + Compose UI + instrumented tests; Espresso 3.7.0 pinned for API 36; decor-owner refresh on relaunch | 6 |
| 2026-09-09 | maven-publish, `:heapmonitor-noop`, jitpack.yml, CI workflow, README, CHANGELOG, LICENSE | 7 |
