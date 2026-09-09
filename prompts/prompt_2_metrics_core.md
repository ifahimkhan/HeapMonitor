# Prompt 2 — Metrics Core (Sampler + Model)

> **Before starting**: Read `CLAUDE.md` for full project context, naming conventions, and rules.

---

## Role

You are a senior Kotlin engineer specialising in Android runtime internals and coroutines. Write
UI-free, fully unit-testable code. Preserve the module layout in `CLAUDE.md`.

---

## Goal

`heapmonitor/.../core/` contains an immutable `HeapSnapshot` model, a `HeapStatsSource` abstraction
with a real `RuntimeHeapStatsSource`, a safe `GcStatsParser`, a `ByteFormatter`, and a coroutine
`HeapSampler` that emits `StateFlow<HeapSnapshot>` at a configurable interval — all with unit tests
passing.

---

## Context Snapshot

- **Stack**: Kotlin 2.0.21, kotlinx-coroutines 1.10.2, JUnit 4, kotlinx-coroutines-test
- **Android APIs used** (only inside `RuntimeHeapStatsSource`):
  - `Runtime.getRuntime()`: `maxMemory()`, `totalMemory()`, `freeMemory()`
  - `android.os.Debug.getNativeHeapAllocatedSize()`, `getNativeHeapFreeSize()`, `getNativeHeapSize()`
  - `android.os.Debug.getRuntimeStats(): Map<String, String>` (API 23+) — keys `art.gc.gc-count`, `art.gc.gc-time`, `art.gc.blocking-gc-count`, `art.gc.blocking-gc-time`, `art.gc.bytes-allocated`, `art.gc.bytes-freed`
  - `ActivityManager.memoryClass` / `largeMemoryClass` (MB), `ApplicationInfo.FLAG_LARGE_HEAP`
- **Relevant existing files**: `heapmonitor/build.gradle.kts` (deps already present), `heapmonitor/src/main/java/com/fahim/heapmonitor/HeapMonitor.kt` (placeholder — leave it)
- **Upstream**: prompt 1 (module exists)
- **Downstream**: prompt 3 (UI collects `StateFlow<HeapSnapshot>`), prompt 4 (facade owns the sampler)

---

## Task Breakdown

1. **Inspect first** — read `heapmonitor/build.gradle.kts`; confirm coroutines + coroutines-test deps. Do not write yet.
2. **Create `core/HeapSnapshot.kt`** (public).
3. **Create `core/HeapStatsSource.kt`** — interface + `RawHeapStats` + `RuntimeHeapStatsSource` (internal).
4. **Create `core/GcStatsParser.kt`** (internal) — pure function over `Map<String, String>`.
5. **Create `core/ByteFormatter.kt`** (public object).
6. **Create `core/HeapSampler.kt`** (internal) — ticker with `start()` / `stop()`.
7. **Write unit tests** under `heapmonitor/src/test/java/com/fahim/heapmonitor/core/` for every class above using a `FakeHeapStatsSource`. Run `./gradlew :heapmonitor:testDebugUnitTest`.
8. **Validate**: all tests green, no Compose/View imports anywhere in `core/`.

---

## Detailed Requirements

### `HeapSnapshot` (public data class, all `val`)
- Fields: `timestampMs`, `maxBytes`, `totalBytes` (allocated by VM), `freeBytes` (free inside total), `nativeHeapSizeBytes`, `nativeHeapAllocatedBytes`, `nativeHeapFreeBytes`, `gcCount`, `gcTimeMs`, `blockingGcCount`, `blockingGcTimeMs`, `bytesAllocatedTotal`, `bytesFreedTotal`, `gcCountDelta` (since previous snapshot), `memoryClassMb`, `largeMemoryClassMb`, `isLargeHeap`
- Derived (computed properties, no stored duplicates): `usedBytes = totalBytes - freeBytes`, `availableBytes = maxBytes - usedBytes`, `usedPercent: Int = (usedBytes * 100 / maxBytes)` guarded against `maxBytes == 0` → 0, clamped 0..100
- `companion object { val EMPTY }` — all zeros, `timestampMs = 0`
- Must be usable in JVM unit tests with no Android classes

