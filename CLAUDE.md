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

# To actually see what a module's @ComponentScan found — the graph can't be started before the
# app is wired — read the generated module out of the bytecode. One `module$lambda` per definition:
javap -p -c core/storage/build/classes/kotlin/android/main/com/grappim/wallosmobile/core/storage/\
ComGrappimWallosmobileCoreStorageStorageModuleModuleKt.class | grep "private static final"
```

A step whose `Verify:` line is about the running app is verified **on the emulator**, not by
assembling. Headless, no snapshot, no interaction needed beyond `input tap`:

```bash
~/Android/Sdk/emulator/emulator -avd Medium_Phone_API_36.1 -no-snapshot-save -no-boot-anim \
  -gpu swiftshader_indirect &
adb wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
./gradlew :androidApp:installDebug && adb shell am start -n com.grappim.wallosmobile/.MainActivity
adb exec-out screencap -p > shot.png   # then read shot.png; tap with `adb shell input tap X Y`
adb emu kill                           # don't leave it running
```

`screencap` is 1080×2400 while the image comes back scaled — multiply the coordinates read off
the screenshot by the stated factor before feeding them to `input tap`.

Filling a form: **tap the first field once, then `input keyevent KEYCODE_TAB` between fields.**
Compose honours TAB for focus, and re-tapping by coordinate goes wrong the moment the keyboard
opens and shifts the layout — a mis-tap lands on whatever moved into that spot (typing an API key
into the URL field, say).

**"Don't keep activities" is not a process-death test.** It recreates the activity inside a live
process, so anything that only breaks when the *process* is rebuilt passes it. The real check is
to background the app and kill it:

```bash
adb shell input keyevent KEYCODE_HOME && adb shell am kill com.grappim.wallosmobile
adb shell monkey -p com.grappim.wallosmobile -c android.intent.category.LAUNCHER 1
```

`am kill` keeps the task and its saved state; `force-stop` discards them, so it tests nothing.
This is the check that caught the nav3 first-composition bug — the developer option did not.

**Run the cycle once, from a clean task.** Every `monkey … LAUNCHER` after an `am kill` *adds* a
`MainActivity` to the task, and once there is more than one the relaunch starts a fresh activity
instead of restoring the killed one — so the second and third cycles restore nothing and read as a
regression that isn't there (2.4 chased exactly this). `adb shell am force-stop` first to reset the
task, relaunch, get to the screen under test, and only then background + `am kill`. `numActivities`
in the `ActivityTaskManager` logcat line tells you which situation you are in.

`localhost` from the emulator is the emulator, so the host is **`http://10.0.2.2:8282`**. The
Bash tool's sandbox also blocks loopback, so `curl` to `127.0.0.1`, `adb` and the emulator all
need `dangerouslyDisableSandbox`.

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
- **Mappers are classes, not extension functions** — one per file, for testability. Same for
  formatters: pure logic gets **no interface**. An interface here is a seam over a platform or
  over IO (`SecretCipher`, `ApiKeyStorage`, `WebLoginApi`) — something a host test can't reach.
  Faking a pure class only lets the consumer's test assert output the app never produces.
- **Storage** is DataStore-backed (KMP), interface + impl, keys in a `private companion object`.
- **A new module must be added to the root `build.gradle.kts` `kover { }` block** — coverage is
  aggregated by an explicit per-module list, so a module left out of it is silently at 0% and
  nothing fails. `:testing` is deliberately absent (fakes, not production code).
- **Every module of a layer gets the same plugin set** — see plan §3.3 for the table and the
  standard dependency blocks. `ui` = `kmp.library` + `library.compose` + `di` + `serialization`;
  `data` = `library` + `di` + `network`; `dto` = `library` + `serialization`;
  `mapper` = `library` + `di`. Coroutines, datetime, immutable collections, `core:logger` and the
  test deps come from the convention plugins — never declare them per module. Same for the Compose
  set, including **material icons** (`Icons.Filled.*`), which material3 does *not* pull in
  transitively and which therefore lives in `configureKmpCompose()`, not in any module.
- **`material-icons-core` is ~50 icons, and the obvious one is usually missing** — no
  `Subscriptions`, no `Payment`, no `Visibility`/`VisibilityOff`, not even `Add`; `ArrowBack`,
  `List` and `Send` live under `Icons.AutoMirrored.Filled.*`. Reaching for anything else means
  adding `material-icons-extended` to `configureKmpCompose()`, so pick from the set, use a
  `TextButton` with a word in it (the login password toggle is Show/Hide text), or say you're
  growing it. To list the set without a compile: `unzip` the `material-icons-core-*.jar` from
  `~/.gradle/caches` and `ls` its `androidx/compose/material/icons/filled/` directory.

## Non-negotiables

- Package root `com.grappim.wallosmobile`. Module namespace follows the Gradle path.
- **Pre-v1, there is nothing to be backward compatible with.** No installs exist, so don't write
  a DataStore migration, a deprecated overload, or a compatibility shim — change the thing and
  say in the commit that stored state is discarded. The serialized nav back stack and the
  DataStore contents are both disposable; a moved route class or a renamed key is free. This
  expires the day the app ships: the stored API key and the back stack are then the two things
  that need real care.
