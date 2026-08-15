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
- `./gradlew :androidApp:installFdroidDebug` failed with `INSTALL_FAILED_UPDATE_INCOMPATIBLE:
  ... signatures do not match` against a prior session's install of the same package id on the
  same AVD — `adb uninstall <package-id>` first, then reinstall, is the fix; no need to chase
  which signing config actually changed.
- `./gradlew :androidApp:assembleFdroidDebug` (16.5's own `Verify:` line, run literally) failed at
  `packageFdroidDebug` with `SigningConfig "fdroidDebug" is missing required property
  "storePassword"` — `WALLOS_STORE_PASS_FDROID_DEBUG`/`WALLOS_ALIAS_FDROID_DEBUG`/
  `WALLOS_KEY_PASS_FDROID_DEBUG` aren't set in this shell session's environment. `compileFdroidDebugKotlin`
  itself succeeds; only packaging needs them. Confirmed pre-existing (fails identically on a clean
  `git stash`'d tree) rather than caused by the step's own changes — worth a `git stash -u` +
  re-run check before assuming a `Verify:` command failure is new.
- `./gradlew :androidApp:installGplayDebug -PgplayBuild`'s first run landed on a stray physical
  device (`SM-A920F`, connected over USB) instead of the intended AVD — no emulator was booted yet,
  so it was the only device `adb`/the install task saw. `adb devices -l` before trusting an install
  reached the AVD (the `emulator-testing` skill's own standing warning) caught it before driving
  unfamiliar hardware any further.
- 16.5's first `MainActivity.kt` built `appUpdateChecker.updateState.filterIsInstance<>().map { }`
  directly inside `setContent { ... }`'s composable lambda — compiled clean, `detekt`/`ktlintCheck`
  both passed, and only Android Studio's own Compose lint (`FlowOperatorInvokedInComposition`)
  caught that this builds a *new* `Flow` object every recomposition. Neither Gradle gate runs
  Compose-specific lint; the fix (hoist the `.filterIsInstance()`/`.map()` call out of the
  composable, into a `val` computed once in `onCreate` before `setContent`) needed a human/IDE
  flag, not a `Verify:` line, to surface at all.
- `./gradlew :core:storage:compileKotlinAndroid` failed with "task 'compileKotlinAndroid' not found"
  — this KMP setup has no such task name; `:core:storage:testAndroidHostTest` (which compiles the
  module as a dependency) is the fast way to check a module compiles without guessing the exact
  Android compile task name. Same guess repeated against `:testing:compileGplayDebugKotlin`, which
  also doesn't exist — `:testing` declares no flavors, so per-flavor compile tasks aren't a thing
  there either.
- `adb shell perfetto ... sched freq idle am wm gfx view input dalvik hal res memory
  binder_driver` (the `emulator-testing` skill's own Step 4b recipe) silently captured only
  kernel-level categories (`sched`, `binder_driver`) on a real Samsung device (`SM-A920F`, Android
  10) — the app-level categories (`view`, `gfx`, `dalvik`) that carry `Choreographer#doFrame`, JIT
  lock-contention, and Coil disk-cache markers produced zero slices, no error either from
  `perfetto` or `atrace --list_categories`. `dumpsys gfxinfo`/`dumpsys gfxinfo ... reset`, which
  doesn't depend on the same OS mechanism, worked fine and was the fallback (full findings in
  `docs/issues/2026-08-09-fab-open-and-list-scroll-jank.md`'s 2026-08-12 addendum).
- `adb reverse tcp:8282 tcp:8282` silently stopped forwarding partway through a session (`adb
  reverse --list` came back empty with no error or disconnect event) — the app's own "Couldn't
  reach that server" screen was the first symptom. Re-running the same `adb reverse` command fixed
  it instantly; worth checking `adb reverse --list` before assuming a sudden network error is the
  app's fault mid-session.
- `dumpsys gfxinfo <pkg> reset` prints the *previous* window's accumulated stats before clearing
  them, not silence — running `reset` then `framestats` in the same shell session produces two
  visually similar "Stats since" blocks (one from each command), and it's easy to read the `reset`
  call's own stdout as the just-captured measurement instead of the real one in the `framestats`
  file. The authoritative numbers are always in the `framestats` output, never the `reset` echo.
- `adb shell run-as <pkg> ...` fails with `run-as: package not debuggable` against a signed release
  build — there's no way to reach a release app's own data dir this way to clear just its Coil
  cache. `adb shell pm clear <pkg>` (wipes everything, including login) followed by re-login was
  the only working substitute for repeating a cold-cache measurement on a release build.
- `docs.gradle.org/current/...` resolves to the *latest* Gradle version's docs, not the installed
  9.6.1's — its Isolated Projects flag names (`--isolated-projects` CLI flag,
  `-Dorg.gradle.isolated-projects=true`) are 9.7.0+ only. On 9.6.1 the CLI flag fails outright with
  `Unknown command-line option`, but the system property silently no-ops with zero error or output
  difference — only `org.gradle.unsafe.isolated-projects=true` (the pre-9.7 experimental name)
  actually enables it. Confirm a flag/property against the *installed* version's own docs
  (`docs.gradle.org/9.6.1/...`) before trusting the "current" page, especially when it fails silent
  rather than loud.
- `./gradlew wrapper --gradle-version 9.7.0 ...` failed with `Test of distribution url ... failed`
  / `HEAD request ... failed: response code (-1)` / `Unexpected end of file from server` in this
  sandboxed environment, even though `curl -I` against the exact same URL succeeded fine —
  `--no-validate-url` was the workaround, restoring `validateDistributionUrl=true` in
  `gradle-wrapper.properties` by hand afterward since it's a one-time generation-time check
  the committed file shouldn't carry disabled permanently.
- Pointing `build-logic`'s new `detekt {}` block at the shared `config/detekt/detekt.yml` (21.2,
  to keep its ruleset consistent with every other module) failed with `Property 'Compose' is
  misspelled or does not exist` — that config section is only valid with the
  `io.nlopez.compose.rules:detekt` plugin on the classpath, which a Kotlin-DSL build-logic project
  has no reason to carry. Reverted to detekt's own default ruleset for `build-logic` instead.
- `import org.jetbrains.uast.UElementHandler` in a new `lint-rules` `Detector` failed as an
  unresolved reference (25.1) — the real package is `com.android.tools.lint.client.api
  .UElementHandler`; `org.jetbrains.uast` only has the UAST node types, not the visitor-dispatch
  helper. Also, `context.report(...)`/`context.getLocation(...)` overload resolution on a
  `UParameter` (implements both `PsiElement` and `UElement`) is ambiguous without an explicit
  `node as UElement` cast at the call site.
- A new `lint-rules` module's `lint()` test task (`TestLintTask`, `lint-tests` artifact) failed
  every test with `This test requires an Android SDK: No SDK configured` (25.1) — the fixtures are
  plain Kotlin with no real Android dependency, so `.allowMissingSdk()` on the task builder was the
  fix, not pointing `sdkHome()` at a real SDK.
- Applying `alias(libs.plugins.jetbrains.kotlin.jvm)` directly in a new subproject's
  `build.gradle.kts` (25.1's `lint-rules`, the project's first non-KMP, non-`build-logic` Kotlin/JVM
  module) failed with "plugin is already on the classpath with an unknown version" until the same
  plugin was also added as `apply false` in the root `build.gradle.kts`'s `plugins {}` block — the
  comment already there ("necessary to avoid the plugins to be loaded multiple times in each
  subproject's classloader") turned out to apply to any plugin a *second* subproject reaches for,
  not just the ones already listed.
- Applying `dev.detekt`/`org.jlleitschuh.gradle.ktlint` with no `config.setFrom` inside a **normal
  subproject** of this build (25.1's `lint-rules`) still auto-discovered the shared root
  `config/detekt/detekt.yml` and failed on its `Compose:` section — unlike `build-logic`, which
  gets away with the same bare-plugins shape only because it's a *separate* included build with its
  own `rootDir`, so the auto-discovery never finds this build's config file at all. A same-build
  subproject needs the real `config.setFrom` + `composeRules-detekt`/`composeRules-ktlint`
  dependencies, the same as every KMP module's `configureLinting()`.
- `lint.abortOnError = false` (`build-logic`'s `KotlinConfiguration.kt`, set in the project's very
  first commit, 0.2/0.3) meant `lintFdroidDebug`/`lintGplayDebug` had never once failed a build on
  a real lint `ERROR` — confirmed by planting a violation, watching it appear in the HTML/SARIF
  report at `ERROR` severity, and watching the Gradle task still exit 0. 21.1 wired these tasks
  into CI believing they were a real gate; they weren't, for anything, the whole time. Fixed by
  flipping to `true` (25.1), confirmed clean against the real codebase first.
- A `lintChecks(project(":lint-rules"))` dependency declared on a *consuming* module (`androidApp`)
  does not reach findings in a *dependency* module's own source (25.1) — a violation planted in
  `feature:subscriptions:ui`'s `commonMain` never appeared in `androidApp:lintFdroidDebug`'s report
  even after wiring `lintChecks` into every module via `configureLinting()` and trying
  `checkDependencies` both `true` and `false`. Root cause: under AGP 9.3.1, a
  `com.android.kotlin.multiplatform.library` module exposes only a `lintAnalyzeAndroidHostTest`
  task, no task that lints its own `androidMain`/`commonMain` production source at all — there is
  currently nothing to propagate. `docs/revisit.md` #1 tracks a real fix.
- `gh pr edit 14 --body ...` failed with `GraphQL: Projects (classic) is being deprecated ...
  (repository.pullRequest.projectCards)` on this repo (gh 2.45.0) — the mutation path it uses
  queries a deprecated Projects-classic field unrelated to the edit itself. `gh api
  repos/<owner>/<repo>/pulls/<n> -X PATCH -f body=...` (plain REST) worked around it.
- First pass at narrowing `check-guardrails.sh`'s `gradle/libs.versions.toml` wire to
  gate-relevant keys silently passed on a synthetic `detekt` version bump it should have caught —
  the diff-line regex anchored `^detekt` against a line that still carried its leading `+`/`-`
  from `git diff`, so it never matched. A check that confidently returns "nothing tripped" reads
  identical to a check that's broken; caught only by testing the case it's supposed to catch
  (a synthetic gate-relevant bump), not just the case it's supposed to let through.
- After PR #15 (the libs.versions.toml narrowing) merged, Renovate's own `renovate/filekit`
  branch failed guardrails again — a *third* flavor of the same root problem, this time a real
  (not synthetic) merge commit: Renovate merges the base branch into its own branch to catch up
  rather than rebasing, and `git merge dev`'s resulting commit, diffed against its first parent
  only (the script's per-commit model), shows the *entirety* of what dev gained since the branch
  point as "touched" — nine unrelated files here. Fixed with `git rev-list --no-merges`, so only
  commits that actually introduce content are walked.
- `testImplementation("dev.detekt:detekt-test:2.0.0-alpha.5")` in a new `detekt-rules` module
  (26.1) failed dependency resolution outright ("No matching variant ... with capability
  'dev.detekt:detekt-api-test-fixtures' was found") — `detekt-test`'s `runtimeElements` variant
  requests that capability from `detekt-api`, but `detekt-api` only ever publishes a *sources* jar
  under it, confirmed by fetching and reading both `2.0.0-alpha.5`'s and `2.0.0-alpha.6`'s
  `.module` metadata directly from Maven Central — a real upstream publishing gap, not something a
  version bump fixes. `testImplementation(libs.detekt.test) { isTransitive = false }` plus the
  module's own real transitive needs declared by hand (`detekt-api`, `detekt-test-utils`,
  `kotlin-compiler:2.4.0`, `kotlin-reflect:2.4.0`) worked around it.
- `ownerFunction.containingClassOrObject` (a new detekt `Rule`, 26.1, `ownerFunction` smart-cast to
  `KtPrimaryConstructor`) failed as "Unresolved reference" despite `KtConstructor.kt`'s own source
  (fetched from `kotlin-compiler:2.4.0`'s `-sources.jar`) clearly declaring
  `abstract fun getContainingClassOrObject(): KtClassOrObject` on that type — Kotlin's
  Java-interop synthetic-property sugar for a `getFoo()` method only applies when the declaring
  class originates from Java bytecode, not when calling from Kotlin into another Kotlin class.
  `ownerFunction.getContainingClassOrObject()` (explicit method call) is what actually compiles.
- A new `androidDeviceTest` calling `performClick()` on a row found via `onNodeWithText` (27.4,
  `SubscriptionEditorContent`'s notify `SwitchRow`) failed with `expected:<false> but was:<null>` —
  the callback never fired, no exception. `performClick()` dispatches a real on-screen touch, not a
  semantics action, and the row sits below the fold in this long scrollable form; `assertIsOn()` on
  the same node passed fine just before it, since a semantics assertion reads state regardless of
  what's actually rendered on screen. `performScrollTo()` before `performClick()` fixed it.
