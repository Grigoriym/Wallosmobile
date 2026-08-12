# Android perf profiling — Perfetto, gfxinfo, Baseline Profile

Portable reference: the on-device profiling technique and the `:benchmark` Baseline Profile
module, written so both can be copied into another Android/KMP project rather than re-derived.
Generic adb/uiautomator technique that isn't perf-specific (screenshot coordinate scaling,
`uiautomator dump`, form filling, network toggling) lives in the shared `emulator-testing` skill
instead — this file only covers frame timing, tracing, and the Baseline Profile pipeline.

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

A `com.android.test` macrobenchmark producer module that generates a Baseline Profile — a list
of hot classes/methods AOT-compiled at install time instead of waiting for the JIT to warm up
cold-navigation and scroll paths. This is the fix for a "first scroll/first navigation is janky,
the second one isn't" finding from the tracing above (JIT lock-contention slices in the trace are
the tell).

### Why it's wired by hand, not through this project's own convention plugins

It's the one module type that is neither KMP nor a shared-convention-plugin consumer — a plain
Gradle Android Test module. Concrete version pin and gotchas (confirmed live, not from docs):

- **`androidx.benchmark`/`androidx.baselineprofile` 1.4.1 (documented-stable at the time) does
  not work against AGP 9.3.1** — fails to apply with `Module :app is not a supported android
  module`. Its module-detection logic predates AGP 9; the docs' own claim of support "up to AGP
  9.0.0-alpha01" was accurate, not conservative. **`1.5.0-beta01` configures and runs cleanly.**
  Re-check this pin whenever AGP is bumped.
