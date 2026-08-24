# Android perf profiling — Perfetto, gfxinfo, Baseline Profile

Portable reference: the on-device profiling technique, written so it can be copied into another
Android/KMP project rather than re-derived. Generic adb/uiautomator technique that isn't
perf-specific (screenshot coordinate scaling, `uiautomator dump`, form filling, network toggling)
lives in the shared `emulator-testing` skill instead — this file covers frame timing and tracing.
**The `:benchmark` Baseline Profile module's own setup — the Gradle wiring, `BaselineProfileRule`
journey gotchas, and the "generated but never applied" `profileinstaller` gap — is now the shared
`android-baseline-profile` skill** (`agentic-grappim`, symlinked at
`~/.claude/skills/android-baseline-profile`); this file keeps only what's confirmed true for
*this* project's own module.

Everything below was exercised for real on this project (`docs/issues/2026-08-09-fab-open-and-
list-scroll-jank.md`, `docs/issues/2026-08-10-editor-open-stall-and-unapplied-profile.md`, M13 —
`docs/archive/CHECKLIST-DONE.md` 13.1/13.2), not copied from documentation unverified. Package
ids, module paths and version numbers below are WallosMobile's own — swap them for the target
project's.

## Two questions, two tools

- **"Is screen A slower than screen B, and by how much?"** → `dumpsys gfxinfo` / `framestats`.
  Zero setup, per-frame nanosecond timestamps, no Python needed. Start here.
- **"*Why* is it slow — what is the CPU actually doing during the slow frames?"** → Perfetto.
  Real setup cost (capture, pull, a Python trace processor), but it names the actual thread,
  slice, and lock behind a stall instead of just its duration.

Neither tool tells you what a screenshot can't: never time an in-flight state by racing a `sleep`
against real network latency (10ms–700ms+ jitter, plus adb's own dispatch overhead on top) — cut
connectivity before the action instead so the state holds open deterministically. That trick and
the rest of the screenshot-verification technique live in the `emulator-testing` skill, not here.

## Quick look: `dumpsys gfxinfo`

```bash
adb shell dumpsys gfxinfo <package-id> reset
adb shell input tap X Y            # or whatever action is under test
adb shell dumpsys gfxinfo <package-id> framestats > out.txt
```

Despite the `framestats` argument, the payload is a `---PROFILEDATA---` block: a CSV header
followed by one row per frame with real nanosecond timestamps (`HandleInputStart`,
`AnimationStart`, `PerformTraversalsStart`, `DrawStart`, `FrameCompleted`, …).
`FrameCompleted − HandleInputStart` on the last row is a real tap-to-settled time for that
screen — cheap enough to run for both sides of an "A feels slower than B" question before
touching any code.

**Gotcha:** the last row in a short capture can carry stale ring-buffer values — a
`FrameCompleted`/`GpuCompleted` timestamp far smaller than the row's own `SwapBuffers`, a dead
giveaway. Use `SwapBuffersCompleted` instead when that happens.

**Real-device gotcha (confirmed on a Samsung Galaxy A9, Android 10):** `dumpsys gfxinfo` works
fine even where the Perfetto app-level categories below silently don't. If a device's Perfetto
capture comes back with only kernel slices, fall back to this rather than assuming the device
is untraceable.

## Deep look: Perfetto

### Capture

```bash
adb shell perfetto -o /data/misc/perfetto-traces/t.perfetto-trace -t 8s \
  sched freq idle am wm gfx view input dalvik hal res memory binder_driver &
sleep 1.5   # let tracing actually start before the action under test
adb shell input tap X Y            # or whatever action is under test
sleep 5     # >= the -t duration minus the head start, or the pull races the writer
adb pull /data/misc/perfetto-traces/t.perfetto-trace
```

**Don't pull the instant your own sleeps sum to `-t`'s duration** — the on-device process needs
a moment past that to flush and close the file. A pull that races it silently grabs a 0-byte
file with no error. Check the file size (or `adb shell ps -A | grep perfetto` — still running
means still writing) before trusting a pull, and re-pull once it's actually stopped.

**Confirmed real-device gap: kernel categories work, app-level ones can silently produce
nothing.** On a real Samsung device (Android 10), the exact category list above captured only
`sched`/`binder_driver`-level slices — the app-level categories that carry
`Choreographer#doFrame`, JIT lock-contention, and library-internal markers (`view`, `gfx`,
`dalvik`) produced zero slices, with no error from either `perfetto` or
`atrace --list_categories`. This didn't happen on the project's AVD. If app-level slices are
silently missing from a real-device trace, that's the known failure mode — fall back to
`dumpsys gfxinfo` (above) rather than debugging the capture further.

### Analysis

```bash
python3 -m venv .venv && source .venv/bin/activate   # bare `pip install` refuses as
pip install perfetto                                  # "externally managed environment" on most distros
```

```python
from perfetto.trace_processor import TraceProcessor
tp = TraceProcessor(trace='t.perfetto-trace')
```

The first `TraceProcessor(...)` call downloads a `trace_processor_shell` binary from Google over
the network — needs connectivity once, then it's cached.

**`QueryResultIterator.as_pandas_dataframe()` fails with "pandas/numpy dependency missing"**
even in a venv with only `perfetto` installed — that package doesn't pull them in transitively.
Iterate the query result directly instead; it needs neither and is enough for count/sum/max-style
aggregate queries:

```python
for row in tp.query("select ..."):
    print(row.some_column)
```

Two schema facts that aren't obvious from the table names:

- **The main thread's `name` is truncated to 15 chars by the Linux `comm` field and is usually
  *not* `"main"`** — it's the tail of the package name instead (e.g. `losmobile.debug` for
  `com.grappim.wallosmobile.debug`). Filter `thread` by `tid = <pid>` (the main thread's tid
  always equals the process pid), not by name.
