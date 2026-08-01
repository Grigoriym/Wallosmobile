# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `4/11` · M2 `0/7`
**Current step:** 1.5

---

## How to use this file

1. Start a fresh session.
2. Say: **"Read `docs/CHECKLIST.md` and do step N."**
3. When it passes its *Verify* line: tick the box, update **Current step** above, add a one-line
   note under the step if anything deviated from the plan.
4. Commit. Clear context. Repeat.

**Rules:**
- Never start a step whose dependencies aren't ticked.
- If a step turns out to be wrong or too big, don't push through — amend it here, note why, and
  say so. The checklist is the source of truth for what's left.
- Notes are for surprises that affect *later* steps (an API that behaved differently, a version
  that had to change). Not a work log.

## Ground rules (apply to every step)

**`CLAUDE.md` at the repo root holds the coding conventions** — KMP/DI/nav3 rules, Compose rules,
error handling, strings, and the think-before-coding / simplicity / surgical-changes guidelines.
It loads automatically; don't duplicate it here. Checklist-specific rules only:

- Do **exactly** the step. Don't pull work forward from a later step because it's convenient.
- `./gradlew detekt ktlintCheck` must pass before a step is ticked.
- A step that adds logic adds its tests in the **same** step — hand-written fakes in `:testing`,
  no mocking library (plan §6.1).
- Read the reference projects rather than guessing:
  `/home/gregory/proj/grappim/TaigaMobileNova` (structure, build-logic, networking)
  `/home/gregory/proj/grappim/MealieMobile` (nav3, drawer, top bar, templates in its `CLAUDE.md`)

---

## M0 — Foundation

Goal: an empty but correctly-structured project that builds, lints and tests.

- [x] **0.1 — Strip the wizard scaffold**
  Delete `desktopApp/`, `iosApp/`, and the `Greeting`/`Platform`/`App` sample files. Rename
  `shared/` → `composeApp/` (directory, `settings.gradle.kts`, namespace). Delete stale `build/`.
  *Verify:* `./gradlew :androidApp:assembleDebug`  ·  *Ref:* plan §2
  *Note:* `composeApp/` is now source-less and its `build.gradle.kts` still declares `jvm()` +
  the two iOS targets and a `Shared` framework — 0.3 replaces that file. `MainActivity.setContent`
  is empty until 1.8.

- [x] **0.2 — Version catalog**
  Replace `gradle/libs.versions.toml` with TaigaMobileNova's, plus MealieMobile's nav3 entries
  (`jetbrainsNav3 = "1.1.1"`, `jetbrains-navigation3-ui`, `jetbrains-lifecycle-viewmodelNavigation3`,
  `jetbrains-androidx-savedstate`). Drop Taiga's `jetbrainsNavigationCompose` — that's nav2. Set
  `app-pkg = "com.grappim.wallosmobile"`, JDK 21, `minSdk` 24.
  *Verify:* `./gradlew :androidApp:assembleDebug`  ·  *Ref:* plan §3.2, §5.1
  *Note:* AGP 9.3.1 needs Gradle ≥ 9.5, so the wrapper went 9.1.0 → **9.6.1** (both reference
  projects are on 9.6.1). The wizard's `build.gradle.kts` files now use Taiga's alias names
  (`libs.plugins.android.application`, `libs.jetbrains.compose.*`, `libs.versions.compileSdk`) and
  `JVM_21` / `VERSION_21` — 0.3 replaces them anyway. `jetbrains-lifecycle-viewmodelNavigation3`
  reuses Taiga's `jetbrainsAndroidxLifecycle = "2.11.0"` (same value as Mealie's
  `jetbrainsComposeLifecycle`), so no second lifecycle version entry. Koin's
  `koin-compose-navigation3` is **not** in the catalog yet — add it when 1.8 needs `koinViewModel`
  inside nav3 entries.