- **`com.android.test` and `androidx.baselineprofile` both need `alias(...) apply false` in the
  root `build.gradle.kts`'s plugin-dedup block**, next to `com.android.application`. Without it,
  applying either from the module's own `plugins {}` block with an explicit version fails
  `already on the classpath with an unknown version` — any subproject that applies
  `com.android.application` already puts every AGP plugin class (including `com.android.test`'s)
  on the shared classloader, and a second, versioned request for an already-loaded class can't
  be compatibility-checked.
- **If the target app has product flavors, the benchmark module needs a matching flavor
  dimension of its own**, by name (a plain subproject script can't import the target app's
  Kotlin flavor-declaration objects the way an in-repo convention plugin can). Left unflavored,
  the target app's flavor-qualified release variants become ambiguous to this module's own
  unflavored one — `generateXBaselineProfile` fails with a Gradle variant-attribute-ambiguity
  error between the flavors' `RuntimeElements`.
- **A convention plugin's `configureLinting()`-style helper isn't callable from a plain
  subproject script** — the import that resolves inside a compiled `build-logic` doesn't resolve
  from a bare module's `build.gradle.kts`. detekt/ktlint need to be configured by hand there
  instead, including any Compose-lint ruleset dependency the shared detekt config requires — if
  the shared config has a `Compose:` section, it's invalid without that plugin present in *every*
  module that runs detekt against it, regardless of whether the module has any Compose code.
- **Generation runs against a real connected device** (`baselineProfile { useConnectedDevices =
  true }`) — reuse whatever AVD/device is already used for other on-device verification rather
  than provisioning a separate Gradle Managed Device.

### Gradle shape (`benchmark/build.gradle.kts`)

```kotlin
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.example.app.benchmark"
    compileSdk = /* … */

    defaultConfig {
        minSdk = /* … */
        targetSdk = /* … */
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"   // the module under benchmark

    // Only needed if the target app has flavors — mirror its dimension/flavor names.
    flavorDimensions += "STORE"
    productFlavors {
        create("gplay") { dimension = "STORE" }
        create("fdroid") { dimension = "STORE" }
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
```

`gradle/libs.versions.toml`:

```toml
androidxBenchmark = "1.5.0-beta01"   # also versions the androidx.baselineprofile plugin
androidx-benchmark-macro-junit4 = { module = "androidx.benchmark:benchmark-macro-junit4", version.ref = "androidxBenchmark" }
androidx-baselineprofile = { id = "androidx.baselineprofile", version.ref = "androidxBenchmark" }
android-test = { id = "com.android.test", version.ref = "agp" }
```

### The generator itself

`BaselineProfileRule` drives real UI journeys through `MacrobenchmarkScope` (uiautomator
under the hood) and records which classes/methods get touched. One `@Test` per journey worth
its own profile coverage — cold start plus whatever screen the tracing pointed at as JIT-cold:

```kotlin
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun coldStart() = baselineProfileRule.collect(packageName = TARGET_PACKAGE) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun listFling() = baselineProfileRule.collect(packageName = TARGET_PACKAGE) {
        pressHome()
        startActivityAndWait()
        openList()
        repeat(3) { device.swipe(540, 2000, 540, 300, 150) }
    }
}
```

Gotchas found writing this project's own version:

- **If a login/auth screen gates the journey and the stored credential is encrypted
  (Android Keystore or similar), a DataStore/SharedPreferences-planting trick to seed it won't
  work** — a planted value that doesn't decrypt just reads as "nothing stored," not a crash.
  Log in once by hand on the same device before running the generator instead.
- **`BaselineProfileRule` kills the process between iterations but does not clear app data** —
  a manually seeded login survives every `@Test` within one Gradle invocation, but the target
  app is uninstalled at the end of every `connectedXInstrumentedTest` run, pass or fail. Re-login
  is needed before every fresh generator invocation, not just once ever.
- **A journey that depends on a view appearing (e.g. clicking a button whose text/icon only
  renders once some async state resolves) needs an explicit `device.wait(Until.hasObject(...))`
  before interacting with it** — `startActivityAndWait()` only waits for the window to go idle,
  not for the app's own first-frame async state. A journey that happens to always land on a fast
  frame will pass without this wait; a slower cold-JIT frame will make `findObject` return null
  and the test flaky, not reliably failing.

### Running it, and where the output lands

```bash
./gradlew :app:generateXReleaseBaselineProfile
```

writes `app/src/xRelease/generated/baselineProfiles/baseline-prof.txt`, committed as source (not
gitignored — a release build reads it from there without regenerating). The assembled release
APK embeds the compiled form at `assets/dexopt/baseline.prof` + `.profm` (confirmed via
`unzip -l`).

**This task is a real `connectedAndroidTest` run** — it boots instrumentation, installs both the
target app and the test APK, and drives the device. It can take several minutes; run it under a
background task rather than a short foreground timeout, or a slow run gets silently cancelled
with no result printed.

## Making sure the profile is actually applied

Generating the file is necessary but **not sufficient** — a plain `adb install` (or a
non-Play-Store install path, e.g. an F-Droid-style flavor) does not automatically apply Baseline
Profile compilation. This bit a real measurement here: a same-day "is it fixed" re-check found
FAB-open and list-open journeys indistinguishable, but the installed APK's own dexopt status
(`adb shell dumpsys package <package-id> | grep status`) read `[status=verify] [reason=install]`
— the profile was compiled into the APK but never applied to the installed copy, and the earlier
"fixed" result was actually measuring an unoptimized install.

**Fix: add `androidx.profileinstaller` as a runtime dependency of the app module.**

```toml
profileinstaller = "1.4.1"
androidx-profileinstaller = { module = "androidx.profileinstaller:profileinstaller", version.ref = "profileinstaller" }
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(libs.androidx.profileinstaller)
}
```

Its bundled `ProfileInstallerInitializer` (an `androidx.startup` initializer, no manual wiring)
fires automatically on first launch after install, and the system's background dexopt job then
compiles the profile in — confirmed via `adb logcat` for the initializer firing, and
`dumpsys package` moving to `[status=speed-profile] [reason=bg-dexopt]`. Real devices run that
job on idle+charging; force it immediately for verification instead of waiting:

```bash
adb shell cmd package bg-dexopt-job
adb shell dumpsys package <package-id> | grep status   # re-check after this
```

Measured effect on this project (real device, editor-open journey): worst frame 150ms → 117ms,
a ~22% reduction, matching the ~109ms a manual `adb shell cmd package compile -m speed-profile -f`
override found — confirming the mechanism, not just the forced override. A *different* journey's
aggregate frame-jank number (scroll) did **not** improve from the same fix — the profile-
application gap explains and fixes the mechanism it targets, not every perceived-slowness finding
in the same investigation. Don't assume one fix explains every symptom just because both were
found in the same trace session.

## Summary checklist for porting this to a new project

1. `dumpsys gfxinfo`/`framestats` — works immediately, no Gradle changes. Use first.
2. Perfetto capture + `TraceProcessor` — needs a venv (`pip install perfetto`), no Gradle
   changes. Use when gfxinfo says "slow" but not "why."
3. If tracing finds JIT lock-contention on cold navigation: add a `:benchmark`-style
   `com.android.test` module, pin `androidx.benchmark`/`androidx.baselineprofile` against
   whatever AGP version the project runs (check the module actually applies before trusting a
   "stable" version number against a recent AGP), write one `@Test` per cold journey the trace
   flagged.
4. Add `androidx.profileinstaller` to the app module regardless of Play Store distribution —
   without it, a non-Play install path never applies the profile at all, and a Play-only install
   path applies it later than a cold-launch initializer would.
5. Re-verify with `dumpsys package | grep status` before trusting any "is it fixed" re-measurement
   — an unapplied profile silently produces the same install as no profile at all.
