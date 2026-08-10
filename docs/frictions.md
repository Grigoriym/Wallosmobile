# Frictions

Tooling friction hit during work, newest last. One line each. Promoted or fixed entries get
deleted — see `/finalize`.

- `pip install perfetto` failed with a PEP 668 "externally managed environment" error on this
  machine's Python — a venv (`python3 -m venv`) was needed, `--break-system-packages` was not tried.
- Backgrounding `adb shell perfetto -o ... -t 8s ... &` inside a `(... &)` subshell and pulling the
  trace after the script's own `sleep`s summed to ~8s grabbed a 0-byte file — the on-device capture
  was still running past the subshell's own exit; re-pulling once `adb shell ps -A | grep perfetto`
  showed it had actually finished got the real (1.2MB) file.
- Timing an `adb shell input tap X Y &` plus a fixed `sleep` to screenshot an in-flight loading state
  repeatedly missed the window — real server latency for the same endpoint varied ~10ms to ~700ms
  run to run, and `adb`'s own shell-dispatch overhead added further unaccounted jitter on top; cutting
  connectivity before the tap (`airplane-mode enable` + `svc wifi disable`) forced a deterministic,
  wide window instead of racing a moving target.
- `docker exec wallos grep -rn ... --include=*.php` failed with "unrecognized option" — the
  container's `grep` is BusyBox's, which has no `--include`; `grep -rln <pattern> <path>` (no flag,
  just a bare recursive search) is what works there.
- `LocalDate.monthNumber` (kotlinx-datetime 0.8.0) warns deprecated in favour of `.month.number`,
  but `Month` in this pinned version has no `.number` member — `javap` on the cached
  `kotlinx-datetime-jvm-0.8.0.jar` confirmed it isn't there; `monthNumber` is what actually compiles
  at this version and the warning is unactionable until the dependency bumps.
- `import kotlinx.datetime.Clock` for `Clock.System.todayIn(...)` failed with "Unresolved reference
  'System'" — kotlinx-datetime 0.8.0 deprecated `Clock` to a typealias for `kotlin.time.Clock`, and
  the deprecated typealias doesn't carry the `System` companion property; `import kotlin.time.Clock`
  is what actually compiles at this version.
- `perfetto` Python package's `QueryResultIterator.as_pandas_dataframe()` failed with "pandas/numpy
  dependency missing" even inside a fresh venv with only `pip install perfetto` run — that package
  doesn't pull pandas/numpy transitively; iterating the `TraceProcessor.query(...)` result directly
  (`for r in tp.query(...)`) needs neither and was simpler than adding two more dependencies.
- Eyeballing a scaled screenshot to compute a FAB tap's real-pixel coordinates landed on the wrong
  list row entirely (a background subscription card, not the FAB) with no error — `adb shell
  uiautomator dump` and reading the target's own `bounds="[x1,y1][x2,y2]"` (already real-pixel) is
  what the skill recommends for exactly this, and skipping it once cost a wasted Perfetto capture.
- `./gradlew :androidApp:generateGplayReleaseBaselineProfile` (a `connectedAndroidTest`-backed task
  that boots the AVD's instrumentation, installs two APKs, and runs `BaselineProfileRule` against a
  real device) ran past a 590s foreground `timeout` with no result printed — running it under
  `run_in_background` plus a `Monitor` watching for `BUILD SUCCESSFUL|FAILED` from the first attempt
  would have avoided the wasted, silently-cancelled run.
- 15.3's `signingConfigs` step generated throwaway test keystores under a guessed filename
  (`wallosmobile_keystore_<flavor>_release.jks`) to verify the Gradle wiring, without first
  checking whether the user already had real ones — they did, under different names
  (`wallos_mobile_<flavor>.jks`), created via Android Studio. `find . -iname "*.jks"` before
  generating anything would have caught this for free.
