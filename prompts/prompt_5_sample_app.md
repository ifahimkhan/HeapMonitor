# Prompt 5 — Sample App

> **Before starting**: Read `CLAUDE.md` for full project context and rules.

---

## Role

You are a senior Android developer writing a minimal demo that proves the library works and shows
consumers the two integration styles (zero-code auto-install vs manual config).

---

## Goal

`:app` is a small Compose demo with buttons that allocate and release memory so the overlay visibly
reacts, plus a second activity to prove the overlay follows navigation. Auto-install path is the
default; manual path is shown but commented.

---

## Context Snapshot

- **Stack**: Compose + Material 3, `:app` depends on `project(":heapmonitor")`
- **Relevant existing files**:
  - `app/src/main/java/com/fahim/heapgarbagecollectionlibrary/MainActivity.kt` — template `Greeting`; replace body
  - `app/src/main/AndroidManifest.xml` — one activity; add second activity + optional `Application`
  - `app/src/main/java/.../ui/theme/*` — keep as is
  - `heapmonitor/.../HeapMonitor.kt`, `HeapMonitorConfig.kt` — public API
- **Upstream**: prompt 4
- **Downstream**: prompt 7 README copies the integration snippet from here

---

## Task Breakdown

1. **Inspect first** — read the files above; confirm the API names. Do not write yet.
2. **Create `DemoApplication.kt`** (registered in manifest) whose `onCreate` contains a commented block showing manual install:
   ```kotlin
   // Manual config (only needed if you removed HeapMonitorInstaller in the manifest):
   // HeapMonitor.install(this, HeapMonitorConfig(sampleIntervalMs = 500, startExpanded = true))
   ```
3. **Create `MemoryStressViewModel.kt`** — holds `MutableStateFlow<Int>` of allocated chunk count; `allocate(mb: Int)` appends a `ByteArray(mb * 1024 * 1024)` to an immutable list (`list + array`), `release()` replaces with empty list, `churn()` allocates and drops 50 × 1 MB arrays in a loop to trigger GC. Guard allocation with `runCatching` to survive `OutOfMemoryError` and expose a `lastError: StateFlow<String?>`.
4. **Rewrite `MainActivity.kt`** — `DemoScreen`: title, four buttons (`Allocate 10 MB`, `Allocate 50 MB`, `Churn (trigger GC)`, `Release all`), a line `"Held: N chunks"`, an error line, and `Open second screen` navigating to `SecondActivity`. No Navigation Compose; plain `startActivity`.
5. **Create `SecondActivity.kt`** — a `ComponentActivity` with a `LazyColumn` of 200 rows; proves overlay attaches to every activity.
6. **Manifest**: register `DemoApplication` (`android:name`), add `SecondActivity` (`exported=false`).
7. **Validate**: `./gradlew :app:assembleDebug`; run on device; overlay reacts to buttons; overlay present on both screens.

---

## Output Specification

| Action | File path | Contents |
|--------|-----------|----------|
| CREATE | `app/src/main/java/com/fahim/heapgarbagecollectionlibrary/DemoApplication.kt` | app class |
| CREATE | `.../MemoryStressViewModel.kt` | allocation state |
| MODIFY | `.../MainActivity.kt` | demo screen |
| CREATE | `.../SecondActivity.kt` | list screen |
| MODIFY | `app/src/main/AndroidManifest.xml` | application name + activity |
| MODIFY | `app/build.gradle.kts` | add `androidx-lifecycle-viewmodel-compose` only if needed (add alias to toml at lifecycle 2.10.0) |

Show **only these files**.

---

## Constraints

- Do NOT add `android:largeHeap` to the manifest
- Do NOT modify `:heapmonitor`
- Do NOT add Navigation Compose, Hilt, or other libraries beyond viewmodel-compose
- Keep `MainActivity.kt` under 150 lines

---

## Success Criteria

- [ ] Tapping `Allocate 50 MB` raises Used in the overlay within one sample interval
- [ ] `Churn` increments GC count and shows `(+n)` delta
- [ ] Overlay visible on `SecondActivity`, single instance after rotation
- [ ] Release build shows no overlay
- [ ] Update `CLAUDE.md` Prompt File Index row 5 to `[x] done`
