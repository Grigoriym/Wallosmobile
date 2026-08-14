# Revisit list

Findings worth fixing that are bigger than "small and isolated" — deferred here instead of fixed
inline, per `docs/CHECKLIST.md`'s M17 instructions. Numbered, not dated; each entry stays until
it's actually done, then gets deleted (git has the history).

## 1. A custom `lintChecks` detector can't see a dependency module's own source

Filed 2026-08-14, M25 (25.1). `:lint-rules`' `UnstableCollectionInUiStateDetector` is correct and
covered by its own `lint-tests`-harness unit tests, and `lintChecks(project(":lint-rules"))` is
wired into every module via `configureLinting()` (`build-logic`'s `Quality.kt`) — but
`androidApp:lintFdroidDebug`/`lintGplayDebug` only ever sees findings in `androidApp`'s own
`src/main`. A deliberately-planted violation in `feature:subscriptions:ui`'s `commonMain` (a real
`*UiState` class) never appeared in `androidApp`'s report, with `checkDependencies` either `true`
or `false`. Every `feature:*:ui`/`composeApp`/`uikit` module — where the actual `*UiState`/
`@Composable` code lives — exposes only a `lintAnalyzeAndroidHostTest` task under AGP 9.3.1's
`com.android.kotlin.multiplatform.library` plugin; there is no `lintAnalyzeDebug`/equivalent task
that runs Lint against that module's own production source at all, so there is nothing today to
propagate. Bundled checks shipped inside an AAR's own `lint.jar` (Compose runtime's
`FlowOperatorInvokedInComposition` among them) are unaffected — that's a different mechanism
(applies automatically to any module compiling against the AAR), confirmed still catchable via
`lintFdroidDebug` now that `abortOnError` is `true` again.

Net effect: the detector only guards code written directly in `androidApp` (MainActivity-adjacent
glue, essentially none of which is `*UiState`/`@Composable`), not the feature/composeApp/uikit code
it was built for. Options considered:

- Check whether a newer AGP version's KMP library plugin adds a production-source lint task —
  untried, and not something to chase just for this (an AGP bump is its own decision).
- Apply `com.android.lint` directly to each Compose-bearing module instead of relying on
  cross-module propagation — untried, uncertain whether it coexists with
  `com.android.kotlin.multiplatform.library` already applied to the same module.
- Move the detector's target modules off `com.android.kotlin.multiplatform.library` for lint
  purposes specifically — not otherwise motivated, heavy-handed.

**Likely real fix, discussed with the user 2026-08-14, not started: port the check to detekt
instead of Android Lint.** `configureLinting()` already runs detekt against every module's own
`commonMain` correctly (`source.setFrom(layout.projectDirectory.dir("src"))`, confirmed no
cross-module gap — that's how `compose-rules`' detekt ruleset already catches things today), so
this sidesteps the whole AGP KMP-library limitation above rather than working around it. Shape:
a new `detekt-rules` module (parallel to `lint-rules`, but implementing detekt's `Rule`/
`RuleSetProvider` API against Kotlin PSI instead of Lint's `Detector`/`IssueRegistry` against
UAST — same underlying check: a `List`/`MutableList`-typed property in a `*UiState` class or a
public `@Composable` parameter), wired in via `detektPlugins(project(":detekt-rules"))` next to
the existing `composeRules-detekt` line in `Quality.kt`. Whether `:lint-rules` stays (its own
unit-tested `UnstableCollectionInUiStateDetector` still catches a violation written directly in
`androidApp`, and the milestone's own real secondary yield — flipping `lint.abortOnError` to
`true` — stands regardless) or gets dropped once the detekt version lands is an open call for
whoever picks this up.

Re-run the planted-violation check in `feature:subscriptions:ui` (temporarily add a `List<String>`
to a real `*UiState` class, `./gradlew :androidApp:lintFdroidDebug --rerun-tasks`, check the HTML
report for `UnstableCollectionInUiState`) before considering the Lint half of this closed; for the
detekt version, `./gradlew :feature:subscriptions:ui:detekt` on the same scratch change is the
equivalent check.
