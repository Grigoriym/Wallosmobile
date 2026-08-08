# CLAUDE.md

WallosMobile — an unofficial Kotlin Multiplatform client for [Wallos](https://github.com/ellite/Wallos),
a self-hosted subscription tracker. **Android only for now**, built KMP-first.

## Status

`docs/CHECKLIST.md`'s `Progress` / `Current step` header is the **only** record of how far the
build has got. Don't duplicate it here — it would go stale every session.

## Docs, not memory

Anything a session learns that the next one should know goes in this file or `docs/` —
**never** in Claude's own memory system. The user can't see or edit that from here, so it isn't a
valid destination for a project fact, a convention, or an instruction they gave directly; write it
down where they can find and correct it instead.

## How work happens here

**One checklist step per session, with context cleared in between.**

1. `docs/CHECKLIST.md` — the executable plan. Numbered, tickable steps, each self-contained.
   The ticked ones live in `docs/archive/CHECKLIST-DONE.md`; read that only for precedent on a step
   that is cited by number, never to find work.
2. `docs/IMPLEMENTATION_PLAN.md` — the reference. Architecture and rationale.
3. `docs/WALLOS_API.md` — the API contract, derived from Wallos PHP source.

When asked to "do step N": read `docs/CHECKLIST.md`, do **exactly** that step, run its `Verify:`
line, then tick the box, add a one-line `Note:` under the step if anything deviated from the plan,
and update the `Progress` / `Current step` header. Don't start a step whose dependencies aren't
ticked, and don't expand scope beyond the step.

**Then close the step out — every time, without being asked:**

1. Run the **`/finalize` skill**. A step almost always teaches something the plan didn't know;
   this is where it gets written down instead of dying with the context.
2. Fold anything structural out of the step's `Note:` and into `IMPLEMENTATION_PLAN.md`, so the
   plan stays the canonical answer for the next session that reads it instead of the step text.
3. **Check the docs for claims the step just made false** — `IMPLEMENTATION_PLAN.md` and this
   file both accumulate stale present-tense statements (a build command that no longer exists,
   a "the repo is still…" line). Grep for what changed rather than trusting a read-through.
4. Commit and push. One commit per step, straight to `master` — subject `0.N — short title`,
   body listing the deltas that aren't obvious from the diff.

### Changing a gate means saying so

The gates constrain the code a session writes; nothing constrains a session from widening a gate
so its own step passes. `.github/workflows/guardrails.yml` doesn't prevent that — it makes it
impossible to do quietly. A commit trips it by touching `.github/`, `build-logic/`,
`config/detekt/`, `.editorconfig` or `gradle/libs.versions.toml`; by adding an `@Ignore` or a
`@Suppress`; or by **reducing** the number of Non-negotiables below or of steps in
`docs/CHECKLIST.md` **plus `docs/archive/CHECKLIST-DONE.md`** — the two are summed, so moving a
step between them is free and dropping one from either is not. Any of those needs a line in the
commit message:

```
Gate-change: what was widened, and why
```

That is an opt-in, not a veto — widening a gate is often right. Run it before committing:
`.github/scripts/check-guardrails.sh HEAD~1..HEAD`. A `@Suppress` **carried over from a reference
project** is worth deleting and recompiling first: 3.7's ported `X509Certificate` fake came with
`@Suppress("OVERRIDE_DEPRECATION")` that this toolchain doesn't need, and it would have bought a
`Gate-change:` line for a warning that never fires. Editing the two rule documents is otherwise
free, so ticking a box, adding a `Note:` and reflowing a bullet all pass; only *dropping* a rule
or a step counts.

## Shared skills and agents

Skills and agents live in the **`agentic-grappim`** repo and are symlinked into
`~/.claude/skills/` and `~/.claude/agents/`. They are wired up **per machine, not per
clone** — there is nothing in this repo to install, and `.claude/agents/` and
`.claude/skills/` here hold none of their own.

Consequences worth knowing before touching one:

- **An edit there changes behaviour in every project on this machine.** That is the
  point of the repo, not a hazard to avoid — but it has to be committed in
  `agentic-grappim`, not here.
- **Never edit a shared skill for a fact about this project.** A WallosMobile-specific
  fact (the AVD name, the package id, a device gotcha) belongs in this file or in
  `docs/EMULATOR_TESTING.md`, not in the `emulator-testing` skill itself.
- A stale in-repo copy of a shared skill or agent would silently shadow the real one —
  don't create one.

This project uses the **`emulator-testing`** and **`finalize`** skills; both are shared.

## Build commands

```bash
./gradlew :androidApp:assembleGplayDebug :androidApp:assembleFdroidDebug   # build (both store flavors)
./gradlew allTests                           # all KMP module tests
./gradlew :module:path:testAndroidHostTest   # one module
./gradlew detekt ktlintCheck                 # must pass before ticking a step
./gradlew :module:path:ktlintFormat          # fix style — don't hand-format
./gradlew koverHtmlReport                    # coverage

# The Room DAOs are the one instrumented suite (3.3) — an emulator must be up, and neither
# `allTests` nor CI runs it. Nothing else in the repo has a device-test source set.
./gradlew :core:storage:connectedAndroidDeviceTest

# ktlint's `standard:function-signature` rule collapses any signature that fits in 120 chars back
# onto one line, so hand-wrapping a parameter list "for readability" fails `ktlintCheck`. Write it
# either way and let `ktlintFormat` decide.

# There is no `jvmTest` and no `testDebugUnitTest`. WallosMobile declares no `jvm()` target, so
# the only unit test task is the AGP KMP host test — `testAndroidHostTest`, source set
# `commonTest`, enabled by `withHostTestBuilder` in `KmpLibraryConventionPlugin`.

# Force the Koin compiler plugin to re-run after DI changes ("no definition found" crashes).
# Koin here is a Kotlin compiler plugin, NOT classic KSP — there is no build/generated/ksp/**
# to inspect; a clean compile is the only signal that new @Single/@KoinViewModel were picked up.
./gradlew :androidApp:compileGplayDebugKotlin --rerun-tasks

# To actually see what a module's @ComponentScan found — the graph can't be started before the
# app is wired — read the generated module out of the bytecode. One `module$lambda` per definition:
javap -p -c core/storage/build/classes/kotlin/android/main/com/grappim/wallosmobile/core/storage/\
ComGrappimWallosmobileCoreStorageStorageModuleModuleKt.class | grep "private static final"

# Any ad-hoc Python tooling (a trace analyzer, a one-off script — not part of the Gradle build)
# goes through a venv, never a bare/system `pip install`: this machine's `pip` refuses one outright
# ("externally managed environment", PEP 668). `python3 -m venv` in the scratch directory first.
```

A step whose `Verify:` line is about the running app is verified **on the emulator**, not by
assembling: `./gradlew :androidApp:installGplayDebug`, then boot and drive it. The generic
adb/`uiautomator` technique (headless boot, screenshot coordinate scaling, dump gotchas, form
filling, `am kill` vs `force-stop`, network toggling) and this project's own device facts and
app-specific gotchas (AVD name, package ids, the `DateField` dump quirk, the DataStore-planting
recipe, the stale-banner network check, and more) live in the **`emulator-testing` skill**
(`~/.claude/skills/emulator-testing`) and `docs/EMULATOR_TESTING.md`, not here — read those before
driving the emulator rather than re-deriving any of it.

CI (`.github/workflows/ci.yml`, plan §3.5) runs assemble + `allTests` + `detekt ktlintCheck` on
push and PR to `master`, but `paths-ignore` skips `**.md` and `docs/**` — a docs-only commit
produces **no CI run**, which is not a failure. Kover is local-only, and so are the instrumented
Room DAO tests — `allTests` doesn't fan out to device tests and the CI job has no emulator. The second workflow,
`guardrails.yml` (plan §3.6), has no `paths-ignore` and runs no Gradle, so a docs-only commit
does produce *that* run — see "Changing a gate means saying so" above.

## Architecture

`androidApp/` → `composeApp/` (DI root, drawer shell, nav) → `feature/` → `core/` → `utils/`.
Feature modules split `data` / `domain` / `dto` / `mapper` / `ui`. MVVM + Clean Architecture,
vertical slices, **all source in `commonMain`**. This list is the rules; the rationale, the
worked examples and which step established each one live in `docs/IMPLEMENTATION_PLAN.md`
(§2, §3.3, §3.4, §4.7, §6) — read there for the "why," not here.

- **Shell**: `ModalNavigationDrawer` + a `TopBarController` provided through `LocalTopBarConfig`;
  each screen declares its own `TopBarConfig`. Feature `ui` modules depend on `uikit`, never on
  `composeApp`.
- **Use cases only when a screen needs multiple calls.** Single repo calls go straight from the
  ViewModel.
- **A feature grows the modules its screen actually needs**, not the `data/domain/dto/ui` set
  plan §2 lists for it. Add a layer when a real repository or a second caller turns up; a third
  `ui` → `core` reach is the point to ask whether the seam is in the right place instead of
  counting reaches further.
- **Mappers are classes, not extension functions** — one per file, for testability. Same for
  formatters: pure logic gets **no interface**. An interface here is a seam over a platform or
  over IO (`SecretCipher`, `ApiKeyStorage`, `WebLoginApi`) — something a host test can't reach.
  Faking a pure class only lets the consumer's test assert output the app never produces.
- **Storage** is DataStore-backed (KMP), interface + impl, keys in a `private companion object`.
  The **Room cache** (`core/storage/.../db/`) is the exception to that shape: entities and DAOs,
  no interface over them, and **every column a SQLite primitive** — no `TypeConverter`, and no
  dependency on any `feature:*:domain`. Converting to domain types is a mapper's job in the
  feature that owns the model.
- **The cache belongs to the stored key, and `ApiKeyStorage.clear()` drops both.** It has three
  callers — disconnect and *both* login paths, which clear the stale key before validating a new
  one — so the eviction lives at that single point, not in a cleaner called next to it.
- **A repository over the cache is `observe*` + `refresh*`, never `get*`.** The DAO `Flow` is the
  only thing a screen reads and it cannot fail; the network only writes to the database. So a
  failed refresh changes nothing but the error, and the loading spinner belongs to the *empty*
  cache alone. Such a repository runs into detekt's **`allowedConstructorParameters: 6`** fast —
  split the DAO half into its own class rather than widening the rule; when the seventh thing has
  **no dependencies of its own**, the other way past it is to stop injecting it (a `private val`
  constructed by its owner instead).
- **What a repository has to say *during* a call is a callback parameter, not a flow beside it**
  (`onThrottleWait: (Duration) -> Unit`, defaulted to `{}`). The call is already the scope, so a
  flow would need a lifetime and an initial value that a parameter has none of.
- **A `SavedStateHandle` holds a JSON string here, not an encoded `SavedState`.** On Android
  `androidx.savedstate`'s `SavedState` **is** `Bundle`, unreachable from a host test — Robolectric
  is out. `SavedStateHandle()` itself, `get`, `set` and `getMutableStateFlow` are pure Kotlin, so
  a `String` value is testable in both directions while anything Bundle-shaped is testable in
  neither.
- **Read a `SavedStateHandle` key before anything starts writing it, not after.** A form that
  persists itself in `init` via `.onEach { savedStateHandle[KEY] = … }.launchIn(viewModelScope)`
  writes its *first* entry the moment that line runs — `viewModelScope`'s dispatcher is
  `Main.immediate`, so nothing defers it to a later loop turn. A same-`init` check of "does this
  key already have a value" has to run *before* the persist line, or it always sees what that
  line just wrote.
- **A new module must be added to the root `build.gradle.kts` `kover { }` block** — coverage is
  aggregated by an explicit per-module list, so a module left out of it is silently at 0% and
  nothing fails. `:testing` is deliberately absent (fakes, not production code).
- **Every module of a layer gets the same plugin set** — see plan §3.3 for the table and the
  standard dependency blocks. Coroutines, datetime, immutable collections, `core:logger` and the
  test deps come from the convention plugins — never declare them per module. Same for the
  Compose set, including **material icons** (`Icons.Filled.*`), which material3 does *not* pull
  in transitively and which therefore lives in `configureKmpCompose()`, not in any module.
- **`material-icons-core` is ~50 icons, and the obvious one is usually missing** — no
  `Subscriptions`, `Payment`, `Visibility`/`VisibilityOff`, `FilterList`; `ArrowBack`, `List` and
  `Send` live under `Icons.AutoMirrored.Filled.*`. `Icons.Filled.Add`/`.Edit`/`.Delete` **are** in
  the set, confirmed and not to be re-doubted. Reaching for anything else means adding
  `material-icons-extended` to `configureKmpCompose()`, so pick from the set, use a `TextButton`
  with a word in it (the login password toggle is Show/Hide text), or say you're growing it. To
  list the set without a compile: `unzip` the `material-icons-core-*.jar` from
  `~/.gradle/caches` and `ls` its `androidx/compose/material/icons/filled/` directory.
- **Material 3's own source is in the Gradle cache, and it is the authority on which colour role a
  component reads** — `unzip` `material3-desktop-*-sources.jar` from
  `~/.gradle/caches/modules-2/files-2.1/org.jetbrains.compose.material3/` and read
  `commonMain/androidx/compose/material3/tokens/*Tokens.kt` or `ColorScheme.kt`, rather than
  guessing which token a component reads.

## Non-negotiables

Rationale and mechanism for the dense ones below (DI, Nav3, Testing) live in
`docs/IMPLEMENTATION_PLAN.md` §1.1, §5 and §6.1 — this list is the rule, not the "why."

- Package root `com.grappim.wallosmobile`. Module namespace follows the Gradle path.
- **Pre-v1, there is nothing to be backward compatible with.** No installs exist, so don't write
  a DataStore migration, a deprecated overload, or a compatibility shim — change the thing and
  say in the commit that stored state is discarded. This expires the day the app ships: the
  stored API key and the back stack are then the two things that need real care.
- **No `androidMain` in feature modules** — use `expect`/`actual`. Platform targets are declared
  **only** in `configureKmp()` in `build-logic`.
- **`commonMain` is not enforced platform-neutral here.** Android being the only target,
  `commonMain` compiles against the JVM variants: `java.io.File` and `kotlinx.coroutines.runBlocking`
  both resolve there and *nothing* fails. Keeping common code common is a discipline, not a
  compiler guarantee — the day a second target is declared, whatever leaked in stops compiling.
- **DI: Koin with `io.insert-koin.compiler.plugin`. Never KSP for DI.** One
  `@Module @Configuration @ComponentScan` class per module. (KSP is still used for Room.)
  **A `@Factory` reached through a `@Single` is a `@Single`** — the singleton resolves it once
  and holds that instance forever, so every class between a lifetime-scoped `@Factory` and the
  call site has to be a `@Factory` too. **Every module from another Gradle module must be in
  `AppModule`'s `includes`**, and a definition declared in `AppModule` itself needs its
  dependencies resolvable from `:androidApp`'s classpath too — `KoinGraphTest` is what catches
  either mistake. **A route parameter reaching a ViewModel through `parametersOf` needs
  `@InjectedParam` on the constructor property**, or the graph looks for a definition of that
  type and the screen crashes at first injection — `KoinGraphTest` will **not** catch a missing
  one for a primitive type (`verify()` whitelists `String`/`Int`/`Long`/`Double`), but **will**
  catch a missing one on `SavedStateHandle`, which is not whitelisted. **A module class needs its
  own line in `composeApp`'s dependencies, separate from `AppModule`'s `includes`** — `includes`
  only tells the Koin compiler where to find the class, it doesn't add a Gradle dependency edge.
  8.3 hit this adding `DashboardDomainModule`: `composeApp` already depended on
  `feature.dashboard.data`, but that module's own dependency on `feature.dashboard.domain` is
  `implementation`, not `api`, so the domain module's class stayed invisible to `composeApp` until
  it got its own `implementation(projects.feature.dashboard.domain)` line. **The same rule applies
  past DI, to any type** — Gradle's `implementation` visibility is never transitive, so a module
  that needs a plain type (not just a Koin module class) reached only through another module's
  `implementation` dependency needs its own direct line too. 8.1 and 8.3 both hit the Koin-class
  form for `composeApp`; 8.4 hit the plain-type form for `feature:dashboard:ui`, which needed its
  own `implementation(projects.feature.subscriptions.domain)` line to see `Subscription` — the type
  `feature:dashboard:domain` already depends on the same `implementation` way.
- **Navigation 3, not nav2.** `NavDisplay` + `entryProvider`; no `NavController`/`NavHost`.
  `org.jetbrains.androidx.navigation3:*` in `commonMain` — never `androidx.navigation3:*`. Every
  new route must be registered in the polymorphic `SerializersModule` in `NavKeySerializers.kt`,
  or back-stack restore breaks silently on process death — `NavKeySerializersTest` only covers
  the drawer destinations, so a detail or editor route registered nowhere passes every gate; the
  `am kill` cycle in the `emulator-testing` skill is the only check on those. Don't put a loading
  state above the shell: `rememberNavBackStack` consumes its restored state only in the *first*
  composition, so gating it on an async value drops the back stack with no error.
- **Not everything on screen is a route.** Login isn't — the startup branch renders it *instead
  of* the shell, so it has no `NavDisplay`, no back-stack entry, and nothing to register. The
  test is whether anything can navigate *back* to it; a screen the app is either on or not is
  state.
- **Tests use hand-written fakes in `:testing`. No mocking library — no MockK, no Mockito,
  anywhere.** `kotlin.test` + Turbine. **This extends to the platform: no Robolectric** — its
  shadows are mocks of Android, and the same objection applies. Don't propose it as the cheaper
  option; it was weighed and declined. Instrumentation (`androidDeviceTest`) is the *last*
  resort, reached only when a host test genuinely cannot construct the thing under test (Room's
  builders, `BundledSQLiteDriver`'s native library) — check `androidHostTest` first, which can
  see `androidMain` classes a `commonTest` fake can't. Fake/fixture shape, `MockEngine`, dispatcher
  rules and detekt interactions with test code: plan §6.1.
- **A ViewModel test must set the main dispatcher.** `viewModelScope` dispatches on
  `Dispatchers.Main`, which a host test doesn't have. Use `MainDispatcherRule` from `:testing` —
  not a JUnit `@Rule` (this is `kotlin.test`), so call its `setup()`/`tearDown()` from
  `@BeforeTest`/`@AfterTest`.
- **A `@Dao` is faked by hand like anything else** — it is an interface, so a `commonTest` fake
  needs no Room runtime. Fake shape and the `MutableStateFlow`-backing gotcha: plan §6.1.
- **A `commonTest` fixture is a Kotlin constant, not a file.** There is no portable way to read a
  resource or a path from `commonTest`, so recorded HTML/JSON lives in a `*Fixtures.kt` and
  anything filesystem-backed needs an in-memory fake.

## Settled decisions

Weighed and declined — don't re-propose these.

| Not used | Instead | Why |
|---|---|---|
| Mocking libraries (MockK, Mockito) | Hand-written fakes in `:testing` | See "Tests use hand-written fakes" above. |
| Robolectric | Real host tests where possible, `androidDeviceTest` where not | Its shadows are mocks of Android; same objection as mocking libraries, plus another dependency. |
| A Kover coverage floor/gate | Tests written per checklist step, coverage read manually | Weighed and declined; don't re-propose a threshold. |

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
- **`compose:parameter-order` exempts exactly one trailing function**, so a `Screen` composable can
  keep `viewModel = koinViewModel()` first only while it has a *single* callback. Adding a second
  fails detekt (4.4's About row on `SettingsScreen`), and the fix is the order the subscriptions
  screens already use: callbacks first, `viewModel` last with its default.
- **`ImmutableList` / `persistentListOf()`** over `List` in state classes and Composable params,
  for stable recomposition.
- **Always write previews** for screens and reusable widgets, using `@PreviewWallosDarkLight` +
  `WallosMobilePreviewTheme` (both from `uikit`). The `Surface` belongs to `WallosMobileTheme`, so
  a preview draws the same background and `LocalContentColor` as the app — don't add a `Surface`
  back into a preview.
- **A colour role left out of `lightColorScheme`/`darkColorScheme` is not derived from `surface`** —
  it falls back to Material's baseline lavender, on screen, silently. Adding a component means
  checking which token it reads (see the sources-jar note under Architecture) and, if that role is
  still unset, setting it rather than passing a colour at the call site.
- Fixed-item screens (settings) use `Column`, not `LazyColumn`.
- **A new `CompositionLocal` fails `ktlintCheck`** (`compose:compositionlocal-allowlist`) until it
  is named in `.editorconfig`'s `compose_allowed_composition_locals` — which is a tripwire path, so
  adding one costs a `Gate-change:` line. There are two, both provided by the shell and by
  `WallosMobilePreviewTheme`: `LocalTopBarConfig` and `LocalIsOffline`.
- Offline → **disable** write actions (`enabled = !LocalIsOffline.current`), no error dialog.
  Wallos has no permission model, so there is no "hide the action" case. The local says only that
  the *device* has a network — a LAN-only Wallos instance is "online" here and still unreachable,
  so a failed request is still the thing that reports a dead server. `StaleBanner` overrides the
  error's own reason line for exactly this reason (plan §7.1) rather than blaming a server the
  request never reached.
- **An error over cached data is a banner, not a screen.** Two derived properties on the UI state,
  never a field the ViewModel sets: `isStale` = error *with* data (banner above rows that stay
  put), `isFailed` = error with *no* data (owns the screen, keeps the Try again button). A stored
  boolean would be a second copy of what `error` and the data field already say, free to drift
  from them.
- **The moment a screen narrows what it draws, those states must ask the *cache*, not the list.**
  `items` is the filtered view, so `items.isEmpty()` doesn't mean "nothing is cached" — a separate
  `hasCachedRows` field does, and `isNoMatch` (rows exist, none survive the filter) is a fourth
  derived state owing the user a Clear button. Details and the derivation table: plan §7.1.
  Filter and sort selections live in **`MutableStateFlow`s beside the UI state**, `combine`d with
  the DAO flow — one render path for "the criteria changed" and "a refresh arrived."
- **A pick-one-of-many field is `ExposedDropdownMenuBox` + `ExposedDropdownMenu`** — the precedent
  over a hand-rolled `ModalBottomSheet`. Both are *members* of `ExposedDropdownMenuBoxScope`,
  resolved through the implicit receiver inside `ExposedDropdownMenuBox { }`'s content lambda —
  there is no top-level symbol for either, so writing an `import` for one fails as an unresolved
  reference. `BoxScope.matchParentSize()` is the same shape. A field that opens something other
  than the keyboard (this dropdown, a `DatePickerDialog`) needs a `readOnly = true`
  `OutlinedTextField` plus a transparent clickable overlay `Box` — `readOnly` alone still routes
  taps into text-cursor placement, not a click callback. Full pattern, plus the `LocalDate` ↔ UTC
  epoch millis conversion `DatePicker` wants: plan §7.1 "Editor UI patterns."

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
- **Everything that leaves `core:api` is a `WallosError`.** A decode failure becomes
  `Malformed`, not a leaked `SerializationException`, so repositories only ever match one type.
- To catch a kotlinx.serialization failure without a bare `catch (Exception)`:
  **`SerializationException` extends `IllegalArgumentException`**, as does the failure of the
  `.jsonObject` accessor — one `catch (e: IllegalArgumentException)` covers both and cannot
  swallow a `CancellationException`.
- **An untrusted certificate is not a type anyone catches.** A platform callback that constrains
  the exception *type* — JSSE's trust manager may throw only `CertificateException` — does not
  constrain its **cause**, so the portable payload rides down there and
  `Throwable.findPendingCertTrust()` (`core:domain`) walks the chain for it. Any "everything else"
  arm has to *ask* it ahead of falling back to a generic unreachable-server message, because a
  rotated certificate is the one transport failure the user can fix. Full mechanism: plan §4.5.

## Strings and resources

- Type aliases: `RString` and `RPlurals` from `:strings`, `RDrawable` from `:uikit`.
- **A quantity string is a `<plurals>` resolved in the composable**, never a string built in a
  ViewModel: `pluralStringResource(RPlurals.x, count, count)` takes the count **twice** — once to
  pick the form, once as the `%1$d` argument. So UI state carries the *number* (and whatever enum
  picks the resource), not a rendered phrase.
- A string produced outside a Composable is a **`NativeText`** (`utils:ui`), resolved with
  `asString()` at the call site. `uikit` depends on it as `api`, so any module that has `uikit`
  already has `NativeText` and must not re-declare `utils:ui`.
- Arguments use printf style in XML — `%1$d`, `%1$s`, `%2$s` — passed to `stringResource(...)`.
- **Each string is imported by name** (`import …strings.generated.resources.login_connect`), so a
  new one fails as `Unresolved reference 'x'` **at the `RString.x` usage**, not at an import line.
  That reads exactly like stale codegen — it isn't; add the import. Don't reach for
  `--rerun-tasks` (which is the right move for Koin, and the wrong one here).
- **Do not escape apostrophes.** CMP resources are not Android XML; `\'` renders literally.
  Write `isn't`.

## The Wallos API will surprise you

Read `docs/WALLOS_API.md` before touching anything network-related — and treat it as **derived,
not complete**, since it was written from the PHP. `docker exec wallos cat /var/www/html/<file>.php`
is a read against the real thing and takes seconds: do it for any endpoint whose *failure* modes
the code has to branch on, before adding a **request** to a flow that already holds a session
(a GET can have a side effect a doc entry never would), and for any response whose *meaning* the
code has to infer, not only its explicit failure modes — `success: true` is the dangerous case,
because it reads as confirmation. `WALLOS_API.md`'s own §5.5 and §7 carry two live corrections of
exactly this kind (a doc row that was silently missing, a check that was actively wrong); read
those before trusting a `Ref:` line into it as more than the previous session's reading.

- **Everything returns HTTP 200**, including auth failures. Only the JSON `success` field is
  reliable. Never branch on HTTP status except 404 (endpoint missing on this version) and 5xx.
- **Auth is a static API key in the form body**, not a header. No login endpoint, no token, no
  refresh. Username/password onboarding works by driving the *web* login and scraping the key
  (plan §1.1).
- **PHP `display_errors` prepends HTML to valid JSON.** Never call `.body<T>()` — always go
  through `WallosEnvelopeParser`.
- **No pagination anywhere.** Filtering and sorting are client-side.
- **Text fields come back HTML-escaped** (`1&1 Telekom` is `1&amp;1 Telekom` on the wire) —
  `HtmlUnescaper` (`feature:subscriptions:mapper`) reverses it; use it from any new mapper rather
  than writing a second one.
- **Unset dates are `""`, not `null`** (`cancellation_date`, `start_date`), so a nullable field is
  not enough — blank has to read as absent.
- `cycle=5` (One-time) is readable but **rejected on write**, and `Unauthorized or Not Found` is a
  per-row ownership error that must **not** clear the stored key.

### There is a live instance, and it is the only one to test against

`docs/local-info.txt` holds the URL and credentials for the user's own Wallos container
(`http://localhost:8282`, i.e. `http://10.0.2.2:8282` from the emulator), plus every throwaway
test instance built on top of it (currency conversion, TLS front, 2FA, OIDC) and the recipe to
rebuild each — read that file rather than re-deriving any of it here. It's committed on purpose:
the instance is LAN-only, and the user has said so explicitly. **Every on-device `Verify:` line
means this instance**, unless a step needs one of the throwaway ones instead.

Its data has holes worth knowing before planning a verify: **`notes` and `url` are `""` on every
subscription**, and `start_date` is `""` on a good few, so anything rendering those fields can only
be proven by unit test and preview. Pick the row deliberately — `Fiton` (id 4) has a start date and
a `&` in its category name. It is also single-currency (conversion off, rates never fetched) — see
`docs/local-info.txt` for the scratch instance that isn't.

**Do not reach for `demo.wallosapp.com`.** Its `profile.php` dies with a PHP fatal, so there is no
`id="apikey"` to scrape and the login bridge can never succeed there.

### Before decomposing anything sizeable, check the actual web UI too — not just the API

`/home/gregory/proj/other/Wallos` is a git checkout of `ellite/Wallos`, kept on the same version as
the docker instance (`includes/version.php` — confirmed `v5.4.2` in both, 2026-08-08). `api/*.php`
is what a client calls, but `index.php`, `subscriptions.php`, `settings.php`, `profile.php` and
`includes/*_calculations.php` are what the *product* actually is — what a screen shows, hides,
gates behind a condition, or computes a value from is frequently a decision the API surface alone
doesn't carry. Filed 2026-08-08 after the dashboard (8.4) turned out to diverge from the web
dashboard in four confirmed ways (see `docs/CHECKLIST.md`'s "To review" — no limit on upcoming
payments where the web caps at 3, no "Overdue Renewals" section, two distinct budget widgets
collapsed into one unlabeled card, several sections with no mobile equivalent at all) despite every
individual API call being used correctly. **Before decomposing a milestone** (a new `M<N>` in
`docs/CHECKLIST.md`, not every small step) that touches a screen with a web equivalent, read that
screen's PHP alongside `WALLOS_API.md` — the API doc says what a field *is*; the web PHP says what
the product *shows*, and the two are not the same question. Keep this checkout up to date with
`git pull` if it starts drifting from the docker instance's own version.

### The same instance is behind the `wallos` MCP

`mcp__wallos__*` reaches the same real instance. Read tools are free to call and are the fastest
way to settle a question `WALLOS_API.md` leaves open. **`wallos_add_subscription`,
`wallos_update_subscription`, `wallos_delete_subscription` and `wallos_set_budget` write to this
instance too — free to call without asking first**, the same as the read tools: confirmed with the
user 2026-08-08, it's their own local, disposable instance (port 8282), not shared or
production data. It returns the payload unwrapped (no `success`/`title`, so it confirms values but
not envelope behaviour) and adds fields the API doesn't have (`logo_url` exists nowhere in the
PHP) — model DTOs against `curl`, use the MCP for values. Getting a key for that `curl`:
`docs/local-info.txt`.

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

### Determinism over process
If a task has one correct, computable answer, use a tool for it rather than following a fixed
procedure by hand — a script or a hook can't skip a step or get one wrong the way a prose
checklist can. `.github/scripts/check-guardrails.sh` is this project's own example: the gate rules
are a script, not a mental checklist to re-derive each session. Reserve judgment for what actually
needs it — ambiguous input, a plan, a choice between options.

### Verification
**"Done" means the `Verify:` line (or the relevant Gradle task) ran and passed.** If it didn't run,
say that instead of asserting the step works.

- **A narrow pass proves your change works, not that you broke nothing.** When a failure shows up
  alongside your change, check it against a clean tree (`git stash -u`) before assuming you caused
  it — or that you didn't.
- **A check that looks the same whether the thing worked or not is not a check.** Before trusting
  one, name what it would show if the change had done nothing.
- **Before/after comparisons need equally fresh runs** — a baseline from a partially cached build
  (`./gradlew` with warm task outputs) measures a different universe than a clean after-run.
- **A performance fix is verified against the user's literal complaint, not against whichever data
  source diagnosed it.** Confirmed 2026-08-07: network-timestamp logs found a real ~500ms stagger
  behind "the add-subscription screen feels slower to open," and a fix landed and measured clean
  against that data — but it left the original complaint (the screen itself takes a while to
  *appear*) unaddressed, because a second, independent cause (a JIT warm-up tax on cold navigation)
  was invisible to network logs and only showed up once the user re-tested the actual complaint.
  One data source agreeing with itself is not the same claim as "this is fixed."

### Friction goes in writing too
The rule above is for problems in the *code*; this one is for friction in the *tooling* — a
guessed command that failed, a flag that needed different quoting, a check that confidently
returned the wrong answer. The reflex is to route around it and say nothing.

Add a one-line, past-tense entry to `docs/frictions.md` before moving on — create the file if it
isn't there. At the end of a session, read the file back and report what was added, with a count,
even when the count is zero. The same friction three times is a fix, not a fourth line — raise it
in `/finalize`.