- **No `androidMain` in feature modules** — use `expect`/`actual`. Platform targets are declared
  **only** in `configureKmp()` in `build-logic`.
- **`commonMain` is not enforced platform-neutral here.** Android being the only target,
  `commonMain` compiles against the JVM variants: `java.io.File` and `kotlinx.coroutines.runBlocking`
  both resolve there and *nothing* fails. Keeping common code common is a discipline, not a
  compiler guarantee — the day a second target is declared, whatever leaked in stops compiling.
- **DI: Koin with `io.insert-koin.compiler.plugin`. Never KSP for DI.** One
  `@Module @Configuration @ComponentScan` class per module. (KSP is still used for Room.)
  **A `@Factory` reached through a `@Single` is a `@Single`** — the singleton resolves it once and
  holds that instance forever. When a `@Factory` exists for a *lifetime* reason (the
  `@WebSessionHttpClient` cookie jar, plan §1.1), every class between it and the call site has to
  be a `@Factory` too, or the reason is silently undone. Nothing fails; the object just lives too
  long.
  **`startKoin<KoinApp>` expands at the *call site***, into
  `startKoinWith(listOf(AndroidModule().module(), AppModule().module()))` — check it with `javap -c`
  on `WallosApp.class`. It gathers `@Configuration` modules only from the compilation that calls it,
  so `androidApp`'s own `AndroidModule` needs no wiring while **every module from another Gradle
  module must be in `AppModule`'s `includes`**. A forgotten line there compiles and crashes at
  first injection; `KoinGraphTest` is what catches it.
  **The graph test uses `koin-test`'s `verify()`, never `checkModules()`** — `checkModules`
  instantiates definitions, which here means a DataStore file and an HTTP engine. `verify()` reads
  a definition through its **bound type's** constructor, so for a `@Single fun provideX(): T` it
  inspects `T`'s constructor rather than the function's parameters; that is why
  `HttpClientEngine` sits in `extraTypes` as a known false positive, next to the types
  `:androidApp` really does supply.
- **Navigation 3, not nav2.** `NavDisplay` + `entryProvider`; no `NavController`/`NavHost`.
  `org.jetbrains.androidx.navigation3:*` in `commonMain` — never `androidx.navigation3:*`.
  Every new route must also be registered in the polymorphic `SerializersModule` in
  `NavKeySerializers.kt`, or back-stack restore breaks silently on process death. The
  `SavedStateConfiguration` carrying it is *not* optional: `rememberNavBackStack` `require`s a
  non-default `serializersModule` and throws on the **first composition** if given
  `SavedStateConfiguration.DEFAULT` — only a *missing route* is the silent, process-death-only
  failure.
  **`rememberNavBackStack` consumes its restored state only in the *first* composition.** Anything
  that gates the shell on an async value — a DataStore flow, a suspend read — composes
  `NavDisplay` a pass later and the restored back stack is dropped with no error: the app comes
  back alive, on the start destination. `WallosAppContent`'s startup branch is seeded from
  `rememberSaveable` for exactly this reason. Don't put a loading state above the shell.
- **Not everything on screen is a route.** Login isn't — the startup branch renders it *instead of*
  the shell, so it has no `NavDisplay`, no back-stack entry, and nothing to register. The test is
  whether anything can navigate *back* to it; a screen the app is either on or not is state.
- **Tests use hand-written fakes in `:testing`. No mocking library — no MockK, no Mockito,
  anywhere.** `kotlin.test` + Turbine. **This extends to the platform: no Robolectric.** Its
  shadows are mocks of Android, and the same objection applies — when Compose UI tests arrive
  they will be **instrumented**, on a real runtime. Don't propose Robolectric as the cheaper
  option; it was weighed and declined, on dependency count as much as on principle. Fake/fixture shape: plan §6.1. `:testing` is for doubles
  **other** modules need; a double used by exactly one test file stays private in that file.
  Ktor's **`MockEngine` is not a mocking library** and is fine — it's the only way to get an
  `HttpClient` in a host test, since `HttpClient { }` autodiscovers an engine and okhttp is
  `androidMain`-only. It reaches every `commonTest` as `api(libs.ktor.client.mock)` in `:testing`,
  alongside `kotlinx-coroutines-test`; never declare either per module.
  **`:testing` is excluded from linting** (`lintingExclusions` in `build-logic/.../Quality.kt`,
  plus `.editorconfig`), so there is no `:testing:ktlintFormat`/`:testing:detekt` task at all —
  asking for one fails with "task not found", which is the config working, not a broken build.
  **A fake's settable field must not be named after the method it feeds.** `var baseUrl` beside
  `override fun getBaseUrl()` is a "platform declaration clash" — the property's getter compiles to
  `getBaseUrl()` too. Name the field for what it holds (`var url`), not for the method.