- [x] **0.3 — build-logic: base plugins**
  Create `build-logic/` with `convention/build.gradle.kts` and `settings.gradle.kts`; port
  `KmpConfiguration.kt`, `KotlinConfiguration.kt`, `ProjectExtensions.kt`,
  `AndroidApplicationConventionPlugin`, `KmpLibraryConventionPlugin`. **`configureKmp()` declares
  `androidTarget()` only** — no `jvm()`, no iOS. Register as `wallosmobile.*` plugin ids.
  *Verify:* `./gradlew :androidApp:assembleDebug`  ·  *Ref:* plan §3.1
  *Note:* there is **no literal `androidTarget()` call** — for KMP library modules the Android
  target is declared by `com.android.kotlin.multiplatform.library` (configured in
  `KmpLibraryConventionPlugin` via `KotlinMultiplatformAndroidLibraryExtension`), so Android-only
  means `configureKmp()` adds *no* targets at all. Kover, `configureTests()` and
  `configureLinting()` were deliberately left out — 0.5 owns them, and `convention/build.gradle.kts`
  so far declares only `android.gradlePlugin` + `kotlin.gradlePlugin`; 0.4/0.5 add the compose,
  detekt, ktlint and kover `compileOnly` deps. `configureKmp()`'s `core:logger` dependency is
  wrapped in a `findProject(":core:logger") != null` guard because the module doesn't exist until
  0.6 — **drop the guard once it does**, or a mis-typed path fails silently.
  `AndroidApplicationConventionPlugin` drops Taiga's flavors, signing configs and
  `configureAndroidOutputNaming` (WallosMobile has none of those), but does apply the Koin compiler
  plugin, so root `build.gradle.kts` gained `alias(libs.plugins.koin.compiler) apply false` — it
  compiles fine with no Koin dependency present, only warning that Koin plugin 1.0.2's newest
  tested Kotlin is 2.4.0 vs our 2.4.10. `composeApp/build.gradle.kts` is now nothing but
  `alias(libs.plugins.wallosmobile.kmp.library)`; its Compose plugins and deps were dropped rather
  than hand-rolled, since the module is source-less and **0.4 gives it Compose back** via
  `kmp.library.compose`.

- [x] **0.4 — build-logic: feature plugins**
  Port `KmpLibraryComposeConventionPlugin` (incl. the `androidResources.enable = true` fix),
  `KmpDiConventionPlugin`, `KmpNetworkConventionPlugin`, `KmpSerializationConventionPlugin`. Add
  the three nav3 deps to `configureKmpCompose()`.
  *Verify:* `./gradlew :androidApp:assembleDebug`  ·  *Ref:* plan §3.1, §5.1
  *Note:* `KmpLibraryComposeConventionPlugin` does **not** call `configureTests()`/`configureLinting()`
  yet — **0.5 must wire them into both `KmpLibraryConventionPlugin` and this one**, or Compose
  modules get no test deps. Root `build.gradle.kts` gained
  `alias(libs.plugins.kotlin.serialization) apply false` (same classpath reason as `koin.compiler`).
  `configureKmpCompose()` drops Taiga's `jvmMain`/desktop block and its `jetbrains.compose.navigation`
  (nav2); Mealie's extra `ui-graphics`/`animation`/`icons` entries have no catalog aliases here and
  were not added — add them if a screen needs them. `KmpNetwork` keeps only `commonMain` (ktor-core)
  + `androidMain` (okhttp). Both `kmp.di` and `kmp.network` configure `androidMain.dependencies`, so
  **a module must apply `kmp.library` first** (it brings `com.android.kotlin.multiplatform.library`,
  which creates that source set). `composeApp` is now `kmp.library` + `kmp.library.compose`.
  All five plugins were verified together on `composeApp` (deps resolved: nav3, savedstate, Koin BOM,
  ktor okhttp, serialization-json), then it was reverted to the two it actually needs.