### `HeapStatsSource`
- `interface HeapStatsSource { fun read(): RawHeapStats }`
- `RawHeapStats`: immutable data class mirroring the raw inputs (runtime triple, native triple, `gcStats: Map<String, String>`, memory class ints, large-heap flag)
- `RuntimeHeapStatsSource(context: Context)`: every Android call wrapped in `runCatching { }.getOrDefault(0 / emptyMap())`. Never throws. Read `memoryClass` once in constructor (it does not change).

### `GcStatsParser`
- `fun parse(stats: Map<String, String>): GcStats` where `GcStats` is an immutable data class of six `Long`s
- Missing key, blank, or non-numeric value → `0L`. Never throws.

### `ByteFormatter`
- `fun format(bytes: Long): String` → `"0 B"`, `"512 B"`, `"1.5 KB"`, `"42.3 MB"`, `"1.2 GB"`; one decimal for KB and up, negative input → `"0 B"`
- `fun formatMb(bytes: Long): String` → `"42.3"` (no unit, for compact chip)

### `HeapSampler`
- Constructor: `(source: HeapStatsSource, intervalMs: Long, dispatcher: CoroutineDispatcher = Dispatchers.Default, clock: () -> Long = System::currentTimeMillis)`
- `val snapshots: StateFlow<HeapSnapshot>` starts at `HeapSnapshot.EMPTY`
- `fun start()`: idempotent; launches loop in its own `CoroutineScope(SupervisorJob() + dispatcher)`: read → build snapshot (compute `gcCountDelta` from previous value) → emit → `delay(intervalMs)`
- `fun stop()`: cancels the loop; safe to call twice; `start()` after `stop()` works again
- `fun sampleNow()`: synchronous single read + emit (used after Force GC)
- `intervalMs` must be >= 100 ms; coerce with `coerceAtLeast`
- Exceptions inside the loop are caught per-iteration and the loop continues (host app must never crash)

### Tests (minimum)
- `HeapSnapshotTest`: derived values, percent clamping, zero max
- `GcStatsParserTest`: full map, missing keys, garbage values
- `ByteFormatterTest`: boundaries at 1024 multiples, negative
- `HeapSamplerTest` with `StandardTestDispatcher` + `runTest` + fake source that returns a scripted list: emits on start, delta computed, stop halts emissions, restart works, throwing source does not kill loop

---

## Output Specification

| Action | File path | Contents |
|--------|-----------|----------|
| CREATE | `heapmonitor/src/main/java/com/fahim/heapmonitor/core/HeapSnapshot.kt` | model |
| CREATE | `.../core/HeapStatsSource.kt` | interface, `RawHeapStats`, `RuntimeHeapStatsSource` |
| CREATE | `.../core/GcStatsParser.kt` | parser + `GcStats` |
| CREATE | `.../core/ByteFormatter.kt` | formatter |
| CREATE | `.../core/HeapSampler.kt` | sampler |
| CREATE | `heapmonitor/src/test/java/com/fahim/heapmonitor/core/*Test.kt` | 4 test files + `FakeHeapStatsSource.kt` |

Show **only these files**.

---

## Constraints

- Do NOT import anything from `androidx.compose.*` or `android.view.*` in `core/`
- Do NOT touch `HeapMonitor.kt`, `:app`, or gradle files
- Do NOT use `GlobalScope`; do NOT block threads (`Thread.sleep`)
- Do NOT add Robolectric or MockK — use the fake source
- Immutable models only; no `var` in data classes

---

## Success Criteria

- [ ] `./gradlew :heapmonitor:testDebugUnitTest` green
- [ ] `HeapSnapshot.EMPTY.usedPercent == 0`, no division by zero
- [ ] `HeapSampler` survives a throwing source
- [ ] Update `CLAUDE.md` Prompt File Index row 2 to `[x] done`
