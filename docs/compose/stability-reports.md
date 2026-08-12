# Compose Compiler stability reports

How to run a Compose stability audit and read its output. Background and the Gradle wiring
decisions live in [stability-reports-plan.md](stability-reports-plan.md) — this doc is the "run it
again" reference, not the history.

## Running the audit

Reports are opt-in (`-PcomposeStabilityReport`) and Gradle does not track that project property as a
task input, so a task that's already `UP-TO-DATE` from a prior build will not regenerate its report —
pass `--rerun-tasks` (or delete the module's `build/compose_reports/` first) to force it.

Unlike TaigaMobileNova, there's no `jvm`-target-only restriction to worry about — this project has no
`jvm()` target at all, so `compileAndroidMain` is each module's only compile task and there's nothing
to duplicate (see [stability-reports-plan.md](stability-reports-plan.md#researched-facts)).

```bash
# clear any stale reports first
find . -type d -name compose_reports -exec rm -rf {} +

# every module with alias(libs.plugins.wallosmobile.kmp.library.compose), plus androidApp
# (single androidJvm target, no flavor split needed since nothing here touches Firebase/Play Core)
./gradlew \
  :composeApp:compileAndroidMain :core:navigation:compileAndroidMain :strings:compileAndroidMain \
  :uikit:compileAndroidMain :utils:ui:compileAndroidMain \
  :feature:categories:ui:compileAndroidMain :feature:currencies:ui:compileAndroidMain \
  :feature:dashboard:ui:compileAndroidMain :feature:household:ui:compileAndroidMain \
  :feature:paymentmethods:ui:compileAndroidMain :feature:profile:ui:compileAndroidMain \
  :feature:settings:ui:compileAndroidMain :feature:setup:ui:compileAndroidMain \
  :feature:subscriptions:ui:compileAndroidMain \
  :androidApp:compileFdroidDebugKotlin \
  -PcomposeStabilityReport --rerun-tasks

python3 docs/compose/stability-scan.py
```

The module list is every module whose `build.gradle.kts` has
`alias(libs.plugins.wallosmobile.kmp.library.compose)` (`grep -rl "wallosmobile.kmp.library.compose"
--include="build.gradle.kts" .` to regenerate it if modules are added/removed — the grep also matches
`build-logic/convention/build.gradle.kts`, which only *declares* the plugin id and isn't a module to
compile) plus `androidApp`.

## Report file formats

Each module's `build/compose_reports/` contains (module name / project path baked into the filename,
so exact names vary):

- `<module>-classes.txt` — one block per class compiled in that module:
  ```
  unstable class com.grappim.wallosmobile.feature.settings.ui.trustedcerts.TrustedCertsUiState {
    stable val isLoading: Boolean
    unstable val certPendingRevoke: PendingCertTrust?
    <runtime stability> = Unstable
  }
  ```
  Each member line is prefixed `stable`/`unstable`/`runtime` (`runtime` means "depends on a generic
  type argument's runtime stability"). The `<runtime stability>` line is the class's overall verdict.
- `<module>-composables.txt` — one block per `@Composable` (and some non-composable top-level
  functions the compiler still tracked), listing `restartable`/`skippable` flags and each parameter
  prefixed the same way:
  ```
  restartable skippable scheme("[androidx.compose.ui.UiComposable]") fun com.grappim.wallosmobile.feature.settings.ui.trustedcerts.TrustedCertRow(
    unstable cert: PendingCertTrust
    stable onRevokeClick: Function1<PendingCertTrust, Unit>
  )
  ```
  **This file is the actionable one** — an unstable *class* only matters in practice if it's also an
  unstable *composable parameter* (a class that's merely stored in a repository or mapper, never
  passed to a skippable composable, doesn't affect recomposition).
  **Known report gap, confirmed 2026-08-12**: a parameter whose type is declared in the *same* module
  as the composable consuming it (e.g. `TrustedCertsContent(uiState: TrustedCertsUiState)`, both in
  `feature/settings/ui`) gets printed with **no** stability prefix at all — not `stable`, not
  `unstable` — even when the `-classes.txt` report for that exact type shows unstable members. Only a
  parameter whose type comes from an *already-compiled, different* module (e.g. `PendingCertTrust`
  from `core/domain`, consumed by `TrustedCertRow` in `feature/settings/ui`) gets a definitive
  `stable`/`unstable` marker. So the composables-with-unstable-parameters list under-counts: cross-check
  a same-module `UiState` parameter's real stability against its own module's `-classes.txt` entry
  rather than trusting a blank prefix to mean "stable."
- `<module>-composables.csv` — same data as the `.txt`, tabular, unused by the scan script.
- `android/main/<module>-module.json` — raw per-target metrics Compose's own tooling can consume; not
  human-oriented, not parsed by the scan script. (No `jvm/` sibling — see "Running the audit" above.)

## Reading the scan script's output

`docs/compose/stability-scan.py` is stdlib-only (no new dependency for a script run a few times a
year), copied verbatim from TaigaMobileNova — no project-specific changes needed, the report format
matches exactly (`-classes.txt`/`-composables.txt`, hyphenated). It prints two sections:

- **Unstable classes** — every class with at least one unstable member, one line per member:
  `module | class FQN | member: Type`.
- **Composables with unstable parameters** — every composable with at least one unstable parameter,
  one line per parameter: `module | fun FQN | param: Type`. Triage from this section first per the
  note above, but remember its known gap for same-module `UiState` parameters (see above).

## What the first audit found (2026-08-12, task 2)

**Zero plain-`List<T>` fields in state classes** — the `ImmutableList`/`persistentListOf()`
convention (CLAUDE.md's Compose rules) is followed correctly everywhere; the audit found no
convention violation to fix.

**The same domain-model gap TaigaMobileNova hit, on a smaller scale.** Three `*/domain` modules
define types consumed directly as Composable parameters, and none of them apply the Compose compiler
plugin, so the compiler has no stability marker to trust and defaults each type to `Unstable`:

| Domain type | Module | Consumed by (composable parameter) |
|---|---|---|
| `PendingCertTrust` | `core/domain` | `TrustedCertRow`, `RevokeConfirmDialog` (`feature/settings/ui`), `CertTrustDialog` (`feature/setup/ui`) |
| `IconFile` | `feature/paymentmethods/domain` | `IconFilePicker` (`feature/paymentmethods/ui`) |
| `LogoFile` | `feature/subscriptions/domain` | `LogoFilePicker` (`feature/subscriptions/ui`) |

The instability also propagates into `TrustedCertsUiState`, `LoginUiState`,
`PaymentMethodEditorUiState` and `SubscriptionEditorUiState` (each embeds one of the three types
above), which in turn makes `TrustedCertsContent`, `LoginContent`, `PaymentMethodEditorContent` and
`SubscriptionEditorContent` unstable too — not visible directly in the composables-with-unstable-
parameters list because of the same-module report gap noted above, confirmed instead by cross-
referencing each `UiState`'s own `-classes.txt` entry.

**Fix scoped as CHECKLIST.md step 20.3** — same minimal-convention-plugin shape as
TaigaMobileNova's task 3 (Compose Kotlin compiler subplugin only, `compileOnly compose-runtime`, no
UI toolkit reaching the domain layer), applied to these 3 confirmed modules rather than Taiga's 11.
Not run yet as of this writing; see the plan doc's task 3 for the scoped work and Taiga's own task 3
write-up for the mechanism and the three rejected alternatives.

**Expected, not actionable without a `stabilityConfigurationFiles` policy decision** (independent of
the domain-model gap, same buckets Taiga found): `kotlinx.datetime.LocalDate` (`ProfileViewModel
.anchorDate`, `SubscriptionEditorUiState.nextPayment`/`.startDate`, `DateField`'s `date` parameter in
`feature/subscriptions/ui`) and `androidx.savedstate.serialization.SavedStateConfiguration`
(`core/navigation`'s `rememberNavigationState`) — both foreign types with no stability marker
regardless of our code. One data point isn't enough to justify a standing config file yet.

**Not actionable, not a bug**: several `*ViewModel` classes show unstable members
(`moneyFormatter`/`dateFormatter`/`savedStateHandle` on `DashboardViewModel`,
`SubscriptionDetailViewModel`, `SubscriptionsViewModel`, `SubscriptionEditorViewModel`; `anchorDate` on
`ProfileViewModel`). This project's Screen/Content split (CLAUDE.md's "UI state and events") means a
ViewModel is never itself passed as a Composable parameter — `Content` composables take `uiState`
only — so a `ViewModel`'s own class-level instability has no effect on recomposition and needs no
fix.

CLAUDE.md's Compose rules links here next to the `ImmutableList` convention bullet.
