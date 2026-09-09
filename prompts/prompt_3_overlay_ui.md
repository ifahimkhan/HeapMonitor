# Prompt 3 — Overlay UI (Compose)

> **Before starting**: Read `CLAUDE.md` for full project context, public API contract, and rules.

---

## Role

You are a senior Jetpack Compose UI engineer. Build a small, self-contained, theme-independent
overlay. Stateless composables take a `HeapSnapshot`; no memory APIs are called from UI.

---

## Goal

`heapmonitor/.../ui/` contains `HeapMonitorOverlay()` — a draggable, tap-to-expand floating panel
that renders a `HeapSnapshot` (heap max / allocated / used / free / available, native heap, GC count +
time, GC delta) with a colored usage bar and a Force GC button — plus stateless sub-composables with
previews.

---

## Context Snapshot

- **Stack**: Compose BOM 2024.09.00, Material 3, foundation, `lifecycle-runtime-compose` (`collectAsStateWithLifecycle`)
- **Relevant existing files**:
  - `heapmonitor/.../core/HeapSnapshot.kt` — model with `usedBytes`, `availableBytes`, `usedPercent`
  - `heapmonitor/.../core/ByteFormatter.kt` — `format()`, `formatMb()`
  - `heapmonitor/.../HeapMonitor.kt` — still a placeholder; prompt 4 adds `snapshots` + `forceGc()`
- **Public API this prompt must satisfy** (from CLAUDE.md): `@Composable fun HeapMonitorOverlay(modifier: Modifier = Modifier, config: HeapMonitorConfig = HeapMonitorConfig())`
- **Upstream**: prompt 2
- **Downstream**: prompt 4 hosts this composable inside a `ComposeView` on the DecorView

---

## Task Breakdown

1. **Inspect first** — read `HeapSnapshot.kt`, `ByteFormatter.kt`, `heapmonitor/build.gradle.kts`. Do not write yet.
2. **Create `HeapMonitorConfig.kt`** in package root exactly as the CLAUDE.md contract (data class + `OverlayPosition` enum). Prompt 4 will reuse it unchanged.
3. **Create `ui/OverlayTheme.kt`** — `internal object OverlayColors` (background `0xCC121212`, text `0xFFECECEC`, ok `0xFF4CAF50`, warn `0xFFFFC107`, critical `0xFFF44336`, accent `0xFF64B5F6`) and `internal fun usageColor(percent, config)`.
4. **Create `ui/UsageBar.kt`** — thin rounded bar, fill = `usedPercent`, color from `usageColor`.
5. **Create `ui/CompactChip.kt`** — single-line pill: `"42.3/192 MB · GC 17"` + 6dp colored dot.
6. **Create `ui/ExpandedCard.kt`** — labelled rows (see Requirements), `UsageBar`, buttons row (`Force GC`, `Collapse`).
7. **Create `ui/DraggableBox.kt`** — `internal fun Modifier.draggableOverlay(offset: MutableState<Offset>, bounds: IntSize, contentSize: IntSize)` using `pointerInput` + `detectDragGestures`, clamped so the box never leaves parent bounds.
8. **Create `ui/HeapMonitorOverlay.kt`** — public entry (see Requirements). Also an `internal @Composable fun HeapMonitorOverlayContent(snapshot, config, onForceGc, modifier)` stateless variant used by tests and previews.
9. **Add `@Preview` composables** for compact + expanded with a fake snapshot (200 MB max, 84 MB used, 17 GCs).
10. **Validate**: `./gradlew :heapmonitor:assembleDebug` passes; previews render in IDE.

---

## Detailed Requirements

### `HeapMonitorOverlay` (public)
- Collects `HeapMonitor.snapshots` via `collectAsStateWithLifecycle()`. Until prompt 4 exists, reference `HeapMonitor.snapshots` anyway and add the property as `val snapshots: StateFlow<HeapSnapshot> = MutableStateFlow(HeapSnapshot.EMPTY)` plus `fun forceGc() {}` stubs in the placeholder — prompt 4 replaces the bodies.
- Wraps content in `Box(Modifier.fillMaxSize().safeDrawingPadding())` so it clears status/nav bars; content is aligned per `config.initialPosition`, then offset by drag state (`rememberSaveable` with `Offset` saver so position survives rotation).
- `remember { mutableStateOf(config.startExpanded) }` toggles compact/expanded on tap (not on drag — use `detectTapGestures` separately or check drag distance).
- Must not intercept touches outside its own bounds — the `Box` root must be `pointerInput`-free; only the chip/card consumes gestures.
- Recomposition: only the chip/card recompose on snapshot change; the drag offset uses `Modifier.offset { }` lambda form.

### `ExpandedCard` rows (label left, value right, monospace-ish `FontFamily.Monospace`)
- Heap max, Allocated (total), Used, Free (in allocated), Available (max - used), Used %
- Native alloc / Native size
- GC count (with `(+delta)` when `gcCountDelta > 0`), GC time ms, Blocking GC count
- Memory class `"192 MB (large: 512 MB)"`, suffix `" · largeHeap"` when `isLargeHeap`
- Buttons: `Force GC` (hidden when `config.showForceGcButton == false`), `Collapse`
- Card width 260dp max, 8dp corner radius, 10dp padding, text 11sp, background `OverlayColors.background`

### `CompactChip`
- Height 24dp, horizontal padding 8dp, pill shape, text 11sp, colored dot before text
- Text: `"${formatMb(used)}/${formatMb(max)} MB · GC ${gcCount}"`

### Accessibility
- Root chip/card `semantics { contentDescription = "Heap monitor overlay" }`, `testTag("heap_overlay")`; Force GC button `testTag("heap_force_gc")`; expanded card `testTag("heap_expanded")`

### Anti-goals
- No Material `Surface` elevation shadows (looks wrong over dark hosts); no animations beyond `animateContentSize()`
- No dependency on host `MaterialTheme` colors — hardcode `OverlayColors`

---

## Output Specification

| Action | File path | Contents |
|--------|-----------|----------|
| CREATE | `heapmonitor/src/main/java/com/fahim/heapmonitor/HeapMonitorConfig.kt` | config + enum |
| MODIFY | `.../HeapMonitor.kt` | add `snapshots` + `forceGc()` stubs only |
| CREATE | `.../ui/OverlayTheme.kt`, `UsageBar.kt`, `CompactChip.kt`, `ExpandedCard.kt`, `DraggableBox.kt`, `HeapMonitorOverlay.kt` | composables |

Show **only these files**.

---

## Constraints

- Do NOT call `Runtime`, `Debug`, or `HeapSampler` from any composable
- Do NOT modify `core/`, `:app`, or gradle files
- Do NOT use `collectAsState()` — use `collectAsStateWithLifecycle()`
- Do NOT request `SYSTEM_ALERT_WINDOW`; this is an in-window overlay
- Keep each file under 200 lines; composable functions under 50 lines

---

## Success Criteria

- [ ] `./gradlew :heapmonitor:assembleDebug` passes
- [ ] Previews show compact chip and expanded card with correct formatting
- [ ] Overlay can be dragged and never leaves screen bounds
- [ ] Tap toggles compact/expanded; drag does not toggle
- [ ] Update `CLAUDE.md` Prompt File Index row 3 to `[x] done`
