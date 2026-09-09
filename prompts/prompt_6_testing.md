# Prompt 6 — Testing (Unit + Compose UI + Instrumented)

> **Before starting**: Read `CLAUDE.md` for full project context and rules.

---

## Role

You are a senior Android test engineer. Prompt 2 already covered `core/` unit tests; this prompt
covers the gate, the facade, the overlay UI, and the attach behaviour. Target 80%+ line coverage of
`:heapmonitor`.

---

## Goal

`./gradlew :heapmonitor:testDebugUnitTest` and `./gradlew :heapmonitor:connectedDebugAndroidTest`
pass, covering `DebugGate`, `HeapMonitor` facade lifecycle, `HeapMonitorOverlayContent`
rendering/interaction, and `OverlayAttacher` view attachment.

---

## Context Snapshot

- **Stack**: JUnit 4, kotlinx-coroutines-test, `compose-ui-test-junit4` (`createComposeRule`), `androidx.test.ext.junit` (`ActivityScenario`)
- **Relevant existing files**:
  - `heapmonitor/src/test/java/com/fahim/heapmonitor/core/*` — existing tests + `FakeHeapStatsSource`
  - `heapmonitor/.../install/DebugGate.kt`, `OverlayAttacher.kt`, `HeapMonitor.kt`
  - `heapmonitor/.../ui/HeapMonitorOverlay.kt` — `HeapMonitorOverlayContent(snapshot, config, onForceGc)` and test tags `heap_overlay`, `heap_expanded`, `heap_force_gc`
  - `heapmonitor/build.gradle.kts` — test deps already declared in prompt 1
- **Upstream**: prompt 4
- **Downstream**: prompt 7 (CI/README badge references these tasks)

---

## Task Breakdown

1. **Inspect first** — read the files above and list which branches are untested. Do not write yet.
2. **Refactor for testability (minimal)**: if `DebugGate` reads `ApplicationInfo` directly, extract `internal fun isEnabled(debuggableFlagSet: Boolean, config: HeapMonitorConfig): Boolean` pure overload and have the Context version delegate. Same idea for `HeapMonitor`: add an `internal fun installForTest(sampler, attacherFactory)` seam only if needed; prefer constructor injection over mocking.
3. **Unit tests** (`src/test`):
   - `DebugGateTest`: null → flag; `true` overrides non-debuggable; `false` overrides debuggable
   - `HeapMonitorFacadeTest`: `install` twice is idempotent; `snapshots` returns EMPTY before install; `forceGc()` safe when not installed; `uninstall()` resets `isInstalled`
4. **Compose UI tests** (`src/androidTest/.../ui/`): `HeapMonitorOverlayContentTest` with `createComposeRule()`:
   - compact chip shows `"84.0/200.0 MB · GC 17"` for the fake snapshot
   - tapping chip shows `heap_expanded`; tapping `Collapse` hides it
   - `heap_force_gc` click invokes callback once; hidden when `showForceGcButton=false`
   - usage bar color: assert via `semantics` custom key or `assertExists` of a `testTag("heap_usage_critical")` when percent ≥ critical
5. **Instrumented attach test** (`src/androidTest/.../install/`): a test `ComponentActivity` declared in `src/androidTest/AndroidManifest.xml`; `ActivityScenario.launch`; call `HeapMonitor.install(app, HeapMonitorConfig(enabled = true))`, recreate → assert exactly one view with `Defaults.OVERLAY_TAG` in decor view; `uninstall()` → zero views.
6. **Coverage**: enable `testCoverage { }` / `enableAndroidTestCoverage` only if it does not fight AGP 9; otherwise report coverage qualitatively per class.
7. **Validate**: run both gradle tasks; paste summary counts.

---

## Detailed Requirements

- Fake snapshot factory `internal fun fakeSnapshot(usedMb: Int = 84, maxMb: Int = 200, gcCount: Long = 17, gcDelta: Long = 0)` in `src/androidTest` and `src/test` (`testFixtures` not available on AGP 9 library without extra config — duplicate the tiny helper instead)
- UI tests must not depend on `HeapMonitor` singleton state — always test `HeapMonitorOverlayContent`
- Facade tests must call `HeapMonitor.uninstall()` in `@After`
- No `Thread.sleep`; use `composeRule.waitUntil` / `runTest`
- Instrumented tests require a device/emulator; if none is connected, still write them and note it

---

## Output Specification

| Action | File path | Contents |
|--------|-----------|----------|
| CREATE | `heapmonitor/src/test/java/com/fahim/heapmonitor/install/DebugGateTest.kt` | gate tests |
| CREATE | `heapmonitor/src/test/java/com/fahim/heapmonitor/HeapMonitorFacadeTest.kt` | facade tests |
| CREATE | `heapmonitor/src/androidTest/java/com/fahim/heapmonitor/ui/HeapMonitorOverlayContentTest.kt` | UI tests |
| CREATE | `heapmonitor/src/androidTest/java/com/fahim/heapmonitor/install/OverlayAttacherTest.kt` | attach test |
| CREATE | `heapmonitor/src/androidTest/AndroidManifest.xml` | test activity |
| MODIFY | `install/DebugGate.kt`, `HeapMonitor.kt` | testability seams only, if required |

Show **only these files**.

---

## Constraints

- Do NOT add MockK, Mockito, Robolectric, or Kover — plain fakes and seams
- Do NOT change public API or behaviour to make tests pass; fix the code only if a test reveals a real bug (say so)
- Do NOT touch `:app`

---

## Success Criteria

- [ ] `testDebugUnitTest` green
- [ ] `connectedDebugAndroidTest` green on a device (or written + noted if none)
- [ ] Every `install/` and `ui/` class has at least one test
- [ ] Update `CLAUDE.md` Prompt File Index row 6 to `[x] done`
