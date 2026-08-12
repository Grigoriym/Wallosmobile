# Compose Compiler stability reports — plan

Ported from `TaigaMobileNova/docs/compose/` (2026-08-12, same day that project ran its own
audit) — same tool, same reasoning (Strong Skipping Mode makes every composable skippable, but an
unstable parameter still falls back to referential (`===`) comparison, so a data class with one
unstable field silently recomposes every time even when unchanged). This project already follows
the mitigating convention (CLAUDE.md's Compose rules: `ImmutableList`/`persistentListOf()` over
`List` in state classes and Composable params) — this plan is about getting the compiler's own
verification of that, not re-arguing the convention.

This is a diagnostic tool, opt-in, zero cost to a normal build — same shape as `-PgplayBuild`.

## Status

| # | Task | Size | Status |
|---|------|------|--------|
| 1 | Gradle wiring: opt-in stability reports | S | Done (2026-08-12) |
| 2 | Aggregator script + first repo-wide audit + doc | M | Not started |
| 3 | Fix whatever task 2's audit finds, scoped after the fact | ? | Not scoped — depends on task 2's findings |

## Researched facts (so task 1 doesn't have to re-derive them)

- **Same Kotlin/Compose-compiler version as TaigaMobileNova (`2.4.10`)** — confirmed via
  `gradle/libs.versions.toml`'s `kotlin` entry matching exactly. Taiga's own task 1 decompiled
  `compose-compiler-gradle-plugin-2.4.10-gradle813.jar` to confirm the
  `ComposeCompilerGradlePluginExtension` shape (`metricsDestination`/`reportsDestination`:
  `DirectoryProperty`, `stabilityConfigurationFiles`: `ListProperty<RegularFile>` — plural, the
  singular form is deprecated, `targetKotlinPlatforms`: `SetProperty<KotlinPlatformType>`). Same
  artifact, same API — no need to re-decompile here.
- **The plugin registration point is identical in shape**: `apply("org.jetbrains.kotlin.plugin.compose")`
  in both `KmpLibraryComposeConventionPlugin.kt:18` and `AndroidApplicationConventionPlugin.kt:21`
  — the same two call sites Taiga's task 1 hooked.