- [x] **0.5 — Quality gates**
  detekt + ktlint + compose-rules + kover, `config/detekt/`, `.editorconfig`. Wire `configureTests()`
  and `configureLinting()` into the convention plugins — `configureTests()` adds `kotlin("test")`,
  Turbine and `project(":testing")` to every `commonTest`, so no module declares them by hand.
  Kover `excludes` for DTOs, DI modules, screens and widgets (plan §6.1).
  *Verify:* `./gradlew detekt ktlintCheck koverXmlReport`
  *Note:* **the unit test task is `testAndroidHostTest`, not `testDebugUnitTest` or `jvmTest`** —
  every later verify line was rewritten. Two things had to be fixed for the gates to be real, both
  latent in TaigaMobileNova too:
  (a) `com.android.kotlin.multiplatform.library` creates **no** host-test compilation unless asked,
  so `KmpLibraryConventionPlugin` now calls `withHostTestBuilder {}.configure { … }`. Without it
  `commonTest` belongs to no compilation and no test task exists at all — the reference projects
  never hit this because they get their test task from `jvm()`.
  (b) detekt's default source set is `src/main/{java,kotlin}`, which no KMP module has, so every
  `:module:detekt` was `NO-SOURCE`; `configureLinting()` now sets `source.setFrom(src/)`.
  Also: Taiga's `detekt.yml` carries a stale `potential-bugs>Deprecation>excludeImportStatements`
  key that detekt 2.0.0-alpha.5 rejects — dropped from our copy.
  `configureTests()` deliberately adds **only** the three deps the step names;
  `kotlinx-coroutines-test` (needed for `runTest`) is **not** among them — it must arrive as
  `api(libs.kotlinx.coroutines.test)` in `:testing`, as in Taiga. **Wire that in 0.6**, or the first
  ViewModel test in 1.10 fails to compile.
  `configureTests()`/`configureLinting()` were also wired into `AndroidApplicationConventionPlugin`
  (Taiga doesn't) — `:androidApp` holds `MainActivity` and the Koin startup glue, and would
  otherwise be the one module the gates never see. `configureTests()` no-ops its `commonTest` block
  when there is no KMP extension. Kover is applied per-module in `configureKmp()` with **no**
  `disabledForTestTasks` (Taiga disables the Android unit test tasks because it measures `jvmTest`;
  Android host tests are our only coverage source). Root aggregates `kover(project(":composeApp"))`
  — **0.6 must add every new module there**, or it is silently absent from coverage.
  `settings.gradle.kts` was missing a trailing newline; ktlint caught it.

- [x] **0.6 — Module skeletons**
  Create every module directory with its `build.gradle.kts` and `.gitignore`, all empty, using the
  **per-layer plugin sets in plan §3.3** (don't invent them — `ui` needs `kmp.serialization` for
  routes, `uikit`/`strings` need the `compose.resources { publicResClass = true }` block). Register
  in `settings.gradle.kts` with `TYPESAFE_PROJECT_ACCESSORS` enabled:
  `core:{api,domain,storage,navigation,async-kmp,appinfo-api,logger}`, `utils:{ui,formatter:decimal,formatter:datetime}`,
  `uikit`, `strings`, `testing`, `feature:setup:{data,domain,dto,ui}`,
  `feature:subscriptions:{data,domain,dto,mapper,ui}`.
  *Verify:* `./gradlew build`  ·  *Ref:* plan §2 (skip `core:crud` and the catalog features — not v1)
  *Note:* **`uikit` needs `api(libs.jetbrains.compose.components.resources)` too**, not just
  `strings` — plan §3.3 only mentions it for `strings`, but `generateResClass = always` emits a
  `Res` class that references `org.jetbrains.compose.resources.*`, so without the dependency
  `:uikit:compileAndroidMain` fails on the *generated* file. Both guards from 0.3/0.5 are now
  gone: `configureKmp()` depends on `:core:logger` unconditionally and `configureTests()` on
  `:testing`, each keeping only the "not myself" check, so a mis-typed path fails loudly.
  `:testing` got `api(libs.kotlinx.coroutines.test)` as 0.5 required. Modules are **build files
  only, no `src/`** — the standard `implementation(projects.…)` blocks from plan §3.3 arrive with
  the code in M1/M2, so nothing here declares an inter-module dependency yet.
  Plugin sets deliberately at the floor: `feature:*:domain` and `core:{domain,appinfo-api,logger}`
  are `kmp.library` alone (**add `kmp.di` when a module gains an injected use case** — 1.1's
  `core:domain` may need it); `core:storage` is `kmp.library` + `kmp.di`, with DataStore added in
  1.4; `feature:*:data` is `kmp.library` + `kmp.di` + `kmp.network`, no serialization.
  `core:serialization` from plan §2 was **not** created — this step's module list omits it; if
  1.2's `FormParams.date()` wants a custom serializer, create it then.
  Root kover lists all 22 new modules but **not `:testing`** (fakes, not production code) — keep
  adding new modules there. `TYPESAFE_PROJECT_ACCESSORS` was verified live
  (`projects.core.asyncKmp`, `projects.utils.formatter.datetime` both resolve).

- [x] **0.7 — CI**
  GitHub Actions: assemble + `allTests` + detekt + ktlintCheck on push and PR.
  *Verify:* workflow green.
  *Note:* one workflow, `.github/workflows/ci.yml`, one job, four gradle steps — no composite
  action (Taiga needs one because it has two workflows sharing setup) and no secrets, since a
  debug build signs itself. `gradle/actions/setup-gradle@v6` replaces Taiga's separate
  `wrapper-validation` + `actions/cache` pair. **`paths-ignore: ['**.md', 'docs/**']` means a
  docs-only commit gets no CI run at all** — fine for step close-outs, but don't read a missing
  run as a failure. Kover/Codecov is deliberately *not* in CI (this step names four tasks; Taiga's
  upload needs a `CODECOV_TOKEN` this repo doesn't have). Cold run ≈ 8 min.
  **M0 done.**

---

## M1 — Login (screen 1)

Goal: username + password → the app holds a validated API key and shows the drawer shell.

- [x] **1.1 — core:logger, core:async-kmp, core:domain**
  `logcat()` + `LogPriority` + Timber-backed Android logger. Dispatcher qualifiers
  (`@IoDispatcher` etc). `resultOf {}` / `mapResult` extensions and the `WallosError` sealed class.
  *Verify:* `./gradlew :core:domain:testAndroidHostTest`  ·  *Ref:* plan §4.3
  *Note:* **`TimberLogger.install()` exists but nothing calls it**, so every `logcat` is still a
  no-op — `androidApp` has `MainActivity` and no `Application` class. Wire it in with the Koin
  startup glue (1.11), or the whole app logs nothing and no test will catch it.
  Taiga's `TaigaLogger` is `WallosLogger` here; `logcat`'s two overloads survive unchanged, but
  **inside a class body `logcat { … }` always resolves to the `Any.logcat` extension** (tag =
  receiver's `simpleName`) — the receiverless one is only reachable from top-level code. A test
  asserting `tag == null` from inside a test class fails; that's the resolution working, not a bug.
  Two divergences from Taiga's `ResultExtension.kt`, both proven by test: `mapResult` uses
  `fold` instead of `getOrNull() != null`, which was wrong for a `Result` whose success value is
  `null`; and the separate `catch (TimeoutCancellationException)` clause is gone, since it is a
  `CancellationException` subclass and therefore unreachable.
  `mapError(title, detail)` is **not** in this step — the sealed class lives in `core:domain`
  (plan §4.3) but the title→error mapping ships with the parser in `core:api` (1.2), which is where
  the §5.3 table test belongs. `UnsupportedEndpoint` is a `data object`, so logs get a real
  `toString()`.
  No new plugins were needed: `core:domain` stays `kmp.library` alone (no injected use cases yet),
  `core:async-kmp` already had `kmp.di` from 0.6, and Taiga's `ThreadSafeMap` + `atomicfu` were
  **not** ported — nothing needs them yet. `core:logger` is the one non-feature module with an
  `androidMain` (Timber), as in Taiga.

- [x] **1.2 — core:api: envelope parser + FormParams** ⭐
  `WallosEnvelopeParser` (404 → `UnsupportedEndpoint`; strip any prefix before the first `{` and
  log it; `success != true` → map by title; else decode) and `FormParams` with
  `flag()`/`literalTrue()`/`date()`. Pure classes, no Ktor. **Write the tests in the same step**,
  including the auth-title table from `WALLOS_API.md` §5.3 as a parameterized test.
  *Verify:* `./gradlew :core:api:testAndroidHostTest`  ·  *Ref:* plan §4.2, §4.3, §4.4
  *This is the highest-value step in M1 — everything downstream trusts it.*
  *Note:* **the parse entry point is `parse(statusCode, body, DeserializationStrategy<T>)`, plus a
  top-level `inline fun <reified T> WallosEnvelopeParser.parse(statusCode, body)`** — a public
  `inline` *member* may not read the class's private `json`, so the reified form has to sit
  outside the class. **1.3's `WallosApiClient.post` hits the same wall**: an `inline reified`
  member can't touch a private `httpClient`/`apiKeyStorage`/`envelopeParser`, so those
  constructor properties need `@PublishedApi internal`, or `post` splits into a non-inline core
  taking the deserializer. Plan §4.1's sketch doesn't compile as written.
  The parser owns a **private** `Json { ignoreUnknownKeys = true; isLenient = true }`: with
  `ContentNegotiation` dropped (plan §4.1) it is the only JSON consumer in the app, so **1.3's
  `NetworkModule` should not define a second `Json`** — Mealie's `@HttpJson` qualifier exists to
  feed `ContentNegotiation` and has nothing to feed here.
  `mapError` is `internal` and lives in `WallosErrorMapper.kt` next to the parser; nothing outside
  `core:api` maps titles. `WallosEnvelopeParser` is `@Single`, but `core:api` still has **no
  `@Module @Configuration @ComponentScan` class** — 1.3's `NetworkModule` is that class and must
  scan `com.grappim.wallosmobile.core.api`.
  `FormParams` exposes `put`/`flag`/`literalTrue`/`date`/`asMap()`, all chainable. **`withApiKey()`
  and `build()` are not here** — they return Ktor `Parameters`, so they belong to 1.3.
  `literalTrue(k, false)` writes `"false"` rather than omitting the key (§3.2: "anything else =
  false"), and `date()` goes through `LocalDate.Formats.ISO`, not `toString()`.

- [x] **1.3 — core:api: HTTP clients**
  `NetworkModule` modelled on `MealieMobile/core/api/.../NetworkModule.kt` (`@HttpJson` qualifier,
  `@Module @Configuration @ComponentScan`, `expectSuccess = false`, `defaultRequest` off
  `BaseUrlProvider`), with the **five Wallos deltas from plan §4.1** — no `AuthHeaderPlugin`, **no
  `ContentNegotiation`** and no JSON default content type, redacting logger gated on build type,
  `HttpRequestRetry` restricted to `get_*.php`, and `BaseUrlProvider` returning the **instance
  root** (not `/api/`) with trailing-slash and subpath normalization. Plus `WallosApiClient`
  injecting `api_key` into every form POST, and the `@WebSessionHttpClient` **`@Factory`**
  (`followRedirects = false`, `HttpCookies`, no key injection).
  *Verify:* `./gradlew :core:api:testAndroidHostTest`  ·  *Ref:* plan §4.1, §1.1
  *Note:* this step had to reach into two modules it doesn't own, because `core:api` cannot
  compile without them. **`core:storage` gained the `ApiKeyStorage` (`getKey()` only) and
  `ServerUrlStorage` (`val serverUrl`) *interfaces*; 1.4 owns the DataStore impls and the rest of
  the surface** (`isConnected`, `setKey`, `clear`, `saveServerUrl`). `ServerUrlStorage.serverUrl`
  is deliberately **not** `suspend` — Ktor's `defaultRequest` block is not a suspend context, so
  **1.4 must keep the URL cached** rather than reading DataStore per request. `core:appinfo-api`
  gained `AppInfoProvider` with `isDebug()` alone (the §4.1 log-level gate). Neither module has an
  implementation yet, so **the Koin graph cannot be instantiated until 1.11** wires 1.4's storage
  and an `AppInfoProvider` in `androidApp`.
  Ktor 3.5.1 vs plan §4.1: `retryOnExceptionIf` takes **no `retryOnTimeout`** and hands the block
  an `HttpRequestBuilder`, whose `url` is a `URLBuilder` — there is **no `encodedPath`**, only
  `encodedPathSegments`. The predicate is extracted as `internal fun isReadEndpoint(pathSegments)`
  so it can be tested at all: Kover and testability both stop at `NetworkModule` (excluded by the
  root `*Module` filter, and its client config is only exercised at runtime).
  `WallosApiClient.post` took the same route as the parser — non-inline member taking a
  `DeserializationStrategy`, plus a top-level `inline reified` extension. `withApiKey(null)`
  **omits** `api_key` rather than sending an empty one, so the server answers `Missing API key` →
  `Unauthenticated` → setup. Useful corollary for **1.9**: with no key stored yet, a
  `FormParams().put("api_key", scrapedKey)` survives `withApiKey`, so the scraped key can be
  validated through `WallosApiClient` before it is persisted.
  Paths passed to `post` are **relative, no leading slash** (`api/status/version.php`) —
  `BaseUrlProviderImpl` appends the trailing slash that Ktor's `DefaultRequest` requires before it
  will append a relative path, and a leading slash discards the user's subpath.
  `HttpClient { }` uses engine autodiscovery (plan §4.1's v1 choice) and okhttp is `androidMain`
  only, so host tests build their own `HttpClient(MockEngine)`. `:testing` therefore gained
  `api(libs.ktor.client.mock)`, reaching every module's `commonTest` the way `coroutines-test`
  does — **1.9's recorded-HTML fixtures need it**. `core:api` declares `libs.ktor.logging` itself;
  it is not in `kmp.network` because no other module installs the plugin. `postMultipart`
  (plan §4.1) was **not** written — nothing in v1 uploads a file.

- [x] **1.4 — core:storage**
  DataStore for base URL + API key, Keystore-backed. `ApiKeyStorage` with `isConnected: Flow<Boolean>`,
  `getKey()`, `setKey()`, `clear()`. No Room, no NetworkMonitor. **The interfaces already exist**
  (1.3) — this step adds the impls and the missing members; keep `ServerUrlStorage.serverUrl`
  non-suspending and cached.
  *Verify:* `./gradlew :core:storage:testAndroidHostTest`
  *Note:* "Keystore-backed" became a **`SecretCipher` interface**, not an `expect`/`actual`:
  `KeystoreSecretCipher` (AES/GCM, `androidMain`) is unreachable from a host test, so as a seam it
  can be faked and both storage impls stay in `commonMain` (plan §4.7). The consequence for later
  steps: **the encryption itself has no test** — only the contract around it. `decrypt` returns
  `null` for a value it can't read (restored backup, invalidated key), so **1.11's startup branch
  sees "not connected" and routes to login** rather than getting a crash or a dead key.
  **`StorageModule` — the `@Module @Configuration @ComponentScan` class — lives in `androidMain`**,
  because the DataStore path needs `Context`; the generated bytecode confirms it still picks up the
  `commonMain` `@Single` impls. **1.11 must call `androidContext()` in `startKoin`**, or
  `provideDataStore` cannot resolve. One store file (`wallos_storage`) for both URL and key, and
  `clear()` **removes only `api_key`** — so **2.6's Disconnect keeps the server URL**, and re-login
  is one field.
  `ServerUrlStorageImpl` blocks on the first read (`runBlocking`) and caches thereafter;
  `runBlocking` resolves in `commonMain` only because Android is the sole target. `saveServerUrl`
  **is** `suspend` (Mealie's isn't) — 1.9/1.10 call it from a coroutine.
  1.3's private fakes in `core:api`'s tests had to grow the new interface members. **No fakes were
  added to `:testing`** — plan §6.1 lists `FakeApiKeyStorage`/`FakeServerUrlStorage` there, so 1.9
  should add them when a second module needs one. Tests use an in-memory `FakePreferencesDataStore`
  in `commonTest`: the real store wants a filesystem path, which `commonTest` has no portable way
  to produce.

- [ ] **1.5 — strings + uikit theme**
  `:strings` with `strings.xml` + the `RString` type alias. `:uikit` with the M3 theme, colour
  scheme, typography, the `RDrawable` alias, plus **`WallosMobilePreviewTheme` and the
  `@PreviewWallosDarkLight` annotation** — every later step's previews depend on these.
  (No apostrophe escaping in `strings.xml` — CMP doesn't apply AAPT rules.)
  *Verify:* `./gradlew :uikit:assemble`

- [ ] **1.6 — uikit: top app bar**
  Port `TopBarConfig`, `TopBarController` + `LocalTopBarConfig`, `NavigationIconConfig`,
  `TopBarAction` variants, `WallosTopAppBar` from
  `MealieMobile/uikit/.../uikit/widgets/topappbar/`.
  *Verify:* `./gradlew :uikit:assemble`  ·  *Ref:* plan §5.4

- [ ] **1.7 — core:navigation**
  Port `NavigationState` (dual back stack), `Navigator`, `toEntries()`, `rememberNavigationState`
  — taking `SavedStateConfiguration` as a **parameter** so this module imports no routes. Note
  `NavBackStack<NavKey>` is generic and `rememberViewModelStoreNavEntryDecorator<NavKey>()` needs
  its type argument. Port `NavigatorTest` too.
  *Verify:* `./gradlew :core:navigation:testAndroidHostTest`  ·  *Ref:* plan §5.2, §5.5

- [ ] **1.8 — composeApp: drawer shell**
  `DrawerDestination`, `DrawerItem`, `IconSource`, `DrawerItemsBuilder`, `RouteConfig(Provider)`,
  `MainAppState`, `WallosDrawerWidget`, `AuthenticatedMainScreen`, `MainNavHost`,
  `NavKeySerializers`. Keep the two details from §5.4: `NavigationBackHandler` composed **after**
  `MainNavHost`, and `isAnimationRunning` alongside `isOpen`. Drawer items: Subscriptions, Settings.
  *Verify:* app launches to an empty shell with a working drawer  ·  *Ref:* plan §5.4

- [ ] **1.9 — feature:setup data + domain** ⭐
  `WebLoginApi`: `POST /login.php` (302 → success; `Location` contains `totp.php` → `NeedsTotp`;
  200 → `InvalidCredentials`), `GET /profile.php`, extract `id="apikey"` by regex. `SetupRepository`:
  login → scrape → **validate via `api/status/version.php`** → persist key, discard session and
  password. **Tests in this step**, over recorded HTML fixtures.
  *Verify:* `./gradlew :feature:setup:data:testAndroidHostTest`  ·  *Ref:* plan §1.1, `WALLOS_API.md` §9

- [ ] **1.10 — feature:setup ui**
  `LoginRoute` (`@Serializable ... : NavKey`), `LoginScreen`, `LoginState` (data + callbacks),
  `LoginViewModel`. Fields: server URL, username, password + visibility toggle, Connect. Secondary
  "I have an API key" reveal. Errors attributed by failure layer (§1.1). `NeedsTotp` → message
  pointing at the key field. No top bar, no drawer.
  *Verify:* `./gradlew :feature:setup:ui:testAndroidHostTest`

- [ ] **1.11 — Wire it up end to end**
  Startup branch: stored key → shell, else login. Login success → shell.
  *Verify:* fresh install → log in against `https://demo.wallosapp.com` (demo/demo) → drawer shell
  appears; kill and relaunch → still logged in; enable "Don't keep activities" → process death
  restores the right screen (this is what proves `NavKeySerializers` is complete).
  **M1 done.**

---

## M2 — Subscriptions (screens 2 and 3)

Goal: the list of real subscriptions, and a detail screen.

- [ ] **2.1 — feature:subscriptions dto + domain**
  `SubscriptionDTO` per `WALLOS_API.md` §3.1 (model `cancellation_date`, *not* the misspelled
  `cancelation_date`). Domain `Subscription` + `BillingCycle` enum (`DAYS/WEEKS/MONTHS/YEARS/ONE_TIME`,
  `ONE_TIME` read-only). `CurrencyDTO` + domain `Currency`.
  *Verify:* `./gradlew :feature:subscriptions:domain:testAndroidHostTest`

- [ ] **2.2 — utils:formatter**
  `decimal`: currency formatting, and parsing `monthly_cost`-style strings with thousands
  separators. `datetime`: `YYYY-MM-DD` parse/format, and cycle + frequency → "every 6 months".
  Tests in this step.
  *Verify:* `./gradlew :utils:formatter:decimal:testAndroidHostTest :utils:formatter:datetime:testAndroidHostTest`

- [ ] **2.3 — feature:subscriptions data**
  `SubscriptionsApi` (`get_subscriptions.php`, `get_currencies.php`, `get_subscription.php`),
  mappers, `SubscriptionsRepository` returning subscriptions already joined to their currency
  symbol. Fetch-on-demand, no cache. **Never** send filters together with `all-user-subscription`.
  Fake api + repository in `:testing`.
  *Verify:* `./gradlew :feature:subscriptions:data:testAndroidHostTest`  ·  *Ref:* plan §7.1

- [ ] **2.4 — feature:subscriptions ui: list**
  `SubscriptionsRoute`, screen, state, ViewModel. Cards: logo (Coil,
  `{base}/images/uploads/logos/{logo}`), name, price + symbol, next payment, cycle text, inactive
  badge. Loading / empty / error states, pull-to-refresh. Top bar: title + `Menu`.
  *Verify:* `./gradlew :feature:subscriptions:ui:testAndroidHostTest`

- [ ] **2.5 — feature:subscriptions ui: detail**
  `SubscriptionDetailRoute(subscriptionId: Int)`, screen, state, ViewModel via
  `koinViewModel(parameters = { parametersOf(id) })`. All fields read-only. Top bar: name + `Back`,
  drawer gestures disabled.
  *Verify:* `./gradlew :feature:subscriptions:ui:testAndroidHostTest`

- [ ] **2.6 — Settings stub**
  Minimal settings screen with Disconnect (clear key → login). Second drawer item.
  *Verify:* disconnect returns to login and the key is gone.

- [ ] **2.7 — v1 acceptance**
  *Verify:* fresh install → log in → see real subscriptions → tap one → see detail → back →
  drawer → Settings → Disconnect → login. Offline shows an error, not a crash.
  **v1 done.** Next: plan §8, Phase 2b (Room, cert trust, TOTP, filters) or Phase 3 (writes).

---

## Deviations log

Anything that turned out differently from `IMPLEMENTATION_PLAN.md`. Keep it short; move anything
structural into the plan itself.

| Step | What changed | Why |
|---|---|---|
| 0.2 | Gradle wrapper 9.1.0 → 9.6.1 (plan §3.2 only mentioned AGP) | AGP 9.3.1 requires Gradle ≥ 9.5.0 — *now in plan §3.2* |
| 0.5 | Unit test task is `testAndroidHostTest`; all verify lines rewritten | Dropping `jvm()` (0.3) left the AGP KMP host test as the only test task — *now in plan §3.1, §8* |
| 0.5 | `KmpLibraryConventionPlugin` enables host tests via `withHostTestBuilder` | AGP's KMP library plugin creates no test compilation by default — `commonTest` was orphaned; *now in plan §3.1* |
| 0.5 | `configureLinting()` sets detekt's `source` to `src/` | detekt defaults to `src/main/kotlin`, so every KMP module was `NO-SOURCE`; *now in plan §3.1* |
| 0.5 | Kover keeps Android unit test instrumentation enabled | Taiga disables it because it measures `jvmTest`; we have no jvm target |
| 0.5 | `:androidApp` gets `configureTests()`/`configureLinting()` (Taiga doesn't) | Otherwise `MainActivity` and the Koin startup glue are never linted |
| 0.6 | `uikit` also takes `api(compose.components.resources)`, not just `strings` | `generateResClass = always` emits a `Res` class that won't compile without it — *folded into plan §3.3* |
| 0.6 | `core:serialization` (plan §2) not created | Not in this step's module list; 1.2 needed none either — `LocalDate.Formats.ISO` covers `date()` and the parser reads `notes` as a raw `JsonElement` — *now in plan §4.4* |
| 0.7 | CI runs no Kover/Codecov step (Taiga's `code_analysis.yml` does) | Step names four tasks; the upload needs a `CODECOV_TOKEN` this repo doesn't have — *now in plan §3.5* |
| 1.1 | `mapResult` uses `fold`; no `TimeoutCancellationException` catch clause | Taiga's null-check loses a `null` success value, and the extra clause is unreachable — *now in plan §4.3* |
| 1.2 | `parse` takes a `DeserializationStrategy`; the `reified` form is a top-level extension | A public `inline` member can't read the parser's private `Json` — *now in plan §4.2* |
| 1.2 | The parser owns its `Json`; no `@HttpJson` instance in DI | Dropping `ContentNegotiation` leaves the parser as the only JSON consumer — *now in plan §4.1, §4.2* |
| 1.2 | A body that parses but doesn't match the model → `Malformed` (plan §5.6 lets it escape) | Keeps everything leaving `core:api` a `WallosError`, so repositories catch one type — *now in plan §4.2* |
| 1.3 | `ApiKeyStorage`/`ServerUrlStorage` interfaces created here, in 1.4's module | `core:api` can't compile without them; 1.4 keeps the DataStore impls — *now in plan §4.1* |
| 1.3 | `AppInfoProvider` created in `core:appinfo-api`, which no step owns | §4.1's log-level gate needs `isDebug()`; the impl lands with the 1.11 startup glue — *now in plan §4.1* |
| 1.3 | Retry predicate reads `URLBuilder.encodedPathSegments`, and there is no `retryOnTimeout` | Ktor 3.5.1's `retryOnExceptionIf` differs from the plan's sketch — *now in plan §4.1* |
| 1.3 | `withApiKey(null)` omits `api_key` instead of sending an empty value | The server's `Missing API key` → `Unauthenticated` → setup is the right destination for a caller with no key — *now in plan §4.1* |
| 1.4 | Keystore access is a `SecretCipher` interface, not encryption inside the storage impl | The Keystore doesn't exist in a host test; as a seam the impls stay in `commonMain` and testable — *now in plan §4.7* |
| 1.4 | `core:storage`'s Koin module class lives in `androidMain` | The DataStore path needs `Context`, and Android being the only target leaves exactly one `@ComponentScan` — *now in plan §4.7* |
| 1.4 | One DataStore file for URL + key; `clear()` removes only the key | Disconnect shouldn't wipe the server the user just typed — *now in plan §4.7* |
