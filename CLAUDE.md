# CLAUDE.md

WallosMobile — an unofficial Kotlin Multiplatform client for [Wallos](https://github.com/ellite/Wallos),
a self-hosted subscription tracker. **Android only for now**, built KMP-first.

## Status

`docs/CHECKLIST.md`'s `Progress` / `Current step` header is the **only** record of how far the
build has got. Don't duplicate it here — it would go stale every session.

## How work happens here

**One checklist step per session, with context cleared in between.**

1. `docs/CHECKLIST.md` — the executable plan. Numbered, tickable steps, each self-contained.
2. `docs/IMPLEMENTATION_PLAN.md` — the reference. Architecture and rationale.
3. `docs/WALLOS_API.md` — the API contract, derived from Wallos PHP source.

When asked to "do step N": read `docs/CHECKLIST.md`, do **exactly** that step, run its `Verify:`
line, then tick the box and update the `Progress` / `Current step` header. Put anything that
affects later steps in the Deviations log at the bottom. Don't start a step whose dependencies
aren't ticked, and don't expand scope beyond the step.

**Then close the step out — every time, without being asked:**

1. Run the **`/finalize` skill**. A step almost always teaches something the plan didn't know;
   this is where it gets written down instead of dying with the context.
2. Fold anything structural out of the step's `Note:` and into `IMPLEMENTATION_PLAN.md`, then
   annotate the Deviations row (*now in plan §X*) so the log and the plan can't contradict
   each other.
3. **Check the docs for claims the step just made false** — `IMPLEMENTATION_PLAN.md` and this
   file both accumulate stale present-tense statements (a build command that no longer exists,
   a "the repo is still…" line). Grep for what changed rather than trusting a read-through.
4. Commit and push. One commit per step, straight to `master` — subject `0.N — short title`,
   body listing the deltas that aren't obvious from the diff.

## Build commands

```bash
./gradlew :androidApp:assembleDebug          # build
./gradlew allTests                           # all KMP module tests
./gradlew :module:path:testAndroidHostTest   # one module
./gradlew detekt ktlintCheck                 # must pass before ticking a step
./gradlew :module:path:ktlintFormat          # fix style — don't hand-format
./gradlew koverHtmlReport                    # coverage

# ktlint's `standard:function-signature` rule collapses any signature that fits in 120 chars back
# onto one line, so hand-wrapping a parameter list "for readability" fails `ktlintCheck`. Write it
# either way and let `ktlintFormat` decide.

# There is no `jvmTest` and no `testDebugUnitTest`. WallosMobile declares no `jvm()` target, so
# the only unit test task is the AGP KMP host test — `testAndroidHostTest`, source set
# `commonTest`, enabled by `withHostTestBuilder` in `KmpLibraryConventionPlugin`.

# Force the Koin compiler plugin to re-run after DI changes ("no definition found" crashes).
# Koin here is a Kotlin compiler plugin, NOT classic KSP — there is no build/generated/ksp/**
# to inspect; a clean compile is the only signal that new @Single/@KoinViewModel were picked up.
./gradlew :androidApp:compileDebugKotlin --rerun-tasks
```

CI (`.github/workflows/ci.yml`, plan §3.5) runs assemble + `allTests` + `detekt ktlintCheck` on
push and PR to `master`, but `paths-ignore` skips `**.md` and `docs/**` — a docs-only commit
produces **no run**, which is not a failure. Kover is local-only.

## Architecture

`androidApp/` → `composeApp/` (DI root, drawer shell, nav) → `feature/` → `core/` → `utils/`.
Feature modules split `data` / `domain` / `dto` / `mapper` / `ui`. MVVM + Clean Architecture,
vertical slices, **all source in `commonMain`**.

- **Shell**: `ModalNavigationDrawer` + a `TopBarController` provided through `LocalTopBarConfig`;
  each screen declares its own `TopBarConfig`. Feature `ui` modules depend on `uikit`, never on
  `composeApp`.
- **Use cases only when a screen needs multiple calls.** Single repo calls go straight from the
  ViewModel.
- **Mappers are classes, not extension functions** — one per file, for testability.
- **Storage** is DataStore-backed (KMP), interface + impl, keys in a `private companion object`.
- **Every module of a layer gets the same plugin set** — see plan §3.3 for the table and the
  standard dependency blocks. `ui` = `kmp.library` + `library.compose` + `di` + `serialization`;
  `data` = `library` + `di` + `network`; `dto` = `library` + `serialization`;
  `mapper` = `library` + `di`. Coroutines, datetime, immutable collections, `core:logger` and the
  test deps come from the convention plugins — never declare them per module.

## Non-negotiables

- Package root `com.grappim.wallosmobile`. Module namespace follows the Gradle path.
- **No `androidMain` in feature modules** — use `expect`/`actual`. Platform targets are declared
  **only** in `configureKmp()` in `build-logic`.
- **DI: Koin with `io.insert-koin.compiler.plugin`. Never KSP for DI.** One
  `@Module @Configuration @ComponentScan` class per module. (KSP is still used for Room.)
- **Navigation 3, not nav2.** `NavDisplay` + `entryProvider`; no `NavController`/`NavHost`.
  `org.jetbrains.androidx.navigation3:*` in `commonMain` — never `androidx.navigation3:*`.
  Every new route must also be registered in the polymorphic `SerializersModule` in
  `NavKeySerializers.kt`, or back-stack restore breaks silently on process death.
- **Tests use hand-written fakes in `:testing`. No mocking library — no MockK, no Mockito,
  anywhere.** `kotlin.test` + Turbine. Fake/fixture shape: plan §6.1. `:testing` is for doubles
  **other** modules need; a double used by exactly one test file stays private in that file.

## UI state and events

- **State classes carry data *and* the callbacks that call into their own ViewModel**, with no-op
  defaults so previews are trivial. The ViewModel wires its own methods in at construction;
  `Content` composables call `uiState.onSomething()` and never hold a ViewModel reference.
- **Exception: pure navigation callbacks** (`onBackClick`, `onItemClick` that only call
  `navigator.navigate(...)`) stay as plain parameters on the outer `Screen` composable — they're
  wired by the `EntryProviderScope`, not by this screen's ViewModel.
- **One-off events** (navigation, snackbars, success signals) use `Channel` + `receiveAsFlow()`,
  observed with `ObserveAsEvents`. **Never** in UI state.
- Screen/Content split — `Screen` takes the ViewModel, private `Content` takes `uiState`:

```kotlin
@Composable
fun FeatureScreen(viewModel: FeatureViewModel = koinViewModel(), onBackClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    FeatureContent(uiState = uiState, onBackClick = onBackClick)
}

@Composable
private fun FeatureContent(uiState: FeatureUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) { … }

@PreviewWallosDarkLight
@Composable
private fun FeatureContentPreview() = WallosMobilePreviewTheme {
    FeatureContent(uiState = FeatureUiState(), onBackClick = {})
}
```

Naming follows MealieMobile: `FeatureUiState` / `uiState` (not Taiga's `FeatureState` / `state`).

## Compose rules

- **No early returns in Composables** — they break composition (remembered state, effects). Use
  conditional content.
- **Lambda params are present tense**: `onClick` not `onClicked`, `onItemAdd` not `onItemAdded`.
  Enforced by the `compose:parameter-naming` lint rule — **not auto-correctable, fails the build.**
- **`ImmutableList` / `persistentListOf()`** over `List` in state classes and Composable params,
  for stable recomposition.
- **Always write previews** for screens and reusable widgets, using `@PreviewWallosDarkLight` +
  `WallosMobilePreviewTheme` (both from `uikit`).
- Fixed-item screens (settings) use `Column`, not `LazyColumn`.
- Offline → **disable** write actions (`enabled = isOnline`), no error dialog. Wallos has no
  permission model, so there is no "hide the action" case.

## Error handling

- **Never use bare `try/catch (Exception)` in coroutines** — it swallows `CancellationException`
  and breaks structured concurrency. Use `resultOf` from `core.domain`.
- **Never swallow an exception silently.** Every `catch` at minimum logs.
- `logcat { }` from `core:logger` has two overloads, and **inside a class body it always resolves
  to the `Any.logcat` extension**, which tags the line with the receiver's `simpleName`. The
  receiverless overload (tag stays `null`) is only reachable from top-level code — passing
  `tag = "…"` is the only way to override it from a class.
- Repository/use-case calls go through `resultOf`; errors reach UI state as `NativeText` via
  `getErrorMessage()`.

## Strings and resources

- Type aliases: `RString` from `:strings`, `RDrawable` from `:uikit`.
- Arguments use printf style in XML — `%1$d`, `%1$s`, `%2$s` — passed to `stringResource(...)`.
- **Do not escape apostrophes.** CMP resources are not Android XML; `\'` renders literally.
  Write `isn't`.

## The Wallos API will surprise you

Read `docs/WALLOS_API.md` before touching anything network-related.

- **Everything returns HTTP 200**, including auth failures. Only the JSON `success` field is
  reliable. Never branch on HTTP status except 404 (endpoint missing on this version) and 5xx.
- **Auth is a static API key in the form body**, not a header. No login endpoint, no token, no
  refresh. Username/password onboarding works by driving the *web* login and scraping the key
  (plan §1.1).
- **PHP `display_errors` prepends HTML to valid JSON.** Never call `.body<T>()` — always go
  through `WallosEnvelopeParser`.
- **No pagination anywhere.** Filtering and sorting are client-side.

Two that bite later: `cycle=5` (One-time) is readable but **rejected on write**, and
`Unauthorized or Not Found` is a per-row ownership error that must **not** clear the stored key.

## Reference projects

Read these rather than guessing — the conventions here are ported from them.

- `/home/gregory/proj/grappim/TaigaMobileNova` — module structure, `build-logic` convention
  plugins, networking, `:testing` fakes.
- `/home/gregory/proj/grappim/MealieMobile` — nav3 (`docs/kmp-nav3.md` plus the real code in
  `composeApp/.../nav/`), drawer shell, `uikit/.../widgets/topappbar/`, `core/api/NetworkModule.kt`,
  and the Screen/ViewModel/Route/DI templates in its `CLAUDE.md`.

Two known drifts in those repos: `MealieMobile/docs/kmp-nav3.md` disagrees with its own code
(plan §5.5), and Mealie's `CLAUDE.md` Tech Stack line says Koin uses KSP — its `build-logic` shows
the compiler plugin.

## Coding guidelines

**Tradeoff:** these bias toward caution over speed. For trivial tasks, use judgment.

### Think before coding
Don't assume. Don't hide confusion. Surface tradeoffs. State assumptions explicitly; if multiple
interpretations exist, present them rather than picking silently. If a simpler approach exists,
say so. If something is unclear, stop and name what's confusing.

### Simplicity first
Minimum code that solves the problem, nothing speculative. No features beyond what was asked, no
abstractions for single-use code, no unrequested "flexibility", no error handling for impossible
scenarios. If you write 200 lines and it could be 50, rewrite it.

### Surgical changes
Touch only what you must. Don't "improve" adjacent code, comments, or formatting. Don't refactor
what isn't broken. Match existing style even if you'd do it differently. Don't add UI or
navigation that wasn't asked for. Remove orphans *your* change created; mention pre-existing dead
code rather than deleting it. **Every changed line should trace directly to the request.**

### Goal-driven execution
Turn tasks into verifiable goals — "fix the bug" → "write a failing test, then make it pass". For
multi-step work, state the plan as steps with a verify check each, then loop until they pass.