- **The one thing that's genuinely different, and simpler: WallosMobile has no `jvm()` target at
  all.** `configureKmp()` (`KmpConfiguration.kt`) adds no platform targets beyond what
  `com.android.kotlin.multiplatform.library` declares itself (CLAUDE.md's own Non-negotiables:
  Android-only, and 19.1's own `Note:` already hit this same fact for a different feature). Taiga's
  entire task 1 "Result" deviation — discovering `targetKotlinPlatforms` can't be used to restrict
  reports to one target because it disables Compose's bytecode transformation outright, and having
  to solve 4x-duplicate reports operationally instead (only invoke the `jvm`-target task) — **does
  not apply here**. Every module in this project compiles exactly one target
  (`compileAndroidMain`, confirmed via `./gradlew :feature:subscriptions:ui:tasks --all`), so there
  is no duplication to avoid and no `targetKotlinPlatforms` question to get wrong. Task 1's
  `configureComposeStabilityReports()` can be a straight port of Taiga's *final* (post-fix) version
  — no parameter, no `restrictToJvm` — with no equivalent research detour needed.
- **Module list — every module with `alias(libs.plugins.wallosmobile.kmp.library.compose)`**
  (confirmed via `grep -rl "wallosmobile.kmp.library.compose" --include="build.gradle.kts" .`,
  2026-08-12): `composeApp`, `core/navigation`, `strings`, `uikit`, `utils/ui`,
  `feature/categories/ui`, `feature/currencies/ui`, `feature/dashboard/ui`,
  `feature/household/ui`, `feature/paymentmethods/ui`, `feature/profile/ui`,
  `feature/settings/ui`, `feature/setup/ui`, `feature/subscriptions/ui` — 14 modules, plus
  `androidApp` itself (single `androidJvm` target, not KMP, same as Taiga's `androidApp`). Re-run
  the same `grep` to regenerate this list if a module is added or removed before relying on it.
- **`androidApp`'s compile task is flavor-qualified** — `compileFdroidDebugKotlin` /
  `compileGplayDebugKotlin` (CLAUDE.md's own build-commands section already uses these names for
  the Koin `--rerun-tasks` recipe). Use `compileFdroidDebugKotlin` for the audit — matches Taiga's
  own choice of its cheaper flavor, and no `-PgplayBuild` is needed since nothing here touches
  Firebase/Play Core code.
- **Domain modules that may hit the same "no stability marker" gap Taiga's task 3 fixed**:
  `core/domain`, `feature/categories/domain`, `feature/currencies/domain`,
  `feature/dashboard/domain`, `feature/household/domain`, `feature/paymentmethods/domain`,
  `feature/profile/domain`, `feature/setup/domain`, `feature/subscriptions/domain` — 9 modules,
  none of which apply any Compose-related plugin today. **Whether this is actually a live problem
  here is unconfirmed, unlike Taiga where the domain-model gap was the dominant finding (60 of the
  audit's unstable-parameter hits).** This project already routes some screens through a mapped
  `*UiItem`/`*UiState` shape rather than passing a domain type straight into a composable
  (`SubscriptionUiItem` in `feature/subscriptions/ui` is `Int`/`String`/`Boolean`/`BillingCycle?`
  only, not the domain `Subscription` — see CLAUDE.md's own Architecture section) — whether every
  screen does this consistently, or some editor/form composable takes a domain enum
  (`BillingCycle`, a `Currency`, a `Category`) straight as a parameter, is exactly what task 2's
  audit will show. **Don't pre-guess this** — Taiga's own task 3 write-up is the reference for the
  fix shape (a minimal `taigamobile.kmp.library.stability`-equivalent convention plugin — Compose
  Kotlin compiler subplugin only, `compileOnly compose-runtime`, no UI toolkit) if the audit does
  turn up the same pattern, but task 1/2 here don't assume it will.
- `docs/revisit.md` is empty (0 entries as of 2026-08-12) — a genuine, out-of-scope-for-this-task
  finding from task 2's triage would be entry **#1** here, not a continuation of any existing
  numbering.

## Task 1 — Gradle wiring: opt-in stability reports

**Size:** S

**What:** Add a shared function, gated behind a Gradle project property, mirroring
`-PgplayBuild`'s pattern exactly.

1. New file
   `build-logic/convention/src/main/kotlin/com/grappim/wallosmobile/buildlogic/ComposeCompilerReports.kt`:
   ```kotlin
   package com.grappim.wallosmobile.buildlogic

   import org.gradle.api.Project
   import org.gradle.kotlin.dsl.configure
   import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

   // Opt-in Compose Compiler stability audit — see docs/compose/stability-reports.md.
   // Off by default: -PcomposeStabilityReport to generate *-classes.txt / *-composables.txt.
   fun Project.configureComposeStabilityReports() {
       if (!project.hasProperty("composeStabilityReport")) return
       extensions.configure<ComposeCompilerGradlePluginExtension> {
           metricsDestination.set(layout.buildDirectory.dir("compose_reports"))
           reportsDestination.set(layout.buildDirectory.dir("compose_reports"))
       }
   }
   ```
   No `targetKotlinPlatforms` — per the "Researched facts" note above, there's nothing to
   restrict here; a single-target module has nothing to duplicate.
2. Call it right after `apply("org.jetbrains.kotlin.plugin.compose")` in both call sites:
   - `KmpLibraryComposeConventionPlugin.kt`
   - `AndroidApplicationConventionPlugin.kt`

**Done when:**
```bash
rm -rf feature/subscriptions/ui/build/compose_reports
./gradlew :feature:subscriptions:ui:compileAndroidMain -PcomposeStabilityReport --rerun-tasks
ls feature/subscriptions/ui/build/compose_reports   # *-classes.txt / *-composables.txt present

rm -rf feature/subscriptions/ui/build/compose_reports
./gradlew :feature:subscriptions:ui:compileAndroidMain --rerun-tasks
ls feature/subscriptions/ui/build/compose_reports   # No such file or directory

./gradlew :androidApp:compileFdroidDebugKotlin -PcomposeStabilityReport --rerun-tasks
ls androidApp/build/compose_reports

./gradlew :build-logic:convention:compileKotlin   # build-logic isn't ktlint-covered, per CLAUDE.md
```
`--rerun-tasks` is needed on every run here, cold or not — Gradle doesn't track `-P` project
properties as task inputs (same gotcha CLAUDE.md's build-commands section already documents for
env vars), so an already-`UP-TO-DATE` compile task silently skips regenerating the report.

**Finalize focus:** if the extension's real member names differ from what's assumed above
(shouldn't happen — same Kotlin version as the already-confirmed Taiga jar — but confirm rather
than trust), record the actual signature here.

**Result (2026-08-12):** extension member names matched as assumed, no deviation. One thing not
predicted above: `-PcomposeStabilityReport` also emits an `android/` subdirectory and a
`*-composables.csv` file alongside the two `.txt` files in `compose_reports/` — worth accounting
for in task 2's aggregator (it already only globs `*-classes.txt`/`*-composables.txt` by pattern,
so the extra files shouldn't confuse it, but confirm rather than assume once task 2 runs it for
real) and in task 2's own doc.

## Task 2 — Aggregator script + first repo-wide audit + doc

**Size:** M

**What:**
1. Copy `TaigaMobileNova/docs/compose/stability-scan.py` here verbatim — it's stdlib-only,
   project-agnostic (parses `**/build/compose_reports/*-classes.txt` /
   `*-composables.txt` by path pattern, no hardcoded module names or package prefixes). No
   changes needed unless this project's actual report format differs (check after task 1 lands).
2. Run the flag across all 14 Compose UI modules' `compileAndroidMain` plus `androidApp`'s
   `compileFdroidDebugKotlin` (module list from "Researched facts" above — re-derive via the same
   `grep` if it's gone stale) with `-PcomposeStabilityReport --rerun-tasks`.
3. Run `python3 docs/compose/stability-scan.py`, triage:
   - Genuine bug (a `List` that should be `ImmutableList`) → fix inline if small.
   - Real but out of scope (third-party type, a real refactor) → `docs/revisit.md` entry #1 (first
     entry in this project — see "Researched facts").
   - Nothing found → say so plainly; a clean audit is still a result, same as Taiga's own
     zero-`List`-violations finding.
4. Write `docs/compose/stability-reports.md` — the "run it again" reference, modeled on Taiga's
   own doc of the same name: how to run the audit (the exact module-list command), the two report
   file formats (`-classes.txt` unstable-class blocks, `-composables.txt` unstable-parameter
   blocks — the actionable one), and this audit's findings.
5. One-line pointer from CLAUDE.md's Compose rules section to the new doc, next to the existing
   `ImmutableList` bullet — don't inline the how-to there.

**Done when:**
```bash
python3 docs/compose/stability-scan.py   # prints a summary (possibly empty)
./gradlew allTests detekt ktlintCheck    # if any production fix landed in step 3
```

**Finalize focus:** update `docs/compose/stability-reports.md` if the report format differs from
Taiga's (`-classes.txt`/`-composables.txt`, hyphenated — Taiga's own plan initially assumed an
underscore and had to correct it; confirm rather than assume the same here).

## Task 3 — fix whatever the audit finds

Not scoped yet — deliberately, the same way Taiga's own task 3 wasn't written until task 2's
findings existed to react to. Two known shapes it might take, from Taiga's precedent:

- **If the dominant finding is the domain-model gap** (domain types read as unstable everywhere
  because their modules apply no Compose plugin): Taiga's fix was a new minimal convention plugin
  (Compose Kotlin compiler subplugin only, `compileOnly compose-runtime`, applied to the domain
  modules whose types reach a composable) — full writeup and the three rejected alternatives (full
  Compose convention on domain modules, a `stabilityConfigurationFiles` trust-list, expanding the
  `*UI` model + mapper pattern everywhere) are in `TaigaMobileNova/docs/compose/stability-reports-plan.md`
  task 3. Re-read it rather than re-deriving the tradeoffs, but confirm the module list against
  this project's own 9 domain modules rather than assuming Taiga's 11 map over.
- **If the finding is something else entirely, or nothing**: scope task 3 (or skip it) once task 2
  actually says what's there. Don't pre-write a fix for a finding that might not exist.