- **`thread_state.state = 'S'` (sleeping) and `'Running'` mean opposite things for a "why is
  this slow" question, and only one of them is a code bug.** A long `Running` span is the thread
  genuinely busy computing something — a real stall, and the overlapping `slice` rows (join on
  `thread_track.utid`) name what it was doing. A long `S` span is the thread idle, blocked on
  I/O or simply out of work — normal while waiting on a network response, not evidence of a
  blocking bug no matter how long it lasts. Check `state` before concluding a gap in rendered
  frames means the main thread is stuck; it usually means the opposite.

Useful starting queries (table/column names are Perfetto's standard schema, not project-specific):

```sql
-- Janky frames and their worst offenders (needs the `gfx`/`view` categories captured)
select ts, dur, jank_type from actual_frame_timeline_slice where jank_type != 'None' order by dur desc;

-- What was a given thread doing during a time window
select s.ts, s.dur, s.name
from slice s join thread_track tt on s.track_id = tt.id join thread t on tt.utid = t.utid
where t.tid = <pid> and s.ts between <start_ns> and <end_ns>
order by s.dur desc;

-- Lock contention on a specific object (e.g. a third-party library's own internal lock)
select count(*), sum(dur) from slice where name like 'monitor contention%<ClassName>%';
```

## Baseline Profile / Macrobenchmark (`:benchmark` module)

Generic setup technique lives in the `android-baseline-profile` skill now — read it for the
Gradle-wiring gotchas (AGP-compat version pinning, the `apply false` classloader trap, flavor
mirroring), the `BaselineProfileRule` journey gotchas (encrypted login gate, no data clear
between test iterations), and the "generated but never applied" `profileinstaller` step. What
follows is only what's confirmed true for *this* project's own module.

- **Version pin: `androidx.benchmark`/`androidx.baselineprofile` `1.5.0-beta01`.** `1.4.1`
  (documented-stable at the time) fails to apply against this project's AGP (`Module :app is not
  a supported android module`) — re-check this pin before assuming it still holds whenever
  `gradle/libs.versions.toml`'s `agp` entry is bumped.
- **Module**: `benchmark/build.gradle.kts`, `targetProjectPath = ":androidApp"`, mirroring
  `androidApp`'s `gplay`/`fdroid` `STORE` flavor dimension.
- **Generate**: `./gradlew :androidApp:generateGplayReleaseBaselineProfile` — writes
  `androidApp/src/gplayRelease/generated/baselineProfiles/baseline-prof.txt`, committed as
  source (see CLAUDE.md's Build commands). The assembled release APK embeds the compiled form at
  `assets/dexopt/baseline.prof`/`.profm` (confirmed via `unzip -l`).
- **`androidx.profileinstaller` `1.4.1`** is a runtime dependency of `androidApp` (all flavors)
  — without it, `fdroid`'s direct-APK distribution never applies the generated profile at all.
  `debug` builds never carry one regardless: the file only exists in the `gplayRelease` source
  set.
- **Measured effect (real device, editor-open journey)**: worst frame 150ms → 117ms (~22%) once
  the unapplied-profile gap was fixed — matching the ~109ms a manual
  `adb shell cmd package compile -m speed-profile -f` override found, confirming the mechanism
  rather than just the forced override. The same fix did **not** move the separate scroll-jank
  number traced in the same investigation — full before/after numbers in M13
  (`docs/archive/CHECKLIST-DONE.md`) and `docs/issues/2026-08-09-fab-open-and-list-scroll-
  jank.md` / `docs/issues/2026-08-10-editor-open-stall-and-unapplied-profile.md`.