- **A ViewModel test must set the main dispatcher.** `viewModelScope` dispatches on
  `Dispatchers.Main`, which a host test doesn't have, so the first `launch` throws. Use
  `MainDispatcherRule` from `:testing` — not a JUnit `@Rule` (this is `kotlin.test`), so call
  its `setup()`/`tearDown()` from `@BeforeTest`/`@AfterTest`.
  **A repository test injects `UnconfinedTestDispatcher()`, not `StandardTestDispatcher()`.**
  Every repository here takes an `@IoDispatcher` and does its work in `withContext(dispatcher)`;
  a `StandardTestDispatcher()` built outside `runTest` carries its *own* scheduler, so that
  `withContext` dies with "Detected use of different schedulers" — which surfaces as an
  `IllegalStateException` where the test expected a `WallosError`, not as anything mentioning
  dispatchers.
- **A `commonTest` fixture is a Kotlin constant, not a file.** There is no portable way to read a
  resource or a path from `commonTest`, so recorded HTML/JSON lives in a `*Fixtures.kt` and
  anything filesystem-backed needs an in-memory fake (`FakePreferencesDataStore`, 1.4).

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
- **Everything that leaves `core:api` is a `WallosError`.** A decode failure becomes
  `Malformed`, not a leaked `SerializationException`, so repositories only ever match one type.
- To catch a kotlinx.serialization failure without a bare `catch (Exception)`:
  **`SerializationException` extends `IllegalArgumentException`**, as does the failure of the
  `.jsonObject` accessor — one `catch (e: IllegalArgumentException)` covers both and cannot
  swallow a `CancellationException`.

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

- **Text fields come back HTML-escaped** — a subscription named `1&1 Telekom` is
  `1&amp;1 Telekom` on the wire, and it renders that way unless the mapper unescapes it. Same for
  `notes`, the resolved `category_name` / `payer_user_name` / `payment_method_name`, and currency
  `name` / `symbol`. `HtmlUnescaper` (`feature:subscriptions:mapper`) is the one place that
  reverses it; use it from any new mapper rather than writing a second one. It decodes `&amp;`
  **last** on purpose — decode it first and `&amp;lt;`, which is the literal text `&lt;`, becomes
  a `<` the user never typed.
- **Unset dates are `""`, not `null`** (`cancellation_date`, `start_date`), so a nullable field is
  not enough — blank has to read as absent.

Two that bite later: `cycle=5` (One-time) is readable but **rejected on write**, and
`Unauthorized or Not Found` is a per-row ownership error that must **not** clear the stored key.

### There is a live instance, and it is the only one to test against

`docs/local-info.txt` holds the URL and credentials for the user's own Wallos container
(`http://localhost:8282`, i.e. `http://10.0.2.2:8282` from the emulator). It is committed on
purpose — the instance is LAN-only, the user has said so explicitly, so don't re-raise it as a
leak. **Every on-device `Verify:` line means this instance.**

**Do not reach for `demo.wallosapp.com`.** Its `profile.php` dies with a PHP fatal
(`no such table: uploaded_avatars`), so there is no `id="apikey"` to scrape and the Path A login
bridge can never succeed there. References to it were removed from the docs in 1.11.

### The same instance is behind the `wallos` MCP

`mcp__wallos__*` reaches a **real Wallos v5.4.2**, and it is the **user's own personal instance** —
`gregorz`, user id 1, real subscriptions. Read tools (`wallos_get_version`, `wallos_get_user`,
`wallos_list_*`, `wallos_get_subscription`, `wallos_get_monthly_cost`, `wallos_get_period_budget`)
are free to call and are the fastest way to settle a question `docs/WALLOS_API.md` leaves open —
it was derived from PHP source, so the MCP is the check on it. **`wallos_add_subscription`,
`wallos_update_subscription`, `wallos_delete_subscription` and `wallos_set_budget` mutate the
user's live data — ask first, every time.**

Three limits worth knowing before leaning on it:

- **It returns the payload unwrapped** — `wallos_get_user` gives `{"user": {…}}` with no `success`
  or `title`. So it confirms **real values**, and says nothing about the envelope behaviour
  `core:api` is built around (HTTP 200 on failure, PHP prefixes, the `title` catalogue). Those
  still need `curl` against `api/*.php` directly, per §8's smoke test.
- **It adds fields the API does not have.** `wallos_list_subscriptions` returns a `logo_url` that
  exists nowhere in the PHP — the server sends the bare `logo` filename. So it is *not* an
  authority on field shapes either: **model DTOs against `curl`, and use the MCP for values.**
- **It is one instance at one version.** A field present here may be absent on the older installs
  the app has to tolerate — `ignoreUnknownKeys` and nullable DTO fields are still the rule.

Getting a key for that `curl`, since the app is the only other thing that has one:

```bash
curl -s -c /tmp/w.txt -o /dev/null \
  -d "username=gregorz&password=$(sed -n '$p' docs/local-info.txt)" http://localhost:8282/login.php
curl -s -b /tmp/w.txt http://localhost:8282/profile.php | grep -o 'id="apikey"[^>]*'
```

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
