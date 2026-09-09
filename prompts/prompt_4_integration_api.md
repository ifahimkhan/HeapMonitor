# Prompt 4 — Integration API (Facade, Debug Gate, Auto-Attach)

> **Before starting**: Read `CLAUDE.md` for full project context, public API contract, and rules.

---

## Role

You are a senior Android platform engineer who has shipped debug-tooling libraries (LeakCanary-style).
Wire the core sampler and the Compose overlay into a one-line-integration facade that is a strict
no-op in non-debuggable builds.

---

## Goal

Adding the `heapmonitor` dependency to any app shows the overlay on every `ComponentActivity` in
debug builds with **zero code**, can be configured or disabled manually, and does nothing at all in
release builds.

---

## Context Snapshot

- **Stack**: Kotlin 2.0.21, `androidx.activity` (ComponentActivity), `androidx.compose.ui.platform.ComposeView`, coroutines
- **Relevant existing files**:
  - `heapmonitor/.../HeapMonitor.kt` — placeholder with `snapshots` + `forceGc()` stubs (replace)
  - `heapmonitor/.../HeapMonitorConfig.kt` — config (reuse unchanged)
  - `heapmonitor/.../core/HeapSampler.kt`, `core/HeapStatsSource.kt` — `RuntimeHeapStatsSource(context)`, `start()/stop()/sampleNow()`
  - `heapmonitor/.../ui/HeapMonitorOverlay.kt` — composable to host
  - `heapmonitor/src/main/AndroidManifest.xml` — empty; add provider here
- **Upstream**: prompts 2 and 3
- **Downstream**: prompt 5 (sample app), prompt 6 (tests), prompt 7 (noop artifact mirrors this API)

---

## Task Breakdown

1. **Inspect first** — read the five files above. Do not write yet.
2. **Create `install/DebugGate.kt`** — `internal object DebugGate { fun isEnabled(context: Context, config: HeapMonitorConfig): Boolean }`: `config.enabled ?: (appInfo.flags and FLAG_DEBUGGABLE != 0)`. Wrap in `runCatching`, default `false`.
3. **Create `install/OverlayAttacher.kt`** — `internal class OverlayAttacher(config) : Application.ActivityLifecycleCallbacks` (see Requirements).
4. **Rewrite `HeapMonitor.kt`** — the facade (see Requirements).
5. **Create `install/HeapMonitorInstaller.kt`** — `ContentProvider` with all abstract methods returning null/0; `onCreate()` calls `HeapMonitor.install(context.applicationContext as Application)` and returns `true`.
6. **Register provider** in `heapmonitor/src/main/AndroidManifest.xml`: `android:name=".install.HeapMonitorInstaller"`, `android:authorities="${applicationId}.heapmonitor-installer"`, `android:exported="false"`, `android:enabled="true"`, `android:initOrder="90"`.
7. **Add `internal object Defaults`** (in `HeapMonitorConfig.kt` or `core/`) for the overlay view tag and log tag; no magic strings inline.
8. **Validate**: `./gradlew assembleDebug` green; run `:app` on emulator/device — overlay appears without any code in `:app`. Build `assembleRelease`, install, confirm no overlay and no log line from the library.

---

## Detailed Requirements

### `HeapMonitor` facade (public `object`)
- `@Volatile private var state: InstallState?` — immutable `internal data class InstallState(application, config, sampler, attacher)`
- `install(application, config)`: synchronized; if already installed → log once at DEBUG and return; if `!DebugGate.isEnabled` → return silently (no log, no allocations beyond the check); else build `RuntimeHeapStatsSource`, `HeapSampler(source, config.sampleIntervalMs)`, `OverlayAttacher(config, sampler)`, register callbacks, store state
- `uninstall()`: unregister callbacks, `sampler.stop()`, detach overlay views from any started activities tracked by the attacher, clear state
- `isInstalled`: `state != null`
- `snapshots: StateFlow<HeapSnapshot>`: returns `state?.sampler?.snapshots ?: emptyFlow` where `emptyFlow` is a single shared `MutableStateFlow(HeapSnapshot.EMPTY).asStateFlow()`
- `forceGc()`: `Runtime.getRuntime().gc()` then `state?.sampler?.sampleNow()`; safe when not installed
- Thread-safe; all public members callable from any thread

