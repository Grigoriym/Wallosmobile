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

**Fixed in CHECKLIST.md step 20.3** — same minimal-convention-plugin shape as TaigaMobileNova's task
3 (Compose Kotlin compiler subplugin only, `compileOnly compose-runtime`, no UI toolkit reaching the
domain layer): new `wallosmobile.kmp.library.stability` convention plugin, applied alongside
`wallosmobile.kmp.library` on exactly the 3 modules confirmed above. See the plan doc's task 3 for
the scoped work and Taiga's own task 3 write-up for the mechanism and the three rejected
alternatives.

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

## After the fix (2026-08-12, task 3)

Re-ran the same 15-module + `androidApp` audit after applying `wallosmobile.kmp.library.stability`
to `core/domain`, `feature/paymentmethods/domain` and `feature/subscriptions/domain` — no straggler
turned up beyond those 3 (unlike TaigaMobileNova's task 3, whose hand-traced list had missed a
module and only the re-scan caught it).

**Composables-with-unstable-parameters dropped to exactly the 2 pre-existing, independently-unstable
entries** — `rememberNavigationState`'s `SavedStateConfiguration` parameter and `DateField`'s
`LocalDate?` parameter, both foreign types untouched by this fix, as expected. `PendingCertTrust`,
`IconFile` and `LogoFile` no longer appear anywhere in that section — the 5 composables in the table
above (`TrustedCertRow`, `RevokeConfirmDialog`, `CertTrustDialog`, `IconFilePicker`,
`LogoFilePicker`) are gone from the list entirely.

**One expected wrinkle**: `IconFile.bytes: ByteArray` and `LogoFile.bytes: ByteArray` still show as
`unstable` members in the classes report — Compose's stability inference treats any `Array`/
`ByteArray` field as unstable regardless of a stability marker, since content can mutate without a
reference change. This is Compose semantics, not a gap the marker plugin can close, and it doesn't
matter here: neither class is consumed as a composable parameter anywhere the `bytes` field itself
drives recomposition (the picker composables read other fields), confirmed by the
composables-with-unstable-parameters list no longer listing either type.

## After the LocalDate trust-list (2026-08-12, CHECKLIST.md 20.4)

`kotlinx.datetime.LocalDate` — the one "expected, not actionable" bucket the fix above couldn't
reach, since it's a third-party type with no module of ours to apply the Compose compiler
subplugin to — is now trust-listed via a `stabilityConfigurationFiles` config file
(`config/compose/stability_config.conf`, one line: `kotlinx.datetime.LocalDate`), wired through a
new `configureComposeStabilityConfig()` (`ComposeCompilerReports.kt`) called unconditionally
(never gated behind `-PcomposeStabilityReport`, unlike the reports themselves — a stability config
file changes real generated bytecode on every build) from the same three call sites
`configureComposeStabilityReports()` already used: `KmpLibraryComposeConventionPlugin`,
`AndroidApplicationConventionPlugin`, `KmpLibraryStabilityConventionPlugin`. That means every
module carrying the Compose compiler subplugin trusts the type, not just the 3 hand-traced
candidates (`feature/subscriptions/domain`, `feature/subscriptions/ui`, `feature/profile/ui`) —
deliberate, since `LocalDate` is genuinely immutable (all `val`s) everywhere, and piggybacking on
the existing call sites needed no new per-module decision.

Re-ran the same 15-module + `androidApp` audit after the change. **Composables-with-unstable-
parameters dropped to exactly 1 entry** — `rememberNavigationState`'s `SavedStateConfiguration`
parameter, the one remaining independently-unstable foreign type. `DateField`'s `date: LocalDate?`
parameter now reads `stable`, confirmed directly in `feature/subscriptions/ui`'s
`-composables.txt`. `SubscriptionEditorUiState.nextPayment`/`.startDate`,
`AddSubscriptionParams.nextPayment`/`.startDate` and `EditSubscriptionParams.nextPayment`/
`.startDate` all read `stable` in their own `-classes.txt` entries too (cross-checked directly
since same-module `UiState` parameters have the report gap noted above) — `Subscription`,
`AddSubscriptionParams`, `EditSubscriptionParams` and `SubscriptionEditorUiState` are now
genuinely stable on every member, not just report-quiet on the domain-model gap 20.3 fixed.

CLAUDE.md's Compose rules links here next to the `ImmutableList` convention bullet.
