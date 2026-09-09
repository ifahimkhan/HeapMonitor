# Changelog

All notable changes to this project are documented here. Format follows Keep a Changelog.

## [0.1.0] - 2026-09-09

### Added
- `heapmonitor` library: draggable in-window overlay showing JVM heap (max / allocated / used /
  free / available / used %), native heap, ART GC counters (count, time, blocking, delta), memory
  class, with a colored usage bar and a Force GC button.
- Runtime debug gate: visible only in debuggable builds unless `HeapMonitorConfig(enabled = ...)`
  overrides it.
- Zero-code auto-install via `HeapMonitorInstaller` ContentProvider; manual `HeapMonitor.install()`
  path when the provider is removed.
- `heapmonitor-noop` artifact with an identical public API for `releaseImplementation`.
- Sample app with allocation / churn / release buttons and a second activity.
- Unit tests (core, gate, facade, drag clamping) and instrumented tests (overlay UI, attach lifecycle).