### `OverlayAttacher`
- Tracks started activity count (`onActivityStarted` / `onActivityStopped`): `0 → 1` calls `sampler.start()`, `1 → 0` calls `sampler.stop()`
- `onActivityResumed(activity)`: if `activity !is ComponentActivity` → log once per class name and skip; else `attach(activity)`
- `attach(activity)`: `val decor = activity.window.decorView as ViewGroup`; if `decor.findViewWithTag<View>(Defaults.OVERLAY_TAG) != null` return; if `decor.findViewTreeLifecycleOwner() == null` call `activity.initializeViewTreeOwners()`; create `ComposeView(activity)` with `tag = OVERLAY_TAG`, `setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)`, `setContent { HeapMonitorOverlay(config = config) }`, add with `FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)`; call `decor.bringChildToFront(view)` so it sits above host content
- `onActivityDestroyed(activity)`: remove tagged view if present (defensive; strategy already disposes composition)
- Keep a `WeakHashMap<Activity, Unit>` (or set of weak refs) of attached activities for `uninstall()`; never hold strong Activity refs
- Every callback body wrapped in `runCatching` with a single `Log.w` on failure — the host app must never crash because of this library

### `HeapMonitorInstaller` ContentProvider
- KDoc explains opt-out: `<provider android:name="com.fahim.heapmonitor.install.HeapMonitorInstaller" tools:node="remove"/>` then manual `HeapMonitor.install(this, HeapMonitorConfig(...))` from `Application.onCreate()`
- `onCreate()` must be cheap: gate check + registration only; sampling starts on first activity start

### Logging
- Single `internal object HeapLog` wrapping `android.util.Log` with tag `Defaults.LOG_TAG = "HeapMonitor"`; only `d`/`w`; nothing logged when gate is closed

---

## Output Specification

| Action | File path | Contents |
|--------|-----------|----------|
| MODIFY | `heapmonitor/src/main/java/com/fahim/heapmonitor/HeapMonitor.kt` | full facade |
| CREATE | `.../install/DebugGate.kt` | gate |
| CREATE | `.../install/OverlayAttacher.kt` | lifecycle callbacks + attach/detach |
| CREATE | `.../install/HeapMonitorInstaller.kt` | ContentProvider |
| CREATE | `.../install/HeapLog.kt` | logger |
| MODIFY | `.../HeapMonitorConfig.kt` | add `internal object Defaults` (tags) |
| MODIFY | `heapmonitor/src/main/AndroidManifest.xml` | provider |

Show **only these files**.

---

## Constraints

- Do NOT modify `core/` or `ui/` except the `HeapMonitor.kt` stubs they referenced
- Do NOT add androidx.startup, Hilt, or ProcessLifecycleOwner (`lifecycle-process`) — activity counting is enough
- Do NOT use `WindowManager.addView` / `SYSTEM_ALERT_WINDOW`
- Do NOT hold strong references to Activities
- Do NOT log or allocate when the debug gate is closed
- Do NOT change the public API shape defined in CLAUDE.md

---

## Success Criteria

- [ ] `:app` with only the dependency (no code) shows overlay in debug
- [ ] `assembleRelease` build of `:app` shows nothing and `adb logcat -s HeapMonitor` is empty
- [ ] Rotating the device keeps one overlay (no duplicates), position persists
- [ ] Backgrounding stops sampling (verify via a `HeapLog.d` in `start/stop`)
- [ ] `HeapMonitor.uninstall()` removes the overlay from the current activity
- [ ] Update `CLAUDE.md` Prompt File Index row 4 to `[x] done`
