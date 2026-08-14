# Gradle Isolated Projects — trial and adoption

Exercised for real on this repo (2026-08-12), not copied from Gradle's blog post
(https://blog.gradle.org/introducing-isolated-projects) or docs unverified. Numbers below are from
this machine, this build, this Gradle version — re-measure rather than trusting them blind if this
doc gets old.

## What it is, in one line

Isolated Projects lets Gradle configure subprojects in parallel by forbidding one project's build
logic from reading another project's live, mutable `Project` state during configuration — the
thing that otherwise forces configuration to happen one project at a time, in dependency order.

## Version reality check (don't trust the blog post's flag name blind)

The blog post (and `docs.gradle.org/current/...`, which resolves to the *latest* Gradle version's
docs, not necessarily the one installed) says the feature graduated to *incubating* in Gradle 9.7.0
with system property `org.gradle.isolated-projects=true` / flag `--isolated-projects`. This repo
runs **Gradle 9.6.1** (`gradle/wrapper/gradle-wrapper.properties`) — one minor version behind that
graduation. On 9.6.1:

- `--isolated-projects` as a CLI flag: **fails** — `Unknown command-line option '--isolated-projects'`.
- `-Dorg.gradle.isolated-projects=true`: silently does nothing (no error, no effect — the run
  behaves exactly as if the flag were never passed).
- **`org.gradle.unsafe.isolated-projects=true`** (system property *or* `gradle.properties` entry)
  is the correct spelling on 9.6.1 — the pre-9.7 experimental name. This is the one actually wired
  into `gradle.properties` now.

No wrapper bump was needed — the feature itself has existed since Gradle 9.0.0, just under this
older flag name pre-9.7. (The wrapper *was* bumped to 9.7.0 later the same day, for the flag's
graduated name — see "9.7.0 update and regression" below for what that actually found.)

## Process

1. **Baseline** (Isolated Projects off, current `gradle.properties` as of the start of this trial):
   5 timed trials of `./gradlew help`, each preceded by `rm -rf .gradle/configuration-cache` (not
   `--stop`-ing the daemon — that isolates the variable under test, a cold configuration phase,
   from JVM-startup noise while keeping the realistic "any build-file edit invalidates CC" case).
   `help` was picked because with no `org.gradle.configureondemand` set, Gradle configures **all**
   56 subprojects for *any* task by default — a cheap stand-in for "IDE sync / first task of the
   session" without paying for real compilation.
2. **Diagnostics pass**: `./gradlew help -Dorg.gradle.unsafe.isolated-projects=true` (no separate
   diagnostics submode was needed or found on 9.6.1 — a single run already reports every problem
   found while trying to store the configuration cache, not just the first one).
3. **Fix violations** (see below), re-running step 2 until clean.
4. **After measurement**: same 5-trial protocol as step 1, flag on. Then the five commands CI
   actually runs, once each, with the flag on, as the real safety net (`help` alone doesn't
   exercise Room/Compose/Koin-compiler-plugin/detekt/ktlint/Kover, all of which run their own
   per-project configuration logic that `help` never touches).
5. Enabled for real once both conditions held: diagnostics clean, and a real (not noise-level)
   speedup — see Results.

## Violations found and fixed

One `diagnostics` pass surfaced **90 problems (88 unique)**, all one shape:
`cannot access 'Project.file[s]/[layout]' functionality on another project ':'` — i.e. build logic
applied to a subproject reading the **root** project's mutable `Project` state. Three call sites,
each hit by nearly every module through a convention plugin:

| Site | Old | New |
|---|---|---|
| `build-logic/.../Quality.kt` `configureLinting()` (applied to every module) | `config.setFrom(rootProject.files("config/detekt/detekt.yml"))` | `config.setFrom(File(rootDir, "config/detekt/detekt.yml"))` |
| `build-logic/.../ComposeCompilerReports.kt` `configureComposeStabilityConfig()` (every Compose module) | `rootProject.layout.projectDirectory.file("config/compose/stability_config.conf")` | `layout.file(provider { File(rootDir, "config/compose/stability_config.conf") })` |
| `build-logic/.../AndroidApplicationConventionPlugin.kt` (keystore paths, `:androidApp` only) | `rootProject.file("wallos_mobile_${flavor.title}.jks")` | `File(rootDir, "wallos_mobile_${flavor.title}.jks")` |
| `benchmark/build.gradle.kts` (not a `wallosmobile.*` convention-plugin consumer, so it duplicates `configureLinting()`'s detekt block by hand — see that file's own comment) | `config.setFrom(rootProject.files("config/detekt/detekt.yml"))` | `config.setFrom(File(rootDir, "config/detekt/detekt.yml"))` |

**The fix in one line: `rootDir`, not `rootProject`.** Every `Project` already carries its build's
root directory as a plain `File` — fixed once the project tree is built from `settings.gradle.kts`,
not something that requires configuring (or even referencing) the actual root `Project` object.
`rootProject` is a live reference to another project's mutable configuration state, which is
exactly what Isolated Projects forbids reading from a subproject; `rootDir` was never that.

`ComposeCompilerGradlePluginExtension.stabilityConfigurationFiles` is a
`ListProperty<RegularFile>`, not a plain file path, so its fix needed one extra step —
`layout.file(Provider<File>): Provider<RegularFile>` — to turn the `File` into something the
property's `add(Provider<RegularFile>)` overload accepts, decompiled from the plugin jar
(`javap -classpath <compose-compiler-gradle-plugin jar> ...ComposeCompilerGradlePluginExtension`)
rather than guessed.

**What turned out fine, unchanged:** the ~50 `kover(project(":x"))` calls in root
`build.gradle.kts` aggregating every module's coverage into one report. These looked risky by the
same "reaches into another project" shape, but `project(":x")` there is dependency-declaration
notation (the standard form any `dependencies { }` block uses), not a read of another project's
mutable state — confirmed by an empty diagnostics report for it, not assumed.

## Results

5 trials each, `./gradlew help`, cold configuration cache (`rm -rf .gradle/configuration-cache`
before every trial), warm daemon:

| Trial | Before (s) | After (s) |
|---|---|---|
| 1 | 3.79 *(cold-daemon outlier — first run after warm-up)* | 0.60 |
| 2 | 1.47 | 0.51 |
| 3 | 1.68 | 0.51 |
| 4 | 1.44 | 0.60 |
| 5 | 1.43 | 0.54 |

**Median (trials 2–5, excluding the cold-daemon outlier): 1.47s → 0.54s — about 2.7x faster.**

This project isn't the giant monorepo the blog post's showcase numbers (1.3x–3.6x) come from — 56
modules, sub-2s baseline configuration — but the parallelism win was real and clearly outside
run-to-run noise (both sides' non-outlier trials cluster tight: before ±0.13s, after ±0.05s).

The five commands CI actually runs (`assembleFdroidDebug`, `assembleGplayDebug -PgplayBuild`,
`allTests`, `detekt ktlintCheck`, `koverXmlReport`) all passed clean with the flag on, both via
`-Dorg.gradle.unsafe.isolated-projects=true` explicitly and via the committed `gradle.properties`
default alone — confirming CI (which reads the committed properties file, no separate override)
will pick this up without any workflow YAML change.

## Decision (9.6.1): enabled by default — later reversed, see below

`gradle.properties` initially carried `org.gradle.unsafe.isolated-projects=true` permanently, on
9.6.1, with eyes open about the accepted risk: this is still an **incubating** feature even at its
9.7.0+ name (the blog post's own words: "not recommended for production builds yet," "more
constraints may be added in the future, including in minor releases"). The predicted failure mode
— "a future Gradle upgrade could tighten a constraint this build currently satisfies and break
configuration with no warning beyond a changelog entry" — happened the same day, on the very next
Gradle release. See below.

## 9.7.0 update and regression

Bumped the wrapper 9.6.1 → 9.7.0 the same day (`update-gradle-wrapper` skill; 9.7.0 was also
`services.gradle.org/versions/current` at the time, i.e. current latest stable, not a specific
target picked for this alone), specifically to drop the `unsafe.` flag name for the graduated
`org.gradle.isolated-projects=true`. Re-ran the same safety net that passed clean on 9.6.1:

```
Plugin 'org.jetbrains.kotlin.multiplatform': Project ':core:storage' cannot access task
dependencies directly

Could not determine the dependencies of task ':core:storage:compileAndroidMain'.
> Could not create task ':core:storage:kspAndroidMain'.
   > Project ':core:storage' cannot access task dependencies directly
```

This is inside the **Kotlin Multiplatform Gradle plugin's own KSP task-wiring code** for an Android
target (`:core:storage` is the Room module — the only one with real KSP codegen) — not a
`rootProject.*` call in this repo's own `build-logic` or module scripts. 9.6.1's Isolated Projects
implementation didn't catch this; 9.7.0's tightened checks do. This is exactly the category the
plan for the original trial called a stop condition going in: *"if a third-party plugin itself is
the source of a violation — not something fixable in our own build-logic — that's a stop condition,
not a workaround."* There is nothing in this repo to edit to fix it; it needs an upstream Kotlin
Gradle Plugin release with better Isolated Projects support for Android-target KSP task creation.

Confirmed 9.7.0 itself has no other regression: the same five-command safety net (assemble ×2,
`allTests`, `detekt ktlintCheck`, `koverXmlReport`) all passed clean on 9.7.0 with Isolated Projects
turned **off**.

## Decision (final): Gradle 9.7.0, Isolated Projects off

`gradle.properties` keeps the wrapper at 9.7.0 (worth having on its own — current latest stable,
verified clean otherwise) but the `org.gradle.isolated-projects=true` line is commented out with
the reason inline, not deleted — this was a real, measured win (§ Results above), just blocked by
someone else's plugin catching up. **Re-check on every future Gradle and/or Kotlin Gradle Plugin
version bump**: rerun the diagnostics-pass step from Process above; if `:core:storage`'s
`kspAndroidMain` violation is gone, the rest of this doc's fix set and measured numbers should still
apply unchanged, since nothing about this repo's own code caused it.

## Frictions logged

See `docs/frictions.md` for the one-line, past-tense entries from this session (the flag-name
mismatch between the blog/docs page and the installed 9.6.1 was the main one; the wrapper task's
own `--validate-url` HEAD-request failing in this sandbox despite `curl` succeeding was the other).
