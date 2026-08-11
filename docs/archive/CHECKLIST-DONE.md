# WallosMobile — Build Checklist, completed steps

The ticked half of [`../CHECKLIST.md`](../CHECKLIST.md), moved here so a session reading the plan
for step N isn't reading a thousand lines of steps that are already in the code. Nothing was
reworded on the way over.

**Read it for precedent, not for instructions.** Each step's `Note:` records what that step
learned, and by the close-out ritual in `CLAUDE.md` anything *structural* in one has already been
folded into [`../IMPLEMENTATION_PLAN.md`](../IMPLEMENTATION_PLAN.md) — the plan is canonical where
the two disagree, and a note that contradicts it is a missed fold worth fixing. A frozen
step-by-step index of those folds through 6.1 lives beside this file, in
[`DEVIATIONS.md`](./DEVIATIONS.md); nothing appends to it any more (retired 2026-08-06 as
duplicate of the fold itself), so a step after 6.1 has no row there — only its own `Note:` below.

Step numbers are stable and are what the rest of the docs cite, so `1.10`, `2.3` and `3.3` mean
the same thing here as anywhere else.

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
  `koin-compose-navigation3` is **not** in the catalog yet, and 1.8's shell did not want it —
  per plan §5.5, `koinViewModel()` + `rememberViewModelStoreNavEntryDecorator()` is enough. Add it
  only if a screen proves otherwise.

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
  `HttpClient { }` uses engine autodiscovery (plan §4.1's v1 choice — 3.7 replaced it with an
  explicit engine) and okhttp is `androidMain` only, so host tests build their own
  `HttpClient(MockEngine)`. `:testing` therefore gained
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

- [x] **1.5 — strings + uikit theme**
  `:strings` with `strings.xml` + the `RString` type alias. `:uikit` with the M3 theme, colour
  scheme, typography, the `RDrawable` alias, plus **`WallosMobilePreviewTheme` and the
  `@PreviewWallosDarkLight` annotation** — every later step's previews depend on these.
  (No apostrophe escaping in `strings.xml` — CMP doesn't apply AAPT rules.)
  *Verify:* `./gradlew :uikit:assemble`
  *Note:* **`WallosMobilePreviewTheme` is only `WallosMobileTheme` + `Surface` today — 1.6 must add
  `LocalTopBarConfig provides TopBarController()` to it**, or every screen preview from 1.10 on
  crashes on a missing composition local. (Mealie also provides `LocalIsOffline`; we have no such
  local.)
  **No dynamic colour, so `uikit` has no `androidMain`**: Mealie's `expect fun colorScheme()` exists
  to reach `dynamicDarkColorScheme(LocalContext)`, which is Android-only. A static palette seeded
  from the logo navy `#233E67` keeps the brand and the
  whole module in `commonMain` — `core:logger` is still the only non-feature module with an
  `androidMain`. Reinstating dynamic colour means adding the `expect`/`actual` back.
  The generated `Res` declares **empty `drawable`/`string`/`array`/`plurals`/`font` objects
  unconditionally**, so `RDrawable` compiles with no drawable in the module at all — no icon asset
  is needed until a screen wants one (drop it in `uikit/src/commonMain/composeResources/drawable/`).
  `strings.xml` holds **only `app_name`** — later steps add their own; nothing speculative here.
  Note the launcher label is *not* this one: `androidApp/src/main/res/values/strings.xml` still says
  "Wallosmobile", and CMP resources can't feed the manifest. Rename it in 1.11 if it matters.
  `Typography` is M3's default scale, named `WallosTypography` purely as the one edit point for a
  future font. `DARK_BACKGROUND_COLOR_FOR_PREVIEW` is UPPER_SNAKE because detekt's
  `TopLevelPropertyNaming.constantPattern` demands it — Mealie `@Suppress`es the rule instead.

- [x] **1.6 — uikit: top app bar**
  Port `TopBarConfig`, `TopBarController` + `LocalTopBarConfig`, `NavigationIconConfig`,
  `TopBarAction` variants, `WallosTopAppBar` from
  `MealieMobile/uikit/.../uikit/widgets/topappbar/`.
  *Verify:* `./gradlew :uikit:assemble`  ·  *Ref:* plan §5.4
  *Note:* like 1.3, this step had to reach into a module it doesn't own: `TopBarConfig` is built on
  **`NativeText`, which lives in `utils:ui` and no step owns**. Only the three variants the top bar
  needs were written — **`Empty` / `Simple` / `Resource` plus `asString()`**; `Plural`, `Arguments`,
  `Multi`, `asStringBlocking()` and `getErrorMessage()` are Mealie's and are **not** here. **1.10
  adds `getErrorMessage(WallosError)` in this file** (CLAUDE.md's error contract) and whichever
  variants its strings need. `uikit` takes `api(projects.utils.ui)` — `NativeText` is in
  `TopBarConfig`'s public signature — so every consumer of `uikit` gets it transitively and
  shouldn't re-declare it.
  **`Icons.Filled.*` does not come transitively from material3**, so `configureKmpCompose()` gained
  `jetbrains.compose.icons` (material-icons-core, the alias 0.2 already put in the catalog); it is
  in the convention plugin, not in `uikit`, because every `ui` module will want it (Mealie does the
  same). `ui-graphics` and `animation` — the other two entries 0.4 skipped — **do** arrive
  transitively via `compose.ui` / `compose.foundation`, so `ImageVector` and `AnimatedVisibility`
  need nothing.
  `WallosMobilePreviewTheme` now provides `LocalTopBarConfig` as 1.5 required, so 1.10's screen
  previews won't crash on the missing local.
  The only unit-testable logic here is `TopBarController` (`update`/`reset`); the widget itself is
  covered by its `@PreviewWallosDarkLight` preview, since there is no Compose UI test setup yet.

- [x] **1.7 — core:navigation**
  Port `NavigationState` (dual back stack), `Navigator`, `toEntries()`, `rememberNavigationState`
  — taking `SavedStateConfiguration` as a **parameter** so this module imports no routes. Note
  `NavBackStack<NavKey>` is generic and `rememberViewModelStoreNavEntryDecorator<NavKey>()` needs
  its type argument. Port `NavigatorTest` too.
  *Verify:* `./gradlew :core:navigation:testAndroidHostTest`  ·  *Ref:* plan §5.2, §5.5
  *Note:* **`rememberNavBackStack` `require`s a non-default `serializersModule`** — it throws
  `IllegalArgumentException` on `SavedStateConfiguration.DEFAULT` at the *first* composition, not
  silently on process death as plan §5.5 warned. So **1.8's `NavKeySerializers` is a launch
  blocker, not a restore-only concern**, and the shell can't be brought up with a stub config.
  **There is no `NavigatorTest` in either reference project** (plan §5.6 says Mealie's is a
  template; it isn't there) — the 10 tests are new. They construct `NavigationState` directly:
  `NavBackStack(vararg elements: T)` is a **public constructor**, so no Compose runtime, no
  `rememberNavBackStack`, and `derivedStateOf` reads fine outside a composition.
  `Navigator` and `NavigationState` are otherwise Mealie verbatim; the only edit is
  `rememberNavigationState(startKey, topLevelKeys, configuration)`. No build file change was
  needed — 0.6's `kmp.library` + `kmp.library.compose` already carries nav3 + savedstate, and the
  module declares **no dependency block at all**.
  Two constraints this hands to 1.8: `currentSubStack` `error()`s on a missing key, so the
  `topLevelKeys` set passed to `rememberNavigationState` must cover **every** `DrawerDestination`;
  and `navigate(startKey)` from another section **clears** the top-level stack rather than popping
  to it, so Home is always depth 1. `Navigator` is not a Koin definition here — Mealie constructs
  it in the shell, and 1.8 owns that call.

- [x] **1.8 — composeApp: drawer shell**
  `DrawerDestination`, `DrawerItem`, `IconSource`, `DrawerItemsBuilder`, `RouteConfig(Provider)`,
  `MainAppState`, `WallosDrawerWidget`, `AuthenticatedMainScreen`, `MainNavHost`,
  `NavKeySerializers`. Keep the two details from §5.4: `NavigationBackHandler` composed **after**
  `MainNavHost`, and `isAnimationRunning` alongside `isOpen`. Drawer items: Subscriptions, Settings.
  *Verify:* app launches to an empty shell with a working drawer  ·  *Ref:* plan §5.4
  *Note:* the shell needs routes before either feature exists, so **`SubscriptionsRoute` and
  `SettingsRoute` are temporary `data object`s in `composeApp/nav/Routes.kt`**, rendered by a
  private `PlaceholderScreen` in `MainNavHost`. **2.4 moves `SubscriptionsRoute` to
  `feature:subscriptions:ui` and 2.6 moves `SettingsRoute`** (plan §5.3 — routes live with their
  screen); that is an import change in four files — `Routes.kt`, `DrawerDestination`,
  `RouteConfig`, `NavKeySerializers` — plus swapping the placeholder entry for the real screen.
  **Nothing here is injected**: `DrawerItemsBuilder` is a plain class defaulted into
  `AuthenticatedMainScreen`, because `startKoin` doesn't exist until 1.11 and `koinInject()` would
  throw. **1.11 should make it `@Factory` + `koinInject()`** once the graph starts, which is also
  when `composeApp` gets `kmp.di` and its `@ComponentScan` class.
  Trimmed from Mealie's shell, each because nothing in v1 feeds it: **`FabConfig`** (no writes
  before Phase 3 — `RouteConfig` carries `drawerConfig` only), `DrawerConfig.Hidden` +
  `MainAppState.showDrawer` (no fullscreen route), the snackbar host (1.10/2.4 put errors in UI
  state), and everything offline-related — there is no `NetworkMonitor` (1.4) and no
  `LocalIsOffline`. `DrawerItem.Group`/`Divider` *were* kept, per plan §5.4: v1 has two flat items,
  Phase 2b's *Manage* group is what the type exists for.
  `DrawerDestination.route` is typed `NavKey`, not Mealie's `Any` — that drops the
  `destination.route as NavKey` cast at the drawer's click site.
  **`material-icons-core` is ~50 icons**, and neither `Subscriptions` nor even `Add` is among
  them (`ArrowBack`, `List` and `Send` are `Icons.AutoMirrored.Filled.*`). Anything richer needs
  `material-icons-extended` in `configureKmpCompose()`; the drawer uses
  `Icons.AutoMirrored.Filled.List` instead.
  `MainActivity` renders one composable, **`WallosAppContent`**, which applies `WallosMobileTheme`
  — `androidApp` therefore still depends on `:composeApp` alone, and **1.11's startup branch goes
  inside `WallosAppContent`**. `composeApp` gained `kmp.serialization` (for `@Serializable` routes)
  and depends on `core:navigation`, `uikit`, `strings`.
  Tests: `NavKeySerializersTest` walks `DrawerDestination.entries` and asserts each route is in
  both `navKeySerializersModule` and `DRAWER_NAV_ITEMS` — the §5.5 "registered set matches the
  reachable set" check, as far as it can go without reflection over the entry providers.
  Verified on an emulator, not just by assembling: launch → Subscriptions, menu → drawer,
  Settings → navigates and closes the drawer, back → Subscriptions, back with the drawer open →
  drawer closes and the screen stays put.

- [x] **1.9 — feature:setup data + domain** ⭐
  `WebLoginApi`: `POST /login.php` (302 → success; `Location` contains `totp.php` → `NeedsTotp`;
  200 → `InvalidCredentials`), `GET /profile.php`, extract `id="apikey"` by regex. `SetupRepository`:
  login → scrape → **validate via `api/status/version.php`** → persist key, discard session and
  password. **Tests in this step**, over recorded HTML fixtures.
  *Verify:* `./gradlew :feature:setup:data:testAndroidHostTest`  ·  *Ref:* plan §1.1, `WALLOS_API.md` §9
  *Note:* **`WebLoginApiImpl` and `SetupRepositoryImpl` are both `@Factory`, not `@Single`** —
  1.3 made the `@WebSessionHttpClient` a `@Factory` so the session cookie dies with the attempt,
  and a `@Single` anywhere above it puts the singleton session straight back. This is the one
  repository in the app that isn't a singleton; **1.10's `LoginViewModel` gets a fresh session per
  ViewModel instance**, which is what "one session per onboarding attempt" reduces to in practice.
  **`SetupRepositoryImpl` calls `apiKeyStorage.clear()` before the bridge starts.** `withApiKey`
  `put`s, so a *stored* key overwrites the caller's own `api_key` — 1.3's note said the scraped key
  survives "with no key stored yet", and this is what makes that true. Without it a re-login
  validates the stale key and stores the new one on the strength of it.
  **`VersionDTO` was created in `feature:setup:dto`** (a third module, despite the step title):
  `WallosApiClient.post<T>` needs a response type, and the `dto` module exists for exactly this.
  `version` is nullable — setup only reads `success`, and a field missing on an older instance must
  not become a `Malformed`.
  **Path B is not here.** `SetupRepository` has only `loginWithPassword`; **1.10 must add
  `connectWithApiKey(serverUrl, key)`** (validate + persist — the tail of the bridge, ~4 lines) for
  the "I have an API key" reveal it already owns. Also absent, and deliberately: the `login.php`
  form probe for `password_login_disabled`/OIDC (plan §1.1) — no step owns it yet, and it is a
  degrade-to-Path-B affordance, not a blocker.
  The outcome table is exactly the three rows of API doc §9.2, which means **every non-302 reads as
  `InvalidCredentials`** — a 404 from a wrong subpath included. 1.10 owns "errors attributed by
  failure layer", so that mis-attribution is its to fix (validating against `api/status/version.php`
  is what catches a wrong URL on Path B; Path A fails one step earlier).
  `remember` is not sent: the session is discarded immediately, so a longer-lived cookie only
  widens the window in which one exists. A blank `value=""` on the `apikey` input counts as *no*
  key — an account that never generated one still renders the input, and storing `""` looks
  connected while failing every call. Fixtures are Kotlin constants in `commonTest`
  (`WallosHtmlFixtures.kt`); `commonTest` has no portable way to read a resource file.
  **No fakes went to `:testing`** — `FakeWebLoginApi`/`FakeApiKeyStorage`/`FakeServerUrlStorage`
  are private to `SetupRepositoryImplTest`, as in 1.4. 1.10 needs `FakeSetupRepository`, which is
  the second consumer that earns the move (plan §6.1).

- [x] **1.10 — feature:setup ui**
  `LoginRoute` (`@Serializable ... : NavKey`), `LoginScreen`, `LoginState` (data + callbacks),
  `LoginViewModel`. Fields: server URL, username, password + visibility toggle, Connect. Secondary
  "I have an API key" reveal. Errors attributed by failure layer (§1.1). `NeedsTotp` → message
  pointing at the key field. No top bar, no drawer.
  *Verify:* `./gradlew :feature:setup:ui:testAndroidHostTest`
  *Note:* **`LoginRoute` is not in `NavKeySerializers` yet** — registering it means `composeApp`
  depending on `feature:setup:ui`, which is 1.11's wiring. **1.11 must either register it or
  decide the login screen never enters a `NavDisplay` back stack at all** (plan §7.1's startup
  branch reads as an either/or *above* the shell, not a route inside it). That decision is the
  one thing 1.10 deliberately left open, and CLAUDE.md's "every new route is registered" rule is
  what makes forgetting it silent.
  Two modules no step owns had to grow again, as in 1.6: **`utils:ui` gained `getErrorMessage`
  (the §1.1 failure-layer attribution) and `ObserveAsEvents`**, so it now depends on `core:domain`
  and `strings`. `getErrorMessage`'s inner `when` is exhaustive over the sealed `WallosError`
  on purpose — a new error type fails the build instead of quietly reading as "check your
  connection". Attribution lands as three buckets: `Malformed`/`UnsupportedEndpoint` and any
  non-`WallosError` → the **URL**, `Unauthenticated` → the **key**, the rest → their own message.
  **`ApiKeyNotFound` is handled in the ViewModel, not in `getErrorMessage`** — it lives in
  `feature:setup:domain`, which `utils:ui` must not see.
  **`SetupRepository.connectWithApiKey(serverUrl, key)` was added** as 1.9 required — Path B is
  `saveServerUrl` → `clear()` → validate → `setKey`, and the `clear()` is load-bearing for the
  same reason as in the bridge. It returns `Result<Unit>`: there is no web session, so no
  `LoginOutcome` to report.
  **`material-icons-core` carries no `Visibility`/`VisibilityOff`**, so the password toggle is a
  Show/Hide `TextButton` in `trailingIcon` — cheaper than pulling `material-icons-extended` into
  every `ui` module for one glyph. The icon set is now 0 for 3 (1.8: `Subscriptions`, `Add`).
  `LoginUiState` holds **both paths at once** with an `isApiKeyMode` flag and a `canConnect`
  computed only from the *visible* path's fields; switching paths clears the error, since it was
  attributed to fields the user can no longer see. On success the ViewModel blanks `password` in
  state — the key is the only thing meant to outlive the attempt.
  `:testing` gained **`MainDispatcherRule`** (plan §6.1): `viewModelScope` dispatches on
  `Dispatchers.Main`, which no host test has, so every future ViewModel test needs it. It is not a
  JUnit `@Rule` — `commonTest` is `kotlin.test`, so the test calls `setup()`/`tearDown()` from
  `@BeforeTest`/`@AfterTest`. **`FakeSetupRepository` did *not* go there**, against 1.9's note:
  CLAUDE.md's rule is that a double used by exactly one test file stays private to it, and
  `:testing` is on *every* module's test classpath, so a `feature:setup:domain` dependency there
  leaks a feature into modules that have no business seeing it.
  `feature:setup:ui` takes `core:domain` in **`commonTest` only** — production code never names a
  `WallosError`, it hands whatever it caught to `getErrorMessage`; the tests construct them to pin
  the attribution down. `utils:ui` reaches the module through `uikit`'s `api`, as 1.6 set up.

- [x] **1.11 — Wire it up end to end**
  Startup branch: stored key → shell, else login. Login success → shell.
  Plus the **Koin graph test**: `koin-test` is in the catalog and unused; this is the first step
  where every definition resolves, so add a host test that runs `checkModules()`/`verify()` over
  the app's module set. Missing definitions are otherwise a launch-time crash no gate catches —
  and `composeApp` picks up `kmp.di` here anyway, for `DrawerItemsBuilder` (1.8).
  *Verify:* the graph test passes, **and** fresh install → log in against the local instance in
  `docs/local-info.txt` → drawer shell appears; kill and relaunch → still logged in; process
  death restores the right screen (this is what proves `NavKeySerializers` is complete).
  *Note:* **the startup branch is state, not a route.** 1.10 left the either/or open; login never
  enters a back stack, so `LoginRoute` was **deleted** (with `kmp.serialization`, which it was the
  only reason for) and `NavKeySerializers` registers nothing for it. The branch is
  `ApiKeyStorage.isConnected` collected in `WallosAppContent` — one source of truth, so
  **2.6's Disconnect needs no navigation at all**: `clear()` flips the flow and the tree swaps.
  That also made `LoginViewModel.connectedEvent` / `LoginScreen(onConnectSuccess)` redundant, and
  both are gone; three of 1.10's tests now assert on state instead of a Turbine event.
  **The branch must render the shell on the *first* composition, and that cost a real bug.**
  `isConnected` has no value for frame 1, so waiting for it pushed `AuthenticatedMainScreen` into
  a second pass — and `rememberNavBackStack` only consumes its restored state in the first one.
  The app survived process death but silently came back to the start destination with the stack
  gone. Fixed by seeding the branch from `rememberSaveable`. **The checklist's own
  "Don't keep activities" test passes either way** — only `adb shell am kill` on a backgrounded
  app catches it, which is the process-death check every later step should use.
  `verify()` (not `checkModules()`, which instantiates) reads a definition through its *bound
  type's* constructor, so for `@Single fun provide…` factories it inspects the returned class
  instead of the function's parameters — hence `HttpClientEngine` in the test's `extraTypes`
  beside the two types `:androidApp` really does supply. Confirmed the test bites: dropping one
  `includes` line fails it by name. `AppModule`'s `includes` list is **not** optional — the
  compiler plugin only auto-gathers `@Configuration` modules from the compilation that calls
  `startKoin`, which is why `androidApp`'s `AndroidModule` needs no entry and every other module
  does.
  Manifest gained `INTERNET` and — flagged, since it is security-relevant and blanket —
  **`usesCleartextTraffic="true"`**: a self-hosted instance on plain HTTP is the normal case, and
  without it the app cannot reach one. Plan §9's "warn on non-HTTPS and steer to Path B" is still
  unowned. `StorageModule` is referenced from `composeApp/commonMain` despite living in
  `androidMain` (Android-only target); a second target turns that into Mealie's
  `expect class PlatformStorageModule`.
  Verified on the emulator against the local instance, both paths: Path A (`login.php` 302 →
  `profile.php` scrape → `api/status/version.php` validate) and Path B (pasted key), then kill +
  relaunch, then `am kill` process death from a Settings-deep stack. `TimberLogger.install()` is
  now called (1.1 left it dangling), so `logcat { }` finally emits.
  **M1 done.**

---

## M2 — Subscriptions (screens 2 and 3)

Goal: the list of real subscriptions, and a detail screen.

- [x] **2.1 — feature:subscriptions dto + domain**
  `SubscriptionDTO` per `WALLOS_API.md` §3.1 (model `cancellation_date`, *not* the misspelled
  `cancelation_date`). Domain `Subscription` + `BillingCycle` enum (`DAYS/WEEKS/MONTHS/YEARS/ONE_TIME`,
  `ONE_TIME` read-only). `CurrencyDTO` + domain `Currency`.
  *Verify:* `./gradlew :feature:subscriptions:domain:testAndroidHostTest`
  *Note:* the shapes were checked against the live instance with `curl`, not just against the doc,
  and three things came back that `WALLOS_API.md` §3.1 didn't say — **now written into it**:
  **`name`, `notes` and the resolved `*_name` fields are HTML-escaped** (`1&amp;1 Telekom`), so
  **2.3's mapper has to unescape or 2.4 renders the entity**; **unset dates are `""`, not `null`**
  (`cancellation_date` on every active row, `start_date` on older ones), so blank must read as
  absent; and `logo_url` is an **MCP invention** — over HTTP there is only the bare `logo`
  filename, which 2.4 turns into a URL itself. Currency `rate` really is a **String** and `in_use`
  a real **boolean**.
  **`BillingCycle.fromCode` returns `null`** for an unknown code rather than defaulting, so
  `Subscription.cycle` is nullable and 2.4 drops the cycle text instead of guessing "months".
  All three domain dates are `LocalDate?` for the same reason — the mapper stays total and one bad
  date can't sink the list.
  **The domain model is narrower than the DTO**: exactly the fields §7.1's list and detail screens
  name, so no `auto_renew`, `notify*`, `replacement_subscription_id`, `payment_method_id`,
  `payer_user_id` or `category_id`. Same for `Currency` — no `rate`, no `in_use` (conversion is
  Phase 2b, the in-use flag guards deletes in Phase 5). Add fields with the screen that needs them.
  **`Subscription` carries no `currencySymbol`** — 2.3 owns the join and therefore its shape
  (a field on the model, or a separate `currency_id → symbol` map).
  No response envelopes here (`SubscriptionsResponse` etc.): 1.9's precedent is that the `dto`
  module grows them when `data` needs a response type, which is 2.3.

- [x] **2.2 — utils:formatter**
  `decimal`: currency formatting, and parsing `monthly_cost`-style strings with thousands
  separators. `datetime`: `YYYY-MM-DD` parse/format. ~~cycle + frequency → "every 6 months"~~ —
  **moved to 2.4**, see the note. Tests in this step.
  *Verify:* `./gradlew :utils:formatter:decimal:testAndroidHostTest :utils:formatter:datetime:testAndroidHostTest`
  *Note:* **the cycle text can't live here: it would point `utils/` at `feature/`.** "every 6
  months" is a function of `BillingCycle`, which 2.1 put in `feature:subscriptions:domain`, and
  the layering is `feature/` → `core/` → `utils/`. The alternatives were a duplicate cycle enum
  owned by `datetime` (a mapping to maintain for one string) or typing it as
  `DateTimeUnit.DateBased` (which has no `ONE_TIME`); both lose to resolving a plural in the
  composable that renders it, which is where a `pluralStringResource` wants to be anyway. **2.4
  owns it**, and it needs no formatter at all — the plurals go in `:strings`. Nothing about
  "every 6 months" was ever a date computation; `datetime` was just a place to put it.
  Both formatters are **plain `@Single` classes, not interface + impl** — the repo's interfaces
  exist for platform or IO seams (`SecretCipher`, `ApiKeyStorage`) and a fake formatter would only
  make a consumer's test assert against wrong output. Same reasoning as CLAUDE.md's mappers-are-
  classes rule. Neither is in `AppModule`'s `includes` yet: nothing injects them until 2.3, and an
  unused `@Configuration` there is a definition `verify()` walks for no reason. **2.3 must add
  `DecimalFormatterModule` *and* `DateTimeFormatterModule`** — a missing `includes` line compiles
  and crashes at first injection.
  `MoneyFormatter` fixes the separators at `1,234.56` instead of reading the device locale,
  because that is what the instance itself renders (`localized_monthly_cost` is `en_US`
  "regardless of the user's language", API doc §3.5) — a device-formatted price would disagree
  with every total the same server shows. Locale-aware money needs `expect`/`actual` over the
  platform `NumberFormat`, which also puts it beyond a host test. The symbol is a **parameter**,
  not derived from a code: Wallos lets it be any string.
  **`kotlin.math.round` breaks ties towards the even integer**, so it renders an exact `0.125` as
  `0.12` while PHP's `number_format` says `0.13`; `floor(x + 0.5)` is the half-up the server uses.
  Caught by a test, not by review.
  `DateFormatter.parseIsoDate` returns `null` for blank *and* for unparseable, so 2.3's mapper
  needs no `try`: `DateTimeFormatException` is an `IllegalArgumentException`, which is the same
  trick `core:api` uses to catch a `SerializationException` without a bare `catch (Exception)`.
  A **display** date format ("5 Mar 2026") is deliberately absent — the step named ISO parse/format
  only. **2.4/2.5 decide it**, and note it is the one thing in these modules that genuinely wants
  `expect`/`actual` (there is no locale-aware date formatting in `commonMain`).

- [x] **2.3 — feature:subscriptions data**
  `SubscriptionsApi` (`get_subscriptions.php`, `get_currencies.php`, `get_subscription.php`),
  mappers, `SubscriptionsRepository` returning subscriptions already joined to their currency
  symbol. Fetch-on-demand, no cache. **Never** send filters together with `all-user-subscription`.
  Fake api + repository in `:testing`.
  *Verify:* `./gradlew :feature:subscriptions:data:testAndroidHostTest`  ·  *Ref:* plan §7.1
  *Note:* **the join shape 2.1 deferred is a `currencySymbol` field on domain `Subscription`**, so
  2.4/2.5 render a price from the model alone and never see a currency map. The cost is that
  *every* repository call is **two round trips** — `get_currencies.php` is re-read per call because
  "no cache" means no cache — so opening the list and then a detail is four requests. Phase 2b's
  Room cache is where that stops; don't paper over it with a field in the ViewModel.
  Subscriptions are fetched **first**, so a failure on the resource the user asked for
  short-circuits before the currencies call.
  **No `SubscriptionQuery` type was created**, though plan §6.1's fake sketch has one:
  `get_subscriptions.php` gets `api_key` and nothing else. Filtering and sorting are client-side
  and the server already defaults to `next_payment`. That is also what keeps §3.2's
  `all-user-subscription` + filter SQL bug unreachable — neither side of the combination exists —
  and **2.4/Phase 2b must keep it that way** when filters arrive.
  **`HtmlUnescaper` is a `@Single` class in `feature:subscriptions:mapper`**, reversing PHP's
  `htmlspecialchars` over `name`/`notes`/`*_name` and over currency `name`/`symbol`. `&amp;` is
  decoded **last**, or `&amp;lt;` (the literal text `&lt;`) silently becomes a `<`. Every future
  mapper of Wallos text needs it.
  The response envelope DTOs (`SubscriptionsResponse`, `SubscriptionResponse`,
  `CurrenciesResponse`) carry **no defaults**, unlike the row DTOs: an empty user really does get
  `"subscriptions":[]` (checked against the live instance), so an *absent* key is a broken response
  and `Malformed` is a truer answer than an empty-state screen. `main_currency` is on the wire and
  deliberately unmodelled — it exists for the Phase 2b conversion hint.
  **`AppModule` gained four `includes`**: `SubscriptionsDataModule`, `SubscriptionsMapperModule`,
  and `DateTimeFormatterModule` + `DecimalFormatterModule` as 2.2 required. `MoneyFormatter` has no
  injector until 2.4, but `verify()` walking a no-arg `@Single` costs nothing and a forgotten
  `includes` line is a runtime crash.
  **The fakes stayed private to their test files**, against this step's own text — same call as
  1.10: `:testing` is on every module's test classpath, so parking them there drags
  `feature:subscriptions:{dto,domain}` into modules with no business seeing them.
  **2.4 owns the `FakeSubscriptionsRepository` decision** and will face exactly 1.10's question
  (one consumer module, `feature:subscriptions:ui`, but two test files in it).
  `feature:subscriptions:data` takes `core:storage` and `utils:formatter:datetime` in
  **`commonTest` only** — the api test has to fake an `ApiKeyStorage` to build a `WallosApiClient`
  over `MockEngine`, and the mappers are constructed rather than injected.
  One test trap worth knowing: a `StandardTestDispatcher()` built outside `runTest` has its own
  scheduler, and `withContext(it)` dies with "Detected use of different schedulers".
  `UnconfinedTestDispatcher()` doesn't, which is why every repository test here uses it.

- [x] **2.4 — feature:subscriptions ui: list**
  `SubscriptionsRoute`, screen, state, ViewModel. Cards: logo (Coil,
  `{base}/images/uploads/logos/{logo}`), name, price + symbol, next payment, cycle text, inactive
  badge. Loading / empty / error states, pull-to-refresh. Top bar: title + `Menu`.
  **Also owns cycle + frequency → "every 6 months"** (moved here from 2.2, which can't see
  `BillingCycle`): plurals in `:strings`, resolved with `pluralStringResource` in the composable,
  and no text at all when `cycle == null`. Decide the **display** date format here too — 2.2 ships
  ISO only.
  *Verify:* `./gradlew :feature:subscriptions:ui:testAndroidHostTest`
  *Note:* **the display date format went back into `utils:formatter:datetime`** as
  `DateFormatter.formatDisplayDate` ("5 Mar 2026"), not into the composable: kotlinx-datetime ships
  `MonthNames.ENGLISH_ABBREVIATED`, so it needs no resource table and no `expect`/`actual`, and it
  stays host-tested. Hard-coded English on the same terms as `MoneyFormatter`'s `1,234.56` — the
  day the app is translated, both move together. `day()` pads to two digits by default;
  `day(Padding.NONE)` is what gives `5 Mar` rather than `05 Mar`. **2.5 should use the same call.**
  The cycle text genuinely can't leave the composable, as 2.2 predicted: `SubscriptionUiItem`
  carries the `BillingCycle` + `frequency` and `SubscriptionCard` resolves the plural.
  `:strings` gained four `<plurals>` and a **`RPlurals` typealias** (`Res.plurals`) beside `RString`
  — CMP supports plurals, and `pluralStringResource(RPlurals.x, n, n)` needs the count twice (once
  to pick the form, once as `%1$d`).
  **The ViewModel builds the logo URL**, so `feature:subscriptions:ui` takes `core:api` for
  `BaseUrlProvider` alone — the bare filename plus the normalized instance root, guarded so a blank
  root yields no URL rather than a relative one. That is the only reason a `ui` module names a
  `core:api` type; if a second screen needs it, consider whether the seam belongs elsewhere.
  Coil follows MealieMobile — `coil.compose` + `coil.ktor` in `commonMain`, no `ImageLoader` setup.
  The ktor3 fetcher's engine autodiscovery finds okhttp on the runtime classpath and logos load off
  the live instance; Wallos serves `images/uploads/logos/` **unauthenticated**, so no header plumbing.
  **A failed load clears the list** — no cache means there is nothing behind the error worth
  keeping, and the retry button is the way back. Revisit when Phase 2b's Room cache lands.
  `PullToRefreshBox` (material3, `@OptIn(ExperimentalMaterial3Api::class)`) wraps a `LazyColumn`
  that is **always composed**, with loading/error/empty drawn on top — an empty state that isn't a
  scrollable has nothing to pull.
  `SubscriptionsScreen` takes **no click callback**: the list has nowhere to go until 2.5, so
  `subscriptionsEntry()` in `composeApp/nav/entries/` takes no `Navigator` yet either.
  Wiring: `SubscriptionsRoute` moved out of `composeApp/nav/Routes.kt`, which left one declaration
  behind and therefore **had to be renamed `SettingsRoute.kt`** (detekt's `MatchingDeclarationName`);
  2.6 deletes it. `AppModule` gained `SubscriptionsUiModule` — `KoinGraphTest` resolves the new
  ViewModel.
  Verified on the emulator against the live instance: 30-odd real rows with logos, `€` prices,
  `Every 3 months`, `1&1 Telekom` unescaped (2.3's `HtmlUnescaper` end to end), inactive badges;
  pull-to-refresh indicator; airplane mode → error + Try again → recovered.
  **Two `am kill` cycles in a row lie.** Each `monkey … LAUNCHER` after an `am kill` adds another
  `MainActivity` to the task (`numActivities=3`), and the relaunch then starts a *fresh* activity
  instead of restoring the killed one — the check silently passes nothing and looks like a
  restore regression. **`am force-stop` first** to reset the task, then do the cycle once.

- [x] **2.5 — feature:subscriptions ui: detail**
  `SubscriptionDetailRoute(subscriptionId: Int)`, screen, state, ViewModel via
  `koinViewModel(parameters = { parametersOf(id) })`. All fields read-only. Top bar: name + `Back`,
  drawer gestures disabled.
  *Verify:* `./gradlew :feature:subscriptions:ui:testAndroidHostTest`
  *Note:* **the route parameter needs `@InjectedParam` on the ViewModel's constructor property**, or
  the Koin compiler plugin looks for an `Int` definition in the graph and the screen crashes at
  first injection. `KoinGraphTest` would **not** have caught it: `verify()` whitelists `String`,
  `Int`, `Long` and `Double` unconditionally (`Verify.primitiveTypes`), so a primitive constructor
  parameter is always "verified" whether or not it is annotated. MealieMobile takes route params
  without the annotation and has no graph test to disagree with it — don't read that as precedent.
  The screen **re-reads its own row** rather than being handed one from the list: with no cache
  (plan §7.2) the alternative is a snapshot from whenever the list last refreshed. The cost is that
  opening a detail is two more round trips (2.3's currency join), which is 2.4's note in a second
  place — one more thing Phase 2b's Room cache collects.
  `SubscriptionDetailRoute` is the first route in the app carrying data, and therefore the first
  thing in `navKeySerializersModule` that `NavKeySerializersTest` can't check: that test walks
  `DrawerDestination.entries`, and a detail route is not a drawer destination. **A non-top-level
  route missing from the module is once again the silent process-death-only failure**, and the only
  gate on it is the manual `am kill` cycle.
  Three things moved into a shared `ui/widgets/` package rather than being written twice —
  `SubscriptionLogo` (now taking a `size`, 48.dp on the card and 96.dp in the detail header),
  `InactiveBadge`, and `cycleText`, which 2.4 had parked as a private composable in
  `SubscriptionCard.kt`. The logo *URL* builder moved the same way, to `ui/LogoUrl.kt` as
  `internal fun BaseUrlProvider.toLogoUrl(logo)`; it is a two-line helper over an injected seam,
  not a mapper, so CLAUDE.md's mappers-are-classes rule doesn't reach it.
  **`onSubscriptionClick` is a plain parameter on `SubscriptionsScreen`, not a field in
  `SubscriptionsUiState`** — CLAUDE.md's exception for pure navigation callbacks, wired by
  `subscriptionsEntry(navigator)`, which now takes a `Navigator` as 2.4 predicted.
  A `DetailRow` whose value is blank is **left out entirely**: `notes`, `url` and `start_date` are
  all `""` on real rows (2.1), and a label over an empty value reads as a bug. `notes` and `url` are
  in fact empty on *every* row of the local instance, so those two are covered by preview and unit
  test only.
  Verified on the emulator against the live instance: list → Disney+ → detail with the logo, `€8.99`,
  `Every month`, `10 Mar 2026`, Entertainment / Apple Pay / Tomiris and no Start date row (the row
  really has `start_date: ""`); Fiton shows `31 Jan 2024` and `Health & Wellbeing`. A left-edge
  swipe on the detail runs the system back gesture instead of opening the drawer, which is
  `DrawerConfig.GesturesDisabled` doing its job. `am force-stop` → detail → `am kill` → relaunch
  restores the detail screen with its id.

- [x] **2.6 — Settings stub**
  Minimal settings screen with Disconnect (clear key → login). Second drawer item.
  *Verify:* disconnect returns to login and the key is gone.
  *Note:* the second drawer item was already there (1.8) — what this step owed it was a screen.
  **A new module, `feature:settings:ui`, and only `ui`**: plan §2's tree has
  `settings/ data domain dto ui`, but disconnect is one call on one storage seam, so the ViewModel
  takes `ApiKeyStorage` directly (CLAUDE.md: a single call goes straight from the ViewModel) and
  the module has no `domain`, no repository and no use case. Phase 5's server display settings are
  what grow the other three. The dependency on `core:storage` from a `ui` module is the second of
  its kind after 2.4's `core:api`; both are a ViewModel naming a `core` seam with no feature layer
  in between, and a third should prompt the question of whether the seam is in the right place.
  `SettingsRoute` moved out of `composeApp/nav/`, so **`composeApp` now declares no route of its
  own** — `PlaceholderScreen` and `SettingsRoute.kt` are both gone, `nav/entries/` has the second
  of its two files, and `MainNavHost` names no screen at all. `composeApp` keeps
  `kmp.serialization` for `navKeySerializersModule` alone.
  **Disconnect is not asynchronous from the screen's side and has no error state.** `clear()`
  flips `isConnected` and the startup branch swaps the tree (1.11), so a spinner would have
  nowhere to render; a failed write is logged and leaves the user connected, which is the honest
  outcome. There is **no confirmation dialog** — the step said minimal, and it is one line to add
  if it turns out to be wanted.
  **1.4's "re-login is one field" does not hold, and the fix isn't here.** `clear()` really does
  keep the server URL, but `LoginUiState` starts blank and nothing seeds it from
  `ServerUrlStorage`, so the user retypes the URL anyway. The first draft of the Disconnect copy
  promised otherwise and was reworded to what the app does ("Nothing changes on the server").
  Prefilling belongs to `feature:setup:ui`, not to a settings stub — **2.7 or a follow-up**.
  Verified on the emulator: drawer → Settings → Disconnect → login screen, then force-stop and
  relaunch → still the login screen (the key is gone, not just the state); logged back in with
  Path A; `am force-stop` → Settings → `am kill` → relaunch restores Settings, which is the only
  gate there is on the route's new package being registered in `NavKeySerializers`.

- [x] **2.7 — v1 acceptance**
  *Verify:* fresh install → log in → see real subscriptions → tap one → see detail → back →
  drawer → Settings → Disconnect → login. Offline shows an error, not a crash.
  **v1 done.** Next was plan §8's Phase 2b or Phase 3; **Phase 2b won and is decomposed below as
  M3** — it discharges the cache debt 2.3/2.4/2.5 each deferred to it, and Phase 3's writes need
  the `NetworkMonitor` it brings before `CLAUDE.md`'s "offline disables writes" rule can hold.
  *Note:* the acceptance run passed end to end on the emulator against the local instance —
  uninstall → install → Path A login → 30-odd real rows with logos → Fiton detail (`Health &
  Wellbeing` unescaped, `31 Jan 2024` start date, no blank rows) → back → drawer → Settings →
  Disconnect → login, then `am force-stop` + relaunch to prove the key was gone rather than just
  the state. Airplane mode → "Couldn't reach that server", Try again after reconnecting →
  the list back. No `FATAL`, no crash and nothing unexpected in logcat across the whole run.
  Two things the run taught, neither a code change:
  **Disconnect looks like it prefills the login form, and doesn't.** The URL and username are
  still there straight after Disconnect because the same `LoginViewModel` is still in the
  activity's `ViewModelStore`; cold-start and they are blank. So 2.6's open item is real but
  narrower than it read — the gap is only visible across a process boundary. Still unfixed, and
  now parked below rather than in the deviations log.
  **`adb shell input text` needs a pause between fields**, or a long password arrives truncated
  and the login fails as `InvalidCredentials`. That reads exactly like a wrong credential, and
  the honest diagnosis is to count the dots in the field against a known-good screenshot. Useful
  accident: it exercised 1.9's outcome table and 1.10's error attribution against the live server.
  On the deferred items: the backward-compatibility bullet **stays** (nobody but us has installed
  this), the Kover floor and the Compose UI test setup **stay parked** for the reason 2.4 gave,
  and the guardrails were built — see the deviations row.

  **Two things are deliberately deferred to here rather than done early:**
  - **Delete the pre-v1 backward-compatibility bullet from `CLAUDE.md`'s Non-negotiables** the
    moment this app is installed by anyone but us. From then on the stored API key and the
    serialized nav back stack are real user state: a renamed DataStore key or a moved route class
    needs a migration, not a shrug.
  - **Verification we chose to grow into, not front-load** (one per step that needs it, never a
    big-bang): a Kover floor that fails under the current number, and a Compose UI test setup.
    The *fork* in that second one is now decided — **instrumentation, not Robolectric** (see
    `CLAUDE.md`); what is still open is **when**, and it stays parked until a screen's logic
    first outgrows its ViewModel test. 2.4 (list: loading / empty / error, pull-to-refresh) is
    the first plausible candidate. Two things to carry into it: instrumented tests need an
    emulator job in CI or they are a local-only gate like Kover, and
    `ActivityScenario.recreate()` does **not** cover process death — the `am kill` check in
    `CLAUDE.md` stays manual whatever gets added.
  - **Agent guardrails — investigate and pick one or two.** The gates in this repo constrain the
    *code* an agent writes; nothing yet constrains an agent from weakening a gate to make its own
    step pass. The tripwire set is small and mechanizable: a step commit touching
    `.github/workflows/ci.yml`, `build-logic/**`, `config/detekt/**`, `.editorconfig`,
    `gradle/libs.versions.toml`, or introducing `@Ignore` / `@Suppress` / a detekt baseline /
    a widened `paths-ignore`, should have to say so out loud. `CLAUDE.md` and this file belong on
    that list too — they set the rules every future session runs under, so an unannounced edit to
    them is the highest-leverage change an agent can make. First candidate: a CI job that diffs
    those paths and fails unless the commit message opts in.
    **Done, as the first candidate**: `.github/workflows/guardrails.yml` +
    `.github/scripts/check-guardrails.sh`. See the deviations row for the two things the
    investigation changed about the shape above, and for what it can't do.

---

## M3 — Hardening and offline (plan §8, Phase 2b)

Goal: the list survives a lost connection, and an instance behind a homelab certificate can be
reached at all. **Done when** the list renders offline after one online fetch, *and* a self-signed
instance connects — plan §8's own two conditions.

No writes in this milestone. Phase 3 is the first one that sends anything, and it depends on 3.2
existing (`CLAUDE.md`: "Offline → **disable** write actions", which needs a `NetworkMonitor` the
app doesn't have yet).

Two things that constrain the steps below, worth knowing before starting any of them (a third,
about wiring Room and KSP, was 3.3's and is spent):

- **Filtering and sorting stay client-side**, over whatever the cache holds. 2.3 chose this and
  told Phase 2b to keep it: v1 sends `api_key` and nothing else, and with neither filters nor
  `all-user-subscription` on the wire, `WALLOS_API.md` §3.2's "no `WHERE` clause" SQL bug is
  unreachable *by construction*. Sending server-side filters would put it back within reach for
  one admin account and no benefit — there is no pagination to save.
- **Any step that touches `build-logic/`, `gradle/libs.versions.toml`, `config/detekt/` or
  `.github/` needs a `Gate-change:` line in its commit** (2.7). 3.2 and 3.3 both did.

- [x] **3.1 — feature:setup ui: prefill the server URL, warn on cleartext**
  Seed `LoginUiState.serverUrl` from `ServerUrlStorage` when `LoginViewModel` is constructed, so
  the URL `clear()` deliberately keeps (1.4) is actually offered back after a Disconnect. Plus
  plan §9's unowned risk: when the typed URL is `http://`, show a warning and **steer to Path B**
  — the password crosses the wire in the clear on Path A, and the API key does not.
  *Verify:* `./gradlew :feature:setup:ui:testAndroidHostTest`, and on the emulator: Disconnect →
  force-stop → relaunch → the URL is there.  ·  *Ref:* plan §9, "Still open after v1"
  **The warning must not block Path A.** `docs/local-info.txt`'s instance is plain HTTP and is the
  only one every later `Verify:` line can use — a hard block here makes M3 untestable. Warn, don't
  disable.
  *Note:* **the prefill goes through `SetupRepository.getStoredServerUrl()`, not through
  `ServerUrlStorage` in the ViewModel.** That would have been the *third* `ui` → `core` reach, and
  CLAUDE.md says the third is the point to ask whether the seam is right: unlike `feature:settings`
  (2.6), this feature has a real `data` layer already, so reaching past it was the wrong half of
  the rule to apply. **3.9 and 3.10 extend the same repository** — keep them there too.
  It returns **`Result<String>`**, matching its two siblings, for one concrete reason: a DataStore
  read can throw, an exception out of `viewModelScope` is a crash, and taking the result as a bare
  `String` would mean `resultOf` — i.e. `core:domain` — in `feature:setup:ui`'s `commonMain`, which
  1.10 kept to `commonTest` on purpose.
  The read is `suspend` and lands **after** the first composition, so `onStoredServerUrl` fills the
  field only when the user hasn't typed into it yet. That is *not* 1.11's
  `rememberNavBackStack` trap in miniature — a text field may arrive a pass late, a back stack may
  not — but the guard is what keeps a slow read from eating what the user typed over it.
  **The cleartext warning is `LoginUiState.isCleartextWarningVisible`**, computed from `serverUrl`
  exactly as `canConnect` is, and **hidden on Path B**: it exists to steer, and once steered there
  is nothing left for the user to act on. It touches `canConnect` nowhere — verified on the
  emulator with the warning on screen *and* Connect enabled, then logging in through it.
  Only a literal `http://` prefix counts. Nothing in the app infers a scheme (`BaseUrlProviderImpl`
  trims and adds a trailing slash, no more), so a scheme-less `10.0.2.2:8282` warns about nothing —
  it also doesn't connect, so **3.10's login-form probe is the place to look at scheme handling**,
  not here. Colour is `colorScheme.error`: M3 has no warning role and the palette's `tertiary` is a
  mauve that reads as decoration.
  Verified on the emulator against the live instance: Disconnect → `force-stop` → relaunch →
  `http://10.0.2.2:8282` is back in the field with the warning under it; Path B hides the warning
  and keeps the URL; Path A then logged in against the prefilled value **without the field being
  touched**. 2.6's reworded Disconnect copy still holds — nothing about the server changed.

- [x] **3.2 — core:storage: NetworkMonitor**
  `NetworkMonitor` interface in `commonMain` + an `androidMain` `ConnectivityManager`
  implementation, exactly the seam shape `SecretCipher` uses (1.4) and for the same reason: the
  platform API doesn't exist in a host test. Port from
  `TaigaMobileNova/core/storage/.../network/`, dropping its `iosMain`/`jvmMain` actuals. Provide
  it to the tree as `LocalIsOffline` from the shell — 1.8 recorded its absence as a deliberate gap
  and this is the step that closes it. `FakeNetworkMonitor` goes to `:testing`: 3.4, 3.5 and every
  Phase 3 write screen are all consumers, which is the bar 1.10 set for that module.
  *Verify:* `./gradlew :core:storage:testAndroidHostTest`, and on the emulator the state flips
  with `adb shell cmd connectivity airplane-mode enable`.  ·  *Ref:* plan §4.7, §6.1
  *Note:* **this step adds no host test, and couldn't.** Both halves are unreachable from one: the
  impl needs `ConnectivityManager` (which is the whole reason for the seam) and the shell wiring
  needs a Compose UI test, which is instrumented and deferred. `:core:storage:testAndroidHostTest`
  therefore only re-runs 1.4's suite — the emulator half is the *only* real check here, and the
  first consumer with assertable behaviour is 3.4.
  Verified by a **temporary** `logcat` in the callback, added and removed within the step: airplane
  on gave `onLost -> isOnline=true` then `onLost -> isOnline=false`, airplane off gave
  `onAvailable -> isOnline=true`. That first line is the `networks` set earning its keep — the
  emulator has two networks with `NET_CAPABILITY_INTERNET`, so losing one is not offline. A monitor
  written against `activeNetwork` alone would have reported offline there and been wrong.
  **`ACCESS_NETWORK_STATE` went into `:androidApp`'s manifest**, the only one in the repo, so the
  impl keeps Taiga's `@SuppressLint("MissingPermission")` — `core:storage` has no manifest to
  declare it in. (`@SuppressLint(` is not `@Suppress(`, so this does not trip the guardrail.)
  `LocalIsOffline` lives in `uikit/widgets/network/` per Mealie and had to be added to
  `.editorconfig`'s `compose_allowed_composition_locals` — a `Gate-change:` commit, and the reason
  3.5 and every Phase 3 write screen won't need one.
  Two shape choices worth not re-litigating: `NetworkMonitor` is `commonMain`-flat, not in a
  `network/` subpackage as in Taiga (this module has six flat files and `@ComponentScan` is
  unaffected); and `AuthenticatedMainScreen` takes it as a **`koinInject()` default parameter** and
  collects it itself, rather than Mealie's `isOnline: Boolean` passed down from the app content —
  `WallosAppContent` renders login too, and threading connectivity through it would put the value
  above the startup branch for no reader. It is a `StateFlow`, so `collectAsState()` has a value in
  the first composition and 1.11's `rememberNavBackStack` trap stays shut.

- [x] **3.3 — core:storage: Room**
  The database only: `WallosDB`, `SubscriptionEntity`, `CurrencyEntity`, their DAOs and the
  `BundledSQLiteDriver` wiring. No repository change — 3.4 owns that. Apply `libs.plugins.ksp` +
  `libs.plugins.androidx.room` to `core:storage`, add a `schemaDirectory`, and **commit the
  generated schema JSON** — it is the only record of what shipped.
  *Verify:* `./gradlew :core:storage:testAndroidHostTest` with DAO tests that really open a
  database.  ·  *Ref:* plan §2, §4.7; `TaigaMobileNova/core/storage/build.gradle.kts`
  **If the bundled driver can't open a database in an AGP host test**, that is a finding, not a
  reason to delete the tests: say so, and move the DAO tests to instrumented (which is the fork
  2.7 parked, arriving earlier than expected). Don't reach for Robolectric — `CLAUDE.md` settled
  that one.
  Entities mirror the **domain** model, not the DTO: 2.1 deliberately kept the domain narrower
  than the wire, and a cache of fields no screen reads is a migration liability for nothing.
  *Note:* **the bundled driver can't open a database in an AGP host test, and the DAO suite is
  now instrumented** — the fork 2.7 parked, arriving here. Two independent blockers, either one
  fatal on its own: on the Android target `Room`'s only builders take a `Context`
  (`Room.inMemoryDatabaseBuilder<WallosDB>()` doesn't compile — "No value passed for parameter
  'context'"), and `BundledSQLiteDriver`'s `libsqliteJni.so` ships in the aar's `jni/`, which is
  not on a host test's classpath. Robolectric is the only thing that bridges either, and
  `CLAUDE.md` settled that. So `core:storage` gained a **device-test compilation** —
  `withDeviceTestBuilder { sourceSetTreeName = null }` in its own build file, not in
  `build-logic`, because it is the only module that needs one. **Verify is
  `./gradlew :core:storage:connectedAndroidDeviceTest`** with an emulator up; 9 tests, all
  passing, opening a real in-memory database through the real generated `_Impl`.
  `sourceSetTreeName = null` is load-bearing: the default (`"test"`) would put `androidDeviceTest`
  in the `test` source-set tree, and 1.4's `commonTest` DataStore suite would be compiled and run
  on the device as well.
  **`allTests` does not include it and CI has no emulator**, so this is the project's first test
  suite that isn't a CI gate. 3.5's Compose tests will land in the same position — decide there,
  not here, whether CI grows an emulator job.
  Entities are **SQLite primitives only, no `TypeConverter`**: `cycleCode: Int?` is the raw wire
  code and the two dates are ISO-8601 text. That is what keeps `core:storage` from depending on
  `feature:subscriptions:domain` for `BillingCycle` — TaigaMobileNova's `core:storage` *does*
  depend on a feature's `domain` module, and following it there would invert this repo's
  `feature/` → `core/` direction. The entity↔domain mapper is **3.4's**, in
  `feature:subscriptions:mapper` where the other mappers are.
  `currencySymbol` is stored **resolved** on the subscription row, so the cached list is one query
  with no join; `CurrencyEntity` is cached for the *next* refresh's resolution, which is 2.3's
  second round trip. `replaceAll` on both DAOs is a `@Transaction` delete-then-insert — a snapshot,
  never a merge, because a row missing from a fresh whole-list fetch has been deleted server-side.
  **Nothing injects the DAOs yet**, so the file-backed builder in `StorageModule` is checked only
  by `KoinGraphTest`'s `verify()` (which does pass with the two DAO interfaces as bound types);
  the instrumented tests build in-memory. 3.4 is the first thing to open the real file — *and it
  did, on the emulator, first try.*

- [x] **3.4 — feature:subscriptions data: offline-first repository**
  `SubscriptionsRepository` serves the cache first and refreshes behind it, instead of fetching on
  every screen open. This is where three deviation rows come due at once: 2.3's **two round trips
  per call** (the currency join re-reads `get_currencies.php` every time), 2.5's **detail re-read**,
  and 2.4's **"a failed load clears the list"** — with a cache there is now something real behind
  the error, so a failure must leave the last-known list standing.
  *Verify:* `./gradlew :feature:subscriptions:data:testAndroidHostTest`  ·  *Ref:* plan §7.1, §7.2
  Decide and write down **what invalidates the cache**. Disconnect must drop it — the rows belong
  to the account whose key was just cleared, and 1.4's `clear()` currently touches only the key.
  *Note:* **the answer to "what invalidates the cache" is `ApiKeyStorage.clear()`**, which now
  empties both tables before removing the key. It is the single place a key is dropped and it has
  *three* callers, not one: disconnect, and **both login paths**, which clear the stale key before
  validating a new one (1.9). A `CacheCleaner` wired into `feature:settings` would have covered
  the first and silently missed the other two — logging in as a second account would have shown
  the first account's rows. The price is that `ApiKeyStorageImpl` takes the two DAOs; that is
  inside `core:storage`, which owns both, and the invariant "no key ⇒ no cache" is then impossible
  to forget rather than merely documented. Nothing else invalidates: a schema change is handled by
  3.3's destructive fallback, and a whole-list refresh already drops rows the server no longer has.
  The repository split in two along the line the parameter limit drew: `SubscriptionsCache`
  (the two DAOs + the two entity mappers, speaking domain models only) and
  `SubscriptionsRepositoryImpl` (the API, the wire mappers and the order things happen in) — eight
  constructor parameters is over detekt's `allowedConstructorParameters: 6`, and the seam it forced
  is the right one.
  The interface is now **`observe*` + `refresh*`** rather than `get*`: reads come off the cache and
  never fail, the network only writes to it. `SubscriptionDao.getById` became **`observeById`** —
  3.3 wrote the one-shot read for this step, and a detail screen whose row a list refresh rewrites
  underneath it needs the `Flow`. That is the one 3.3 file this step touched; the device suite is
  10 tests now, still green.
  **Both ViewModels are cache-first**, so the spinner is only the *empty* cache's and a failed
  refresh keeps its rows — which closes 2.4's "a failed load clears the list" and 2.5's detail
  re-read (one round trip now, the symbol coming from the cached currency table). The **rendering**
  is untouched and still wrong for it: the error state draws *over* the list, and over the detail
  row it already has. Verified on the emulator — airplane mode, cold start, the whole cached list
  on screen with the error text over it, and the detail top bar carrying the cached name. Turning
  that overlay into a banner is 3.5, which is now the only thing between here and the first half
  of plan §8's "done when".

- [x] **3.5 — feature:subscriptions ui: stale and offline states**
  The visible half of 3.4: a list backed by cached rows says so rather than pretending to be
  fresh, and an offline refresh failure is a banner over real data instead of an empty screen with
  a Try again button. Detail screen likewise.
  *Verify:* `./gradlew :feature:subscriptions:ui:testAndroidHostTest`, and on the emulator: load
  online → airplane mode → force-stop → relaunch → the list is still there, marked stale.
  That relaunch is the **first half of plan §8's "done when"**, and the first check in the project
  that a *cold* start with no network shows real data. — **done, first try.**
  *Note:* the whole step is **two derived properties per UI state**, not a new field anywhere:
  `isStale` (`error` **and** data) and `isFailed` (`error` and **no** data) split what the same
  error means on screen, and the ViewModels didn't change at all — 3.4 had already stopped
  clearing the data. A boolean the ViewModel *sets* would have been a second copy of a fact both
  existing fields already carry, and could disagree with them.
  The banner reads **`LocalIsOffline` itself** rather than being handed the network state: an
  offline refresh fails as `error_unreachable` — "Check the URL and your connection" — which sends
  the user to look at a server that is not the problem, so the local overrides the reason line.
  That is the first real use of 3.2's local, and the first thing in the app whose text depends on
  it. Its `errorContainer`-vs-`surfaceVariant` question resolved to `surfaceVariant`: what is on
  screen is real data and the failure is only that it might be old.
  `StaleBanner` is a **`widgets/`** file, joining the four the plan lists — both screens show the
  identical banner, and the list wires its retry to `onRefresh` (so the pull-to-refresh indicator
  reports it) while the detail, which has no such indicator, wires `onRetryClick`.

- [x] **3.6 — feature:subscriptions ui: filter and sort**
  Filter by household member, category, payment method and active/inactive; sort by the fields
  `WALLOS_API.md` §3.2 lists. **All client-side, over the cached list** — see the milestone note.
  The option sets need no new endpoint and no Phase 3 catalog module: every row already carries
  its resolved `category_name` / `payer_user_name` / `payment_method_name` (2.1), so the filter
  sheet is built from the data already on screen.
  *Verify:* `./gradlew :feature:subscriptions:ui:testAndroidHostTest`  ·  *Ref:* `WALLOS_API.md` §3.2
  Sorting is worth a pure class in the module rather than a `sortedWith` in the ViewModel —
  §3.2's server-side directions are per-field (`price` and `id` descend, the rest ascend) and
  matching them is the kind of table a test should pin. — **done.**
  *Note:* the step's real cost was **not** the filtering — it was that `items` stopped meaning
  "what the cache holds". Every derived state 3.5 built on `items.isEmpty()` had to be re-asked of
  the cache (`hasCachedRows`), or a filter matching nothing reads as an empty instance, and offline
  it flips 3.5's stale banner into a full-screen error over rows that are right there. That is the
  one new *field* on the UI state; `isNoMatch` joins `isEmpty`/`isStale`/`isFailed` as a derived
  one. **Any later step that narrows what is drawn inherits this**: ask the cache, not the list.
  Filter and sort are `MutableStateFlow`s **beside** the state, `combine`d with the DAO flow, so a
  changed filter and an arriving refresh render through one path — and sorting again costs no
  refetch, which a test pins.
  Three of §3.2's sort fields are ids this app never receives (`payer_user_id`, `category_id`,
  `payment_method_id`), so those sort by the **resolved name** instead — the same names 2.1 put on
  the row and the same ones the filter chips are built from. `alphanumeric` is skipped as an alias
  for `name`. The default order is now §3.2's `next_payment`, where before it was the DAO's
  `ORDER BY id`.
  The top bar action is a **text button reading "Filter"**: `Icons.Default.FilterList`, which plan
  §5.4's sketch names, is not in `material-icons-core` — the third time that set has come up short.

- [x] **3.7 — core:storage + core:api: certificate trust**
  `TrustedCertStorage` and a composite trust manager that falls back to a per-host trust-on-first-use
  set, wired into the OkHttp engine. Port from `TaigaMobileNova` — `core/storage/.../cert/`,
  `core/domain/.../PendingCertTrust.kt`, and read its `docs/features/private-cert-trust/plan.md`
  first; it settled trust-on-first-use over the alternatives and the reasoning transfers whole.
  No UI in this step.
  *Verify:* `./gradlew :core:storage:testAndroidHostTest :core:api:testAndroidHostTest`  ·  *Ref:* plan §4.5
  This matters more here than in Taiga: nearly every Wallos instance is self-hosted, and pinning
  is not an option for a certificate the user minted themselves. — **done.**
  *Note:* the port dropped Taiga's **whole platform-exception apparatus** (an `androidMain`
  `CertificateException` subtype in `core:domain`, a portable twin, and an `expect`/`actual`
  mapper to unwrap one into the other). JSSE only lets a trust manager throw
  `CertificateException` — but nothing says the *payload* must be a platform type, so the trust
  manager throws `CertificateException(UntrustedCertificateException(pendingCertTrust))` and
  `Throwable.findPendingCertTrust()` (`core:domain`, `commonMain`) walks the cause chain for it.
  One class instead of three, no `androidMain` in `core:domain`, and it is testable in
  `commonTest`. **3.8 catches nothing** — it asks a failed `Result`'s throwable for a
  `PendingCertTrust`.
  Wiring the engine explicitly means `HttpClient(engine)` and no more autodiscovery, for the web
  client too — onboarding is where the prompt has to appear. It also puts `core:api`'s first
  `androidMain` on the map, and with it the module's first **`androidHostTest`**: `commonTest`
  cannot see an `androidMain` class, and detekt's test exclusions don't list that source set, so
  the test names are camelCase for the same reason 3.3's instrumented ones are.
  The Verify is unit tests, but the engine swap is under *every* request in the app, so it was
  also smoke-tested on the emulator against the local instance — a live `get_subscriptions.php`
  through the new engine, checked in the Ktor log rather than by looking at rows that could have
  come from the cache.

- [x] **3.8 — feature:setup ui: the trust prompt**
  The dialog and the retry: an untrusted certificate on connect surfaces the host and fingerprint,
  and accepting it stores the trust and retries the attempt that failed.
  *Verify:* on the emulator, against a **TLS front for the local instance** — plain
  `http://10.0.2.2:8282` cannot exercise this. `TaigaMobileNova/docs/features/private-cert-trust/server-setup.md`
  has the recipe; put a self-signed proxy in front of port 8282 rather than touching the Wallos
  container. This is the **second half of plan §8's "done when"**.
  *Note:* the recipe works verbatim — an `nginx:alpine` container on `wallos_default` proxying to
  `http://wallos:80`, leaf `CN=10.0.2.2` with `subjectAltName=IP:10.0.2.2`. Verified beyond the
  step: cancel persists nothing (the prompt returns), accept pins + retries + lands on the list,
  the pin survives `force-stop`, and a **rotated** leaf on the same host is rejected again — the
  fingerprint on screen matched `openssl x509 -fingerprint -sha256` every time. **What that last
  check exposed belongs to a later step**: a certificate that changes *after* connecting is a dead
  end on the list screen, which has no prompt — 3.5's stale banner says "couldn't reach that
  server" and the only way out is Disconnect and log in again. Filed under "Still open after v1".

- [x] **3.9 — feature:setup: TOTP second step**
  Drive `totp.php` instead of degrading to manual key entry: `LoginOutcome.NeedsTotp` becomes a
  code field, and `POST one-time-code` on the **same session** completes the login
  (`WALLOS_API.md` §9.2, §9.3).
  *Verify:* `./gradlew :feature:setup:data:testAndroidHostTest :feature:setup:ui:testAndroidHostTest`
  ·  *Ref:* `WALLOS_API.md` §9.2–9.3, plan §1.1
  **This step fights 1.9 head on, and that is the whole of its design work.** `WebLoginApiImpl`,
  `SetupRepositoryImpl` and the `@WebSessionHttpClient` are all `@Factory` *specifically* so the
  session cookie dies with the attempt — and TOTP needs that session to survive a human typing six
  digits. Resolve it deliberately (a scoped session held for one attempt with an explicit
  lifetime, say), don't quietly promote anything to `@Single`: `CLAUDE.md` warns that a `@Single`
  anywhere above a `@Factory` silently undoes it, and nothing fails loudly when it does.
  **On-device verification needs TOTP enabled on the user's real account** — that is a mutation of
  their live instance, so **ask first**, and offer the MockEngine tests as the alternative.
  *Note:* the `@Factory` fight resolved to **no change at all** — Koin resolves a `@Factory` once
  per *injection point*, and the only one in the chain is `LoginViewModel`'s constructor, so the
  cookie jar already spanned the screen rather than the call. What the step actually needed was to
  say so (*now in plan §1.1*). Asked, and verified on a **throwaway `bellamy/wallos` container**
  instead of the user's account — a scratch user with a known TOTP secret written straight into
  its SQLite, zero mutation of live data; recipe below. `totp.php` turned out to have a **third**
  answer the API doc didn't carry (*now in `WALLOS_API.md` §9.2*), and all three were driven on
  the emulator, expired session included.

- [x] **3.10 — feature:setup: password-login probe and backoff**
  Probe `login.php`'s form for `password_login_disabled` / OIDC and degrade to Path B before the
  user types a password that cannot work. Plus client-side backoff on repeated failures: plan §9
  notes the **server has no rate limiting or lockout of its own**, which makes an unthrottled
  retry loop a brute-force tool pointed at the user's own instance.
  *Verify:* `./gradlew :feature:setup:data:testAndroidHostTest`  ·  *Ref:* plan §9, §1.1
  1.9 left the probe unowned on purpose ("a degrade-to-Path-B affordance, not a blocker"). It is
  cheap here because 3.1 already touched this screen's copy.
  *Note:* the probe runs off the **URL field** (700 ms debounce), not off Connect — Connect isn't
  earlier than the password, which was the point. Reading `login.php` out of the container paid for
  itself twice (*now in `WALLOS_API.md` §9.1, §9.5*): a GET of it **clears
  `$_SESSION['totp_user_id']`**, so an unguarded probe would kill a live TOTP challenge, and
  `password_login_disabled` is only read when OIDC is *configured*, which is why "no password
  input" can be reported as SSO rather than as a generic refusal. Both branches were driven on the
  emulator against a throwaway `bellamy/wallos` — OIDC needs no IdP, just
  `OIDC_ENABLED=1 OIDC_DISABLE_PASSWORD_LOGIN=1` plus the seven fields `is_configured` checks
  (recipe in `CLAUDE.md`) — and the backoff's 1s/2s/4s were read off `LoginThrottle`'s own logcat
  line on device. Both halves are *now in plan §1.1*. **What the backoff does not do is say
  anything**: the wait is spent under the existing spinner, so a user on their fifth attempt sees
  a slow login and no explanation. Filed under "Still open after v1".

- [x] **3.11 — currency conversion hint**
  Send `convert_currency` and handle the silent failure: `WALLOS_API.md` §3.2 says conversion only
  happens once exchange rates have been fetched at least one time, and otherwise prices come back
  **unconverted with nothing in `notes` to say so**. Show that state rather than displaying a
  converted-looking total that isn't.
  *Verify:* `./gradlew :feature:subscriptions:data:testAndroidHostTest`  ·  *Ref:* `WALLOS_API.md` §3.2
  Check the live instance for `last_exchange_update` before designing the UI — if the user's own
  rates have never been fetched, this state is the *default* one and not an edge case.
  *Note:* that check said yes — empty table, all 32 rates at `1`, every row already in the main
  currency, so the state is the default three times over. But reading the PHP found something bigger
  that **inverts the step's premise** (*now in `WALLOS_API.md` §5.5, plan §7.1*): a conversion that
  *succeeds* rewrites `price` and leaves `currency_id` naming the source currency, so sending the
  flag naively would have drawn `$29.35` on an amount in euros — worse than not converting at all.
  §5.5's own "compare `currency_id` against `main_currency`" detection **cannot work**, and is
  corrected there. Measured, not inferred: `31.99 → 29.348623853211006` with `currency_id` still
  `2` and `notes` empty. Asked, and chose to **honour the instance's own `convert_currency` setting**
  (`get_settings.php`) rather than override it — the user's is off, so their app is unchanged and the
  banner carries the step. All four branches driven on the emulator against a throwaway copy of their
  database: converted rows in `€`, rates removed → `$` plus the banner, setting off → neither, and a
  cold offline start showing the stale and conversion banners stacked. **Two things this does not
  do** are filed under "Still open after v1".

- [x] **3.12 — Phase 2b acceptance**
  *Verify:* plan §8's two conditions, end to end on the emulator — fetch the list online, go
  offline, cold-start, and see real data marked stale; then connect to a self-signed instance from
  a fresh install. Plus a filter and a sort that survive a rotation, and the `am kill` cycle from
  `CLAUDE.md` once, from a clean task.
  Same shape as 2.7: this is a verification step, so anything it uncovers that is a *behaviour*
  change goes to "Still open after v1" rather than being fixed here.
  Reconsider two parked things with M3's evidence rather than on a schedule: the **Kover floor**
  (3.3 and 3.4 add the first logic in the project with real branch depth) and **instrumented
  Compose tests** (3.5's stale/offline/empty/error matrix is the screen 2.7 predicted would
  outgrow a ViewModel test — if 3.3 already forced instrumentation for the DAOs, the setup cost is
  paid).
  *Note:* **M3 closes — both of plan §8's conditions passed**, each proven against the thing that
  could have faked it. The offline half ran from a `force-stop` cold start with airplane mode on and
  **every request logged as `ConnectException`**, so the 35 rows under the offline-worded stale
  banner were provably Room's rather than a refresh that quietly succeeded. The self-signed half ran
  from a **fresh install** against a rebuilt nginx front, with the prompt's fingerprint compared to
  `openssl x509 -fingerprint -sha256` before accepting; accept retried and landed on the list. Filter
  + sort survived a rotation, and the `am kill` cycle from a clean task (`sz=1`, process confirmed
  gone) restored the detail screen it was on, with Back landing on the list. No `FATAL` anywhere in
  the run.
  **One real defect found, and it sits in 3.7/3.8's blind spot: logos never load on a self-signed
  instance.** `AsyncImage` is Coil's own network stack and no `ImageLoader` is configured anywhere in
  the repo, so the trust the app pins belongs to the *HTTP client* and Coil never sees it — over the
  TLS front every row draws a blank gap, because `SubscriptionLogo`'s initial-letter placeholder only
  fires on an **empty** filename and not on a failed load. Evidenced, not inferred: `curl -k` serves
  the same logo `200 image/png` and the same build renders logos over plain HTTP. Filed under "Still
  open after v1".
  **Both parked decisions are now answered with numbers, and both answers are "not an aggregate
  gate".** Kover reads **48.8% line** overall, but **388 of the 2012 measured lines are Room's
  generated `*_Impl` classes at 0%** — uncoverable by any host test, since Kover never sees the
  instrumented DAO suite — while every hand-written class in that package is at 100%. Composables are
  the other 0%: `ui/list/widgets` (159 lines), `ui/widgets` (84), `uikit` (98). The logic layers are
  **82–100% with no floor asking them to be**. So an aggregate floor would be a number about
  generated code and unrendered Compose, and the only useful floor is a scoped one — a `kover { }`
  edit, i.e. a `Gate-change:`, not a verification step's work. The same numbers date the Compose
  fork: M3's entire rendering surface (stale banner, filter sheet, conversion banner, trust dialog)
  is at 0% and is verified only by runs like this one. Both stay parked, now with a shape rather than
  a date.

## M4 — Appearance, and the fixes v1 deferred (plan §8, Phase 2c)

Goal: the app looks right in **both** light and dark on every screen, the user can pick which, and
the certificate they accepted covers the logos too. **Done when** a dark-mode device shows no
light-mode screen anywhere, the choice in Settings survives a restart, and a self-signed instance
renders its logos.

This milestone is **not in plan §8's phase order** — it jumped ahead of Phase 3 because 3.12 and a
look at the running app turned up defects in shipped screens, and Phase 3's write surface would only
be built on top of them. It is small and mostly `ui`; nothing here sends data.

Three things that constrain the steps below:

- **The login screen is the only screen with no `Scaffold`**, which is why it was the only one that
  looked unthemed. Everything downstream of `AuthenticatedMainScreen` gets a `Scaffold`, and a
  `Scaffold` paints `colorScheme.background`. Since 4.1 `WallosMobileTheme` paints its own `Surface`,
  so this no longer bites — but it is still the screen to check first for anything colour-related.
- **Previews became trustworthy for colour in 4.1.** Before it, `WallosMobilePreviewTheme` wrapped
  content in a `Surface` that `WallosMobileTheme` did not, so `@PreviewWallosDarkLight` rendered a
  themed background the app never drew. The `Surface` now lives in `WallosMobileTheme` and the
  preview theme adds only composition locals, so the two render identically.
- **The theme is device state, not account state.** `ApiKeyStorage.clear()` evicts the Room cache
  because that data belongs to the key (3.4) — the theme is the counter-example and must **survive**
  Disconnect.

- [x] **4.1 — uikit + androidApp: one themed surface, and the colour roles the palette skips**
  The reported bug, and it is two stacked causes rather than a missing theme. (a) `WallosMobileTheme`
  paints nothing, so on the login screen — the one screen with no `Scaffold` — the visible background
  is the **window's**, and the manifest hardcodes `@android:style/Theme.Material.Light.NoActionBar`:
  white in dark mode. (b) With no `Surface` there is no `LocalContentColor`, so Compose's default
  **black** wins for text that doesn't name a colour while `OutlinedTextField`'s placeholder reads
  the dark scheme's pale `onSurfaceVariant` — pale grey on white, which is the "grey text" as
  reported. Wrap `WallosMobileTheme`'s content in a `Surface`, give `androidApp` a real
  `res/values/themes.xml` + `values-night/` so the window background follows night mode, and fill in
  the M3 roles `lightColorScheme`/`darkColorScheme` leave unset — `Card` takes
  `surfaceContainerLow`, which is **not** in the palette, so every card in the app is currently
  drawing Material's baseline lavender rather than a Wallos colour.
  *Verify:* on the emulator, `adb shell cmd uimode night yes` / `no` across **login, list, detail,
  drawer, filter sheet and the trust dialog** — screenshots in both modes, since this is the one step
  whose whole content is what the screen looks like.  ·  *Ref:* `uikit/.../Theme.kt`, `Color.kt`;
  `MealieMobile/app/src/main/res/values/themes.xml`
  **minSdk is 24, which rules out the obvious parents.** `Theme.DeviceDefault.DayNight` is API 29,
  and `Theme.Material3.DayNight.NoActionBar` needs `com.google.android.material:material` — the
  alias exists in `libs.versions.toml` already, so adding it costs no `Gate-change:`, but a
  `values/` + `values-night/` pair with a `windowBackground` colour needs no dependency at all and
  is the smaller change for a Compose-only app. Decide in the step and record which.
  After this step, previews are trustworthy for background and content colour for the first time —
  say so in the note, because every later step's preview inherits it.
  **Note:** the `values/` + `values-night/` pair won — no `com.google.android.material` dependency,
  so no `Gate-change:` line. The `Surface` moved *into* `WallosMobileTheme`
  (`Modifier.fillMaxSize()`, which is a no-op under a preview's unbounded constraints) and came
  *out* of `WallosMobilePreviewTheme`, so a preview and the app now render on the same background
  with the same `LocalContentColor` — **previews are trustworthy for colour from here on**.
  Verified on the emulator across login / list / detail / drawer / filter sheet / trust dialog in
  both modes, over the nginx TLS front so the dialog was real.

- [x] **4.2 — core:storage: `ThemeMode` + `ThemeStorage`, honoured above the shell**
  A three-value preference (`System` / `Light` / `Dark`) in the existing DataStore, collected by
  `WallosAppContent` and passed to `WallosMobileTheme(darkTheme = …)`. **No new ViewModel** — inject
  `ThemeStorage` the way `apiKeyStorage` already is; MealieMobile routes this through a
  `MainViewModel` it has and this project doesn't, and a whole ViewModel for one flow is the
  abstraction `CLAUDE.md` says not to add.
  *Verify:* `./gradlew :core:storage:testAndroidHostTest`, then on device: set Dark, `force-stop`,
  relaunch, still dark — **and the `am kill` back-stack cycle still restores**, which is the real
  check here.  ·  *Ref:* `MealieMobile/core/storage/.../theme/`, plan §4.7, §5.5
  **This is a second DataStore flow above the shell, which is exactly 1.11's trap.** It must never
  gate rendering: collect with `initial = ThemeMode.default()` and let the stored value arrive late.
  A `when (themeMode)` that renders a placeholder while it loads would push `NavDisplay` into a
  later composition and silently drop the restored back stack — the failure has no error and only
  shows up under `am kill`. Also make sure Disconnect does **not** clear it (see the milestone note),
  and test that.
  **Note:** `ThemeStorage.themeMode` (not Mealie's `themeModeFlow` — the type says it is a `Flow`),
  `distinctUntilChanged` because every write to the shared DataStore file re-emits the whole
  `Preferences`. Verified on the emulator all three ways: stored Dark on a light device, stored
  Light on a night device, and no stored value tracking `cmp uimode night` both directions; Dark
  survived `force-stop`, and the `am kill` cycle restored the detail screen, so the second flow
  above the shell did not cost the back stack.
  **There is no UI to set the mode until 4.3, so the device check plants the preference by hand.**
  DataStore Preferences is a plain protobuf `map<string, Value>` with no checksum, and protobuf
  merges repeated fields — so *appending* an encoded entry to
  `files/datastore/wallos_storage.preferences_pb` through `run-as` sets a key without disturbing
  the stored URL, key or pins, appending it twice makes the last one win, and `truncate -s` back to
  the original size undoes the lot. `adb shell "run-as … sh -c '…'"` needs the **outer** double
  quotes and an absolute path: `adb` flattens its arguments, so single quotes are stripped and a
  `$VAR` would be expanded by the device's own shell.
  **Found, and it belongs to 4.3:** `MainActivity` calls bare `enableEdgeToEdge()`, whose
  `SystemBarStyle.auto` picks the status-bar *icon* tint from the resource configuration, not from
  the Compose theme. So the moment the two diverge — which is exactly what this step makes possible
  — the icons are dark-on-dark (or light-on-light) and all but invisible. Nothing user-reachable
  can produce the divergence until the Interface screen ships, so it is left for that step.

- [x] **4.3 — feature:settings ui: the Interface screen**
  A settings sub-screen with a radio group over the three modes, reached from a row on the settings
  root. The ViewModel takes `ThemeStorage` directly — `feature:settings` is `ui`-only by design
  (2.6), and this is the same one-seam case as Disconnect.
  *Verify:* `./gradlew :feature:settings:ui:testAndroidHostTest`, then on device switch all three and
  watch it apply live; plus **`am kill` while on the Interface screen**, to prove the new route was
  registered.  ·  *Ref:* `MealieMobile/feature/settings/ui/.../appearance/`, plan §5.3
  **Mealie's screen is the wrong shape for this repo** and copying it will fail review: it passes
  `selectedTheme` + `onThemeSelect` as parameters, where `CLAUDE.md` wants a state class carrying its
  own callbacks with no-op defaults. Take its layout, not its signature.
  **The new route must go in `NavKeySerializers`' polymorphic module**, and `NavKeySerializersTest`
  **cannot catch a missing one** — it walks `DrawerDestination.entries` and this route is not a
  drawer destination. The `am kill` cycle is the only check that exists.
  **This step also owns the status bar** (found in 4.2): `MainActivity`'s bare `enableEdgeToEdge()`
  tints the system-bar icons from the resource configuration rather than from the Compose theme, so
  picking Light on a night-mode device — the very thing this screen adds — leaves them invisible.
  It needs `SystemBarStyle` driven by the same `darkTheme` boolean `WallosAppContent` computes,
  which is `androidApp`'s side of a value that currently never leaves `composeApp`.
  **Note:** the boolean leaves `composeApp` as a callback — `WallosAppContent(onDarkThemeChange =
  ::applyEdgeToEdge)`, fired from a `LaunchedEffect(darkTheme)` — rather than as a value the
  activity computes for itself, which would have meant a second `ThemeStorage` collection above the
  shell and 1.11's trap twice over. `enableEdgeToEdge` is re-callable by design, and the scrims it
  takes are androidx's own `DefaultLightScrim`/`DefaultDarkScrim` copied out (they are `internal`),
  so only the dark-mode *detection* changes.
  `SettingsScreen` grew an `onInterfaceClick` plain parameter and `settingsEntry` now takes the
  `Navigator` it didn't need before. Verified on the emulator: all three modes apply live, the
  status-bar icons are white on Dark over a light device and black on Light over a night device
  (the 2× crop of the top 90 rows is what shows it), and the `am kill` cycle came back **on the
  Interface screen** with Settings still under it. Both checks needed the device's night mode and
  the stored mode *diverged* — matching them proves nothing, since three different mechanisms
  produce the same screenshot (now in `CLAUDE.md`).
  **`InterfaceRoute` is deliberately not in `RouteConfigProvider`**, so the drawer stays
  swipe-openable on it. `SubscriptionDetailRoute` disables gestures because a horizontal swipe is
  the *screen's* there; a radio list has no horizontal gesture to protect. 4.4's About screen is
  the same case — leave it out too unless it grows one.

- [x] **4.4 — feature:settings ui: the About screen**
  Version and build info plus a link out to the project. `AppInfoProvider` (`core:appinfo-api`)
  currently exposes `isDebug()` alone, so it gains whatever the screen shows — the impl in
  `androidApp` is the only place `BuildConfig` exists (1.3's deviation is why the interface and its
  impl live apart). Link handling is `LocalUriHandler`, not an intent.
  *Verify:* `./gradlew :feature:settings:ui:testAndroidHostTest` with a fake `AppInfoProvider`, and
  on device: open About, check the version matches the installed build, tap the link.
  ·  *Ref:* `MealieMobile/feature/settings/ui/.../about/`
  Same route-registration rule as 4.3. Nothing here is user data, so there is nothing to redact and
  no reason for this screen to talk to the network.
  **Note:** `AppInfoProvider` gained `versionName()` and `versionCode()` — the *raw* fields, not
  Mealie's rendered `getAppInfo(): String`. The formatting is `about_version_value` (`%1$s (%2$d)`)
  and the Debug/Release word is a resource pair, neither of which an `androidApp` impl can reach;
  a rendered string would also have put presentation in the one class no host test can construct.
  `AboutViewModel` reads all three in its constructor and never touches `viewModelScope`, so its
  test is the first ViewModel test here with **no `MainDispatcherRule`**.
  **`SettingsScreen`'s signature had to be reordered**: a second callback without a default makes
  `compose:parameter-order` fail, because it exempts only a *single* trailing function from
  following the defaulted params. `viewModel` moved last, which is what the subscriptions screens
  already do — 4.3's `viewModel`-first shape passed on the one-callback exemption alone.
  Verified on the emulator: About shows `0.1.0 (1)` against `dumpsys package`'s `versionName=0.1.0
  versionCode=1`, the button fires `capturedLink=https://github.com/Grigoriym/Wallosmobile` into
  Chrome, the `am kill` cycle came back **on the About screen**, and the screen was read in both
  modes with the stored mode and the device's night mode diverged.

- [x] **4.5 — composeApp: an `ImageLoader` that trusts what the user trusted**
  3.12's defect. Coil builds its own client through a `FetcherServiceLoaderTarget`, so it never sees
  the trust manager and a self-signed instance loads its data and none of its logos. Register a
  singleton `ImageLoader` whose network layer is `KtorNetworkFetcher.factory(client)` over a client
  built on `createPlatformHttpClientEngine(trustedCertStorage)`.
  *Verify:* on the emulator **against the nginx TLS front** (`CLAUDE.md`'s recipe, as in 3.8 and
  3.12): accept the certificate, then see logos. Plain HTTP passes today and proves nothing.
  ·  *Ref:* plan §4.5, `core/api/.../NetworkModule.kt`
  **Give it its own minimal client, not the `@Single HttpClient`.** That one carries
  `Logging(LogLevel.ALL)` in debug — every logo would dump its PNG bytes into logcat, right after
  3.12 added a `CLAUDE.md` note about the Ktor log being unreadable — plus a retry predicate written
  for API paths. The engine is the only part that carries the pin. The API confirmed against the
  artifact on disk: `KtorNetworkFetcher.factory(HttpClient)` in `coil-network-ktor3`, and
  `coil3.network.ktor3.internal.KtorNetworkFetcherServiceLoaderTarget` is the autodiscovery this
  replaces. No need to read Coil's sources.
  **Second half, and it is what makes the failure visible**: `SubscriptionLogo` branches on an
  *empty* filename, so a load that fails draws a blank gap while a perfectly good initial-letter
  placeholder sits unused. Give `AsyncImage` its `error` slot so a broken load falls back to it.
  **Note:** the Kotlin name is `KtorNetworkFetcherFactory(client)` — `KtorNetworkFetcher.factory` is
  the `@JvmName`, and the single-argument overload the step quotes is `DeprecationLevel.HIDDEN`, so
  Kotlin binds the one that also takes a `ConcurrentRequestStrategy` default. `AsyncImage`'s `error`
  slot is a **`Painter`**, and the fallback here is an initial to lay out, so this is
  `SubcomposeAsyncImage` with a composable `error` slot and the placeholder extracted into a private
  `LogoPlaceholder`.
  **The definition had to move into `AppModule` itself, and `core:storage` had to become `api`.**
  A `@Factory class ImageLoaderProvider` in `composeApp` failed to compile with `[KOIN-D001] Missing
  dependency: TrustedCertStorage` — the compiler plugin re-checks the definitions of every
  `@Configuration` class the `startKoin` compilation can read, and `:androidApp` can read `AppModule`
  (a direct dependency) but not the `includes`, which arrive through `implementation`. So the check
  needs both the parameter *type* and the definition binding it on that classpath. Making it a
  module function did not help — the fix is `api(projects.core.storage)` in `composeApp`.
  That is why `NetworkModule.provideHttpClient(trustedCertStorage)` has never tripped this: it is
  invisible from `:androidApp`, so it is never re-checked there.
  `feature:subscriptions:ui` gave up `coil-network-ktor3`; it held it only for the autodiscovery
  this replaces. Verified on the emulator over the nginx TLS front: after *Trust and connect*, all
  35 logos render, and the front's access log shows the `GET /images/uploads/logos/*` requests
  arriving as `ktor-client` with `200`s. With the front stopped and `cache/coil3_disk_cache` deleted,
  the cached rows render initial letters instead of blank gaps.

## M5 — the defects the verification steps filed (not in plan §8's phase order)

Goal: close the list "Still open after v1" accumulated, so what remains there is policy and
deferred *features* rather than known-wrong behaviour. **Done when** a rotated certificate, a
process death and a multi-currency instance each produce something honest on screen.

Every step here is a **defect that was seen, not inferred** — each one names the step that filed it,
and the entry it closes comes out of "Still open after v1" when it is ticked. That is the same
filing loop 3.12 and 4.5 used, run in the other direction.

Three things that shape these steps:

- **The live instance can't show two of them.** All 35 rows are `currency_id = 1` with conversion
  off (`CLAUDE.md`), so nothing about currency comparison is visible there at all — 5.3 and 5.4 need
  the scratch container 3.11 describes, and each must say which instance its screenshot came from.
- **The TLS front is the rig for 5.1 and 5.6**, both of which are about what happens *after* a
  connection was working. Regenerating the leaf and restarting the container — optional in Taiga's
  recipe, and the check that found 3.8's real gap — is 5.1's whole premise.
- **Three of them change a surface an earlier step drew** (5.2 and 5.3 change 3.6's, 5.5 changes
  3.10's).
  That is why they are steps rather than notes: the earlier step's title didn't cover them, which
  is exactly what filing them recorded.

- [x] **5.1 — utils:ui: a certificate that rotates after onboarding says so**
  3.8's gap, and the one with a real trap in it. `getErrorMessage` is exhaustive on `WallosError`
  and sends **everything else** to `error_unreachable` — "check the URL and your connection" — so an
  instance whose certificate changed reads as a dead server, and the one route out (Disconnect, log
  in again, accept the new certificate) is the one the copy argues against. `Throwable
  .findPendingCertTrust()` (`core:domain`, 3.7) already answers "is it that?"; it is simply not
  consulted outside the login screen. Add the branch in the non-`WallosError` arm, ahead of
  `error_unreachable`, plus a string that names the certificate and the route.
  *Verify:* a case in `GetErrorMessageTest`; on device over the nginx TLS front — connect, then
  **regenerate the leaf and restart the container**, pull to refresh, read the banner.
  ·  *Ref:* `utils/ui/.../NativeText.kt`, `core/domain/.../findPendingCertTrust`, plan §4.5
  **Do not add a second trust prompt.** `feature:subscriptions` has no trust surface, and writing a
  pin from it would reach past `SetupRepository` — 3.1's rule, restated in plan §4.5. Naming the
  cause is this step; a prompt anywhere a refresh can fail is a bigger change and needs its own.
  *Note:* the branch is invisible on the login screen and that is correct — `LoginViewModel.onFailure`
  asks `findPendingCertTrust()` first and shows the prompt, so `getErrorMessage` is only reached by
  screens with no trust surface. Verified against the live instance behind the nginx front
  (`https://10.0.2.2:8443`), leaf regenerated from the same CA: the pin is per-certificate, so a
  re-issued leaf fails exactly like a new host's would.

- [x] **5.2 — feature:subscriptions:ui: the filter and sort survive process death**
  3.12 filed the inconsistency: the nav back stack is carefully serialized and the two
  `MutableStateFlow`s beside the UI state are not, so `am kill` restores the detail screen the user
  was on and the list behind it has forgotten its filter. Keep 3.6's shape — the criteria stay
  `combine`d with the DAO flow, one render path — and give them somewhere that survives a process.
  *Verify:* the `am kill` cycle from `CLAUDE.md`, **from a clean task**: filter and sort, background,
  kill, relaunch, the list comes back filtered. Plus a ViewModel test.
  ·  *Ref:* 3.6's `SubscriptionsViewModel`, `CLAUDE.md` "Don't keep activities is not a
  process-death test"
  **Check which vehicle is actually available before writing it.** `SavedStateHandle` is the obvious
  answer and this repo has never injected one — Koin's `@KoinViewModel` has to be able to supply it
  through `koinViewModel()`'s `CreationExtras`, and that is a five-minute check, not an assumption.
  `rememberSaveable` at the screen with the criteria hoisted is the fallback, and it is not a worse
  answer if the first one needs plumbing this app doesn't have.
  *Note:* `SavedStateHandle` works with no plumbing at all, and the check was worth running — it
  needs `@InjectedParam`, because Koin builds it from the `CreationExtras` rather than from the
  graph. What it can *hold* was the real constraint: a value has to be Bundle-safe on Android, and
  `androidx.savedstate`'s own `encodeToSavedState` is unreachable from a host test for exactly that
  reason (`SavedState` **is** `Bundle`). So the criteria go in as one JSON string under one key,
  which a host test reads back with no Android runtime. Verified on device across a real process
  death — pid 4155 → gone → 4304, task `sz=1` — with the contrast that `force-stop` correctly
  discards them.

- [x] **5.3 — feature:subscriptions:ui: the price sort stops comparing across currencies**
  3.6's, unchanged by 3.11. Whenever conversion is off or unavailable, `SubscriptionSort.PRICE`
  orders raw doubles, so €5 sorts below $10 as though they were the same unit — 3.11's banner tells
  the user their prices don't compare while the sheet beside it still offers the comparison.
  Disable or annotate the Price option when the drawn rows span more than one denomination. No new
  data is needed: each row already carries the `currencySymbol` it is actually denominated in
  (3.11's `symbolFor`), so "spans more than one" is derivable where the rows are.
  *Verify:* a test on the derived flag, and the filter sheet's preview in both states. **The live
  instance cannot show this** — build 3.11's scratch container, and say in the note which instance
  the screenshot came from.  ·  *Ref:* 3.6's `SubscriptionSorter`, 3.11's conversion banner
  *Note:* the step's "each row already carries the `currencySymbol` it is denominated in, so it is
  derivable where the rows are" is **half right, and the wrong half is load-bearing** — neither field
  on the row answers it alone. A symbol is not a currency (four dollars, three kroner share a sign),
  and a *working* conversion puts every row in one unit while `currency_id` still names the source it
  was converted from, so counting ids would refuse the sort exactly where it is honest. The flag
  therefore reproduces the repository's `symbolFor` decision — `!conversion.isActive &&` distinct
  `currencyId` — which makes 3.11's banner condition a strict subset of it and left the two sharing
  one computation. Disabled rather than annotated **and** annotated: the chip goes, and the note under
  it says why, because a sort chosen while the rows were comparable stays selected when a widened
  filter brings a second currency back — verified in that exact state, where the chip is selected,
  greyed and inert and the note still reads true. Screenshots came from a **throwaway
  `bellamy/wallos` on :8284**: a `cp -a` of the live database with 8 of the 35 rows moved to
  `currency_id = 2` and the copied API key rotated. It is **stopped, not removed**, since 5.4 needs
  the same rig (plus rates, which this copy still has at `1`).

- [x] **5.4 — feature:subscriptions:ui: a converted price says what it was converted from**
  3.11's. The amount is right and the symbol is right — `symbolFor` denominates a converted row in
  the main currency — but nothing on either screen says a conversion happened, which Wallos' own web
  UI offers as `show_original_price`. **The original amount is unrecoverable**: the server overwrites
  `price` and leaves `currency_id` naming the source (plan §4.5's third API surprise), so this is a
  *label* naming the source currency, not a second number. Detail screen only; the list row has no
  space and the banner already covers the instance-wide case.
  *Verify:* a test on whatever derives it, plus a preview. Same scratch container as 5.3, same
  requirement to say which instance.  ·  *Ref:* `SubscriptionsRepositoryImpl.symbolFor`,
  `PriceConversionEntity`
  *Note:* the row carries a `currency_id` and nothing that names it, so the label needed the
  **currency table** — a new `observeCurrencies()` on the repository, and a `CurrencyDao.observeAll()`
  under it. It is the third flow the detail screen combines and the derivation stayed in the
  ViewModel, reproducing `converts(currencyId)` exactly as 5.3's `spansCurrencies` reproduces
  `symbolFor`: the same question, asked where the rows are. The **code** (`USD`) rather than the
  symbol, since a symbol is not a currency, and blank rather than a guess when the cached table
  no longer holds the id. **5.3's rig could not show this at all** — its rates were still `1`, so
  nothing converted. The scratch instance on `:8284` now has `rate = 1.17` on USD,
  `convert_currency = 1` and a `last_exchange_update` row (the flag the server really gates on),
  which is what makes the 8 USD rows come back divided; that state is left in place and written up
  in `CLAUDE.md`. Screenshots: `Mullvad VPN` at `€4.27 / Converted from USD` in both modes, and
  `icloud` — already in the main currency — with nothing under its price.

- [x] **5.5 — feature:setup:ui: the login backoff is visible**
  3.10's. Past three refused attempts the next one waits 1–8 seconds under the spinner that was
  already there, so a login that got slow says nothing about why. Announce the wait before it
  starts.
  *Verify:* a ViewModel test, plus four refused logins in a row on device.
  ·  *Ref:* 3.10's `LoginThrottle`, `SetupRepositoryImpl`
  **Two things make this bigger than it looks.** `LoginThrottle` is a `private val` constructed by
  `SetupRepositoryImpl` — DI was buying it nothing (plan §4.1) — so surfacing the wait means the
  repository telling a caller something, which changes 3.10's surface. And the state exists only
  *while* a call is in flight, so `MainDispatcherRule`'s unconfined dispatcher will run straight
  past it: the test needs 3.4's `CompletableDeferred` trick to see it at all.
  *Note:* the shape the repository tells a caller with is an **`onThrottleWait: (Duration) -> Unit`
  parameter on the two throttled methods**, not a flow beside them: the wait exists only for the
  duration of one call, so a `Flow` on the repository would have needed a lifetime, an initial value
  and a clear — three decisions a parameter doesn't have. It defaults to `{}`, which is what keeps
  the twelve existing `SetupRepositoryImplTest` call sites and the whole non-throttled surface
  untouched. The UI state carries **seconds as an `Int`**, per the `<plurals>` rule, with
  `isThrottled` derived from it rather than stored beside it; it is cleared in one place — after the
  `when` in `onConnectClick`, since the notice belongs to the *call* and not to any of the five
  outcomes. Verified against the live instance on `:8282`: eight refused attempts, the singular form
  ("held back for a second") on the fourth and "held back for 8 seconds" from the seventh, the
  notice gone the moment the attempt landed, and the ninth — with the right password — waiting its
  8 seconds and then connecting. `cmd uimode night no` mid-wait confirmed the light mode and, as a
  bonus, that the counter survives an activity recreation: the ViewModel and its `@Factory`
  repository do.

- [x] **5.6 — feature:subscriptions:ui: a recovered server reloads its logos**
  4.5's leftover, and the smallest thing here. Coil does not re-issue a request whose state is
  already `Error`, so when the server comes back the stale banner's *Try again* refreshes the data
  and leaves every visible placeholder alone until its row happens to recompose. Key the request on
  something that changes when a refresh succeeds.
  *Verify:* on device over the TLS front — stop the front, delete `cache/coil3_disk_cache`, relaunch
  to initial letters, start the front, tap *Try again*, and see logos **without scrolling**.
  ·  *Ref:* 4.5's `SubscriptionLogo`, `CLAUDE.md`'s note on Coil's disk cache
  *Note:* the equality delegate that decides whether `AsyncImagePainter`'s `Input` changed compares
  an `ImageRequest`'s `data`, its cache keys, size, scale and precision (`coil3.compose`'s
  `AsyncImageModelEqualityDelegate.Default`, read from the `coil-compose-core` sources jar) — none
  of which differ between one `logoUrl` string and the next recompose of the same one, which is the
  exact mechanism of the defect. The fix is a `logoRefreshToken: Int` (bumped only by
  `onRefreshed()`, i.e. only a *successful* `refreshSubscriptions()`/`refreshSubscription()`, never
  a failed one) threaded through `SubscriptionUiItem`/`SubscriptionDetailUiItem` and read into
  `SubscriptionLogo`, which builds an explicit `ImageRequest` and sets it as a
  `memoryCacheKeyExtra` — changing the request's identity without touching `logoUrl`, the disk
  cache key, or the memory cache key it decorates. A row that already loaded successfully is a
  cheap memory-cache-miss-then-disk-cache-hit; a row stuck in `Error` gets a real network retry.
  Verified against the live instance behind the nginx TLS front (`https://10.0.2.2:8443`): stopped
  the front, cleared the disk cache, relaunched to eight initials and a stale banner, restarted the
  front, tapped *Try again* — every row's logo came back without a scroll, and the front's own
  access log shows all eight `images/uploads/logos/*` requests re-issued with the `ktor-client`
  user-agent in the same second, confirming a real re-fetch rather than a stale render.


## M6 — packaging and identity (not in plan §8's phase order)

Goal: the app stops looking and installing like a scaffold. **Done when** the launcher shows the
user's logo, and a debug build can sit on the same device as a release one without either of them
being ambiguous about which is which.

Neither step is a defect and neither blocks the other — they are here because they are the two
things that are *only* obvious from outside the code, and both get forgotten the moment a feature
step is available to do instead. Take them in either order.

Both are cheap in code and expensive in **documentation**: `CLAUDE.md`'s emulator recipes hardcode
`com.grappim.wallosmobile` and `installDebug`, and 6.2 invalidates every one of them.

- [x] **6.1 — androidApp: the launcher icon is the app's own**
  The icon is still Android Studio's scaffold — `drawable/ic_launcher_background.xml` plus
  `drawable-v24/ic_launcher_foreground.xml` (the green droid), with legacy PNG pairs in all five
  `mipmap-*dpi/`. **The user supplies the artwork; don't draw or generate one.** Replace the
  adaptive layers, regenerate the legacy PNGs — `minSdk = 24`, so the `mipmap-*dpi` bitmaps are
  still the icon on API 24–25 and cannot simply be deleted — and add a `<monochrome>` layer to
  `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`, which the current scaffold has
  no entry for and which is what Android 13+ themed icons read.
  Scope is the **launcher** icon. The drawer header draws `stringResource(RString.app_name)` as
  text (`WallosDrawerWidget`) and the login screen has no mark at all; putting the logo in either is
  a separate ask, so do it only if the user says so when handing over the asset.
  *Verify:* install on the emulator, `adb shell input keyevent KEYCODE_HOME`, and screencap the
  launcher — plus one `adb shell cmd uimode night yes` capture, since a logo with a light background
  is the one that disappears against a dark launcher. Say in the note which shapes were checked
  (adaptive mask, round, themed).  ·  *Ref:* `androidApp/src/main/res/`, `AndroidManifest.xml`'s
  `android:icon` / `android:roundIcon`
  Note: the user supplied `art/wallosmobile_logo.png` (a navy circular "sticker" mark, full bleed
  on transparent) and ran Android Studio's Image Asset Studio to generate the adaptive layers —
  it already wired `<monochrome android:drawable="@mipmap/ic_launcher_foreground"/>` into both
  `ic_launcher.xml` and `ic_launcher_round.xml` unprompted, reusing the foreground (fine: themed
  icons key off alpha, not colour). What the wizard left alone was
  `drawable/ic_launcher_background.xml` — still the scaffold's opaque green grid. Since the
  foreground art already bakes in its own navy disc on a transparent canvas (unlike Taiga's/
  Mealie's bare-glyph foregrounds, which need a background fill for contrast), the leftover
  scaffold background showed through as a black-ish ring in the adaptive mask's safe-zone margin —
  confirmed on device before and after. Replaced it with a single fully-transparent path (matching
  Taiga/Mealie's simplified one-`<path>` vector shape, just transparent instead of their opaque
  tint), which removed the ring. **A wizard reimport reverts this file** — it happened mid-step —
  so re-check it after any future regeneration. Verified adaptive mask shape in both light and
  dark launcher (drawer, `cmd uimode night yes/no`); round icon file was regenerated but not
  separately forced on-screen (Pixel launcher always masks to its own circle). Themed icons
  (`settings put secure themed_icons 1`) produced no visible retint for *any* app on this AVD's
  default wallpaper, ours included — an emulator/wallpaper limitation, not evidence about the
  monochrome layer, which is correctly wired.

- [x] **6.2 — build-logic: gplay and fdroid flavors, and a debug build that says it is one**
  There are no product flavors today (0.3 dropped Taiga's deliberately) and no
  `applicationIdSuffix`, so a debug install *replaces* a release one and both are called
  "Wallosmobile" on the launcher. Port Taiga's `AppFlavors.kt` + `configureFlavors()` — one `store`
  dimension, `gplay` and `fdroid`, the latter with `applicationIdSuffix = ".fdroid"` — and Taiga's
  `AppBuildTypes` `.debug` suffix on the debug build type. The debug *name* is Taiga's shape too:
  `androidApp/src/debug/AndroidManifest.xml` overriding `android:label="@string/app_name_debug"`,
  with a per-flavour-debug `strings.xml` under `src/gplayDebug/` and `src/fdroidDebug/`.
  **Nothing in this app differs by store** — no Play Services, no Firebase, nothing proprietary to
  strip — so this is distribution identity and side-by-side installs, not a source split. Don't
  create empty `src/gplay/kotlin` trees, and don't put the flavour in the About screen unless the
  user asks.
  *Verify:* `./gradlew :androidApp:assembleGplayDebug :androidApp:assembleFdroidDebug`, install
  both, `adb shell pm list packages | grep wallosmobile` showing two ids, and a launcher screencap
  showing two distinguishable names.
  ·  *Ref:* TaigaMobileNova `build-logic/.../AppFlavors.kt`, `AppBuildTypes.kt`,
  `androidApp/src/debug/AndroidManifest.xml`
  **Three things make this bigger than the diff.** It edits `build-logic/`, so the commit needs a
  `Gate-change:` line (`CLAUDE.md`, "Changing a gate means saying so"). It **breaks every emulator
  recipe in `CLAUDE.md`**: `installDebug` stops existing (it is `installGplayDebug` /
  `installFdroidDebug`), `compileDebugKotlin` — the Koin `--rerun-tasks` line — gains a flavour, the
  installed id becomes `com.grappim.wallosmobile.fdroid.debug` so every `run-as`, `am kill`,
  `force-stop` and DataStore-planting command changes, and `am start -n <pkg>/.MainActivity` needs
  the **full** class name because the class is not suffixed with the id. Updating those commands is
  part of the step, not follow-up. And `app_name` is **two** resources: the Android one in
  `androidApp/src/main/res/values/strings.xml` that the manifest labels with, and `:strings`'
  Compose resource that the drawer header reads — renaming only the first leaves the drawer saying
  the release name inside the debug app, which is either fine or the point, and the note should say
  which was chosen.
  Note: ported `AppFlavors.kt`/`AppBuildTypes.kt` from TaigaMobileNova verbatim (package
  `com.grappim.wallosmobile.buildlogic`) and wired both into `AndroidApplicationConventionPlugin` —
  `applicationIdSuffix` on the debug/release build types, `configureFlavors(this)` alongside
  `configureKotlinAndroid(this)`. No signing-config changes; Taiga's release/debug keystore blocks
  weren't part of this ask and this repo has no keystores to point them at.
  `androidApp/src/debug/AndroidManifest.xml` overrides only `android:label` with
  `tools:replace="label"` — Wallos's main manifest sets no `allowBackup`/`dataExtractionRules`
  overrides for Taiga's debug manifest to replace, so those attributes were left out rather than
  copied for parity. Per-flavour debug names live in `src/gplayDebug/res/values/strings.xml` and
  `src/fdroidDebug/res/values/strings.xml` alone (`"Dbg G Wallosmobile"` / `"Dbg F Wallosmobile"`) —
  no fallback `app_name_debug` in `main/`, since every real debug variant is one of those two
  flavour-debug source sets and a third definition would never be read.
  **The Compose `app_name` the drawer reads stays the release string in every variant** — Taiga's
  own drawer widget reads the same unsuffixed resource in its debug build, so this follows that
  precedent rather than inventing a second, flavour-aware string the plan never asked for; the
  Android manifest label is the only thing that changes.
  Verified on-device (macOS `Pixel_9a` AVD, not the Linux `Medium_Phone_API_36.1` this file
  otherwise names): `assembleGplayDebug`+`assembleFdroidDebug` both build,
  `installGplayDebug`+`installFdroidDebug` coexist, `pm list packages | grep wallosmobile` shows
  `com.grappim.wallosmobile.debug` and `com.grappim.wallosmobile.fdroid.debug` side by side, the app
  drawer shows "Dbg G Wallo…" and "Dbg F Wallo…" as distinct entries, and
  `am start -n com.grappim.wallosmobile.debug/com.grappim.wallosmobile.MainActivity` (full class
  name, no leading dot) launches the gplay variant. Rewrote every emulator recipe in this file that
  named `com.grappim.wallosmobile`, `installDebug` or `compileDebugKotlin` to the `gplayDebug`
  variant, and added a line documenting that choice — except "Package root `com.grappim.wallosmobile`."
  under Non-negotiables, which names the Kotlin namespace, not the applicationId, and is unaffected.
  `Gate-change:` this step edits `build-logic/`.

## M7 — Subscriptions: writes + reference data (plan §8, Phase 3)

Goal: a subscription can be created, edited and deleted from the app, with its logo, category,
payer, currency and payment method chosen from real server data rather than typed free-hand.
**Done when** all three actions round-trip against a live instance and the picker lists are the
account's own categories, household members and payment methods.

Two decisions plan §10 left open for "when Phase 3 starts" are settled by this decomposition
rather than by a step of its own, so no step below has to re-litigate them:

- **Three feature modules on `core:crud`, not one `feature:catalog`.** §3.4 already sketched this
  shape and it matches the granularity TaigaMobileNova uses for the same four resources — plan
  §10 moves this from "Still open" to "Settled" alongside this milestone.
- **Currencies don't get a fourth module.** `feature:subscriptions` already has a full read path
  for currencies — 2.3's `currencySymbol` join and 3.11's `observeCurrencies` cache — so a
  `feature:currencies` module here would duplicate it for no caller. 7.6's currency picker reads
  that existing flow; a standalone module with `add`/`edit` (rate maintenance) is Phase 5's
  management-screen work, and it can sit on `core:crud` like the other three when it lands.

- [x] **7.1 — core:crud: the shared CRUD contract**
  `CrudResource` (`id`, `name`, `inUse`) and `CrudApi<T>` (`getAll`/`add`/`edit`/`delete`) per plan
  §3.4, plus whatever the four `set_*.php` endpoints genuinely share: the `action=add|edit|delete`
  form shape, the per-resource ID-parameter alias, and the `"<Resource> in use"` delete failure
  (`WALLOS_API.md` §3.10). No DTOs live here — each feature's own DTO satisfies `CrudResource`
  through its mapper, the same way `feature:subscriptions` DTOs never touch `core:api` directly.
  *Verify:* `./gradlew :core:crud:testAndroidHostTest` — a fake resource type declared in the test
  round-tripped through `MockEngine` for `get`/`add`/`edit`/`delete`, plus the "in use" delete
  failure surfacing as a typed error.  ·  *Ref:* plan §3.4, `WALLOS_API.md` §3.10
  **Note:** the plan sketch was interfaces only; making `getAll`/`add` actually generic needed one
  more type, `CrudEndpoint` (`getPath`, `setPath`, `listKey`, `idParam`) — the list's wrapper key
  and the id alias differ per resource (`categories` vs `fakeId`-style aliases in the doc), so the
  one implementation, `WallosCrudApi<T>`, decodes to a raw `JsonObject` and pulls both out by name
  rather than needing a per-resource response DTO. The "in use" delete failure needed no crud-side
  code at all: `WallosErrorMapper`'s `title.endsWith(" in use")` branch already covers it for every
  endpoint, crud included. A feature's data module is expected to compose `WallosCrudApi` by
  delegation (`CrudApi<CategoryDTO> by WallosCrudApi(...)`) rather than reimplement the four calls —
  unverified until 7.2 actually does it.

- [x] **7.2 — feature:categories: data + domain + dto + mapper on core:crud**
  The simplest of the four resources — `name` only, plus `order` and `in_use`. `CategoryDTO`,
  domain `Category(id, name, inUse)` (`order` stays out of the domain model — nothing in Phase 3
  reads it; Phase 5's list screen adds it if it needs it, per 2.1's "domain model only what the
  screen renders"). `CategoriesRepository` wraps a `CrudApi<CategoryDTO>` pointed at
  `get_categories.php` / `set_categories.php` (`categoryId`/`id` alias). Category names are
  HTML-escaped on the wire like everything else server-rendered — reuse `HtmlUnescaper`, don't
  write a second one.
  *Verify:* `./gradlew :feature:categories:data:testAndroidHostTest`
  `:feature:categories:mapper:testAndroidHostTest` — get/add/edit/delete against `MockEngine`
  fixtures, and `"Category in use"` mapped to the typed delete-failure from 7.1.
  ·  *Ref:* `WALLOS_API.md` §3.10, plan §3.4
  **Note:** `CategoriesRepository` has no cache behind it — unlike `SubscriptionsRepository`
  (3.4), reference data has no offline requirement in this milestone, so `get`/`add`/`edit`/
  `delete` are plain round trips wrapped in `resultOf`, not `observe*`/`refresh*`. Reusing
  `HtmlUnescaper` means `feature:categories:mapper` takes `implementation(projects.feature.
  subscriptions.mapper)` — the first cross-feature dependency in the repo — and any module that
  constructs a real `CategoryMapper` in tests (`feature:categories:data`'s repository test) needs
  its own `implementation` line on it too, since `categories:mapper` doesn't re-export it as `api`.
  `CategoriesDataModule`/`CategoriesMapperModule` were wired into `AppModule`'s `includes` in this
  step, same as 2.3 did for subscriptions, even though nothing calls `CategoriesRepository` until
  7.6 — `KoinGraphTest`'s `verify()` costs nothing on an unreached definition and a forgotten
  `includes` line is a runtime crash waiting for whichever step first injects it.

- [x] **7.3 — feature:household: data + domain + dto + mapper on core:crud**
  Same shape as 7.2, copied — the second instance of the pattern should take a fraction of 7.2's
  time. Adds the optional `email` field. `memberId`/`id` alias.
  *Verify:* `./gradlew :feature:household:data:testAndroidHostTest`
  `:feature:household:mapper:testAndroidHostTest`, same coverage as 7.2.
  ·  *Ref:* `WALLOS_API.md` §3.10
  **Note:** confirmed `email` against the live PHP (`api/household/set_household.php`) rather than
  trusting the doc's summary alone — both `add` and `edit` run `email` through the same `validate()`
  as `name`, so `HouseholdMemberMapper` unescapes both, not just the name. The response's list key
  is `"household"`, not the plan's illustrative `"members"` — §3.4's `CrudEndpoint` sketch used a
  generic placeholder name, not the real one. Domain model is `HouseholdMember` (not `Household`,
  which would name the collection rather than a row) with methods `getMembers`/`addMember`/
  `editMember`/`deleteMember` on `HouseholdRepository`, matching 7.2's `Category`/`getCategories`
  naming pattern one level down. `composeApp/build.gradle.kts` needed the two new module lines
  (`implementation(projects.feature.household.data/.mapper)`) that 7.2 already added for categories
  — `AppModule`'s `includes` alone doesn't make the classes resolvable, the module needs the
  dependency too; `:androidApp:compileGplayDebugKotlin --rerun-tasks` is what caught the miss.

- [x] **7.4 — feature:paymentmethods: data + domain + dto + mapper on core:crud**
  Same shape again; `enabled` (`1`/`0`) and `icon`. **`icon` is already a full relative path**
  (`images/uploads/icons/paypal.png`), unlike a subscription's bare `logo` filename — its display
  URL is `{base}/{icon}` directly, no prefix to add client-side (`WALLOS_API.md` §4).
  `paymentId`/`id` alias. The write side accepts an `icon_url` fetch (same shape as subscriptions'
  `logo_url`, 7.8) and a `paymenticon` file upload — the upload is **out of scope here**: no picker
  screen calls it until Phase 5's management UI exists, and building it unreached would be the
  untested code CLAUDE.md's simplicity rule rules out.
  *Verify:* `./gradlew :feature:paymentmethods:data:testAndroidHostTest`
  `:feature:paymentmethods:mapper:testAndroidHostTest`, same coverage as 7.2.
  ·  *Ref:* `WALLOS_API.md` §3.10, §4
  **Note:** read the live PHP (`api/payment_methods/{get,set}_payment_methods.php`) rather than
  trusting the doc's summary alone — confirmed `enabled` is a SQLite `INTEGER` (`1`/`0` in the
  JSON, not a boolean), so `PaymentMethodDTO.enabled` is an `Int` and `PaymentMethodMapper` folds
  it to `Boolean` the same way `SubscriptionMapper` folds `inactive` (3.4's `INACTIVE_FALSE`
  pattern, here `ENABLED = 1`). `icon_url` **is** in scope, unlike the file upload: it costs
  nothing but an optional `String?` parameter on `addPaymentMethod`/`editPaymentMethod` — no
  platform code, no picker, so it carries its own host tests same as every other field, whereas
  the multipart upload would need 7.9's unreached `expect`/`actual` image-picker plumbing to even
  compile a test against. `PaymentMethodsRepository`'s methods therefore take one more parameter
  than 7.2/7.3's (`name`, `enabled`, `iconUrl: String? = null`) rather than mirroring either
  precedent exactly.

- [x] **7.5 — feature:subscriptions: add / edit / delete on the repository**
  `SubscriptionsRepository` gains `add(params): Result<Int>`, `edit(id, params): Result<Unit>`,
  `delete(id): Result<Unit>` — the one place in this feature where 3.4's "`observe*`/`refresh*`,
  never `get*`" rule doesn't apply as written, since a write is neither: it mutates the server, and
  the cache catches up by a refresh afterward (`add`/`edit` re-run the existing
  `refreshSubscriptions()` path; `delete` removes the row from Room directly rather than waiting on
  a refetch). New `AddSubscriptionParams`/`EditSubscriptionParams` in `domain`, encoding: `cycle`
  restricted to 1–4 at the type level — no `ONE_TIME`, `WALLOS_API.md` §3.4's server-side rejection
  kept client-side so the error never reaches the wire — dates as strict `YYYY-MM-DD` strings, and
  `"1"`/`"0"` (not `true`/`false`) for `auto_renew`/`notify`/`inactive`. No UI in this step.
  *Verify:* `./gradlew :feature:subscriptions:data:testAndroidHostTest` — add/edit/delete against
  `MockEngine`, the cache updated after each, and a server-rejected write mapped to `WallosError`.
  ·  *Ref:* `WALLOS_API.md` §3.4
  **Note:** read the live `set_subscriptions.php` (`docker exec wallos cat
  api/subscriptions/set_subscriptions.php`) rather than trusting the doc summary alone — it matched
  exactly, including that `add`'s response key is `subscriptionId` while `edit`/`delete` accept
  `id`/`subscriptionId`/`subscription_id` as aliases for the same parameter. "Cycle restricted at
  the type level" became a new `WritableBillingCycle` enum (`DAYS`/`WEEKS`/`MONTHS`/`YEARS`, no
  `ONE_TIME` member at all) rather than a runtime check on `BillingCycle` — 7.6's picker reads this
  enum, not `BillingCycle` filtered by `isWritable`. This feature does **not** route through
  `core:crud`: `set_subscriptions.php` shares the `action=add|edit|delete` shape but has ~18 fields
  against a resource-agnostic `name`, plus response/request id-key asymmetry `CrudEndpoint` doesn't
  model, so `SubscriptionsApi` grew three hand-written methods instead (mirroring `WallosCrudApi`'s
  envelope handling, not reusing it). This needed `feature:subscriptions:data` to add the
  `kmp.serialization` plugin (previously absent — the module had never decoded raw `JsonObject`
  before) to resolve `kotlinx.serialization.json`; not a tripwire path, no `Gate-change:` line.
  `add`/`edit` re-run `refreshSubscriptions()` and propagate its failure like any other step in the
  call; `delete` calls the new `SubscriptionDao.deleteById` directly. Three pre-existing hand-written
  fakes (`core:storage`'s and `feature:subscriptions:ui`'s two `FakeSubscriptionsRepository`s) needed
  the new abstract members added to keep compiling — the UI ones stub to `error("not used by this
  test")` per plan §6.1, since no screen calls them until 7.6/7.7.

- [x] **7.6 — feature:subscriptions:ui: the add/edit form (no logo)**
  New screen + ViewModel: name, price, currency (a picker over the *existing* `observeCurrencies`
  flow — no new module per this milestone's second settled decision), cycle + frequency (a picker
  over `BillingCycle`, excluding one-time), `next_payment`/`start_date`, category/payer/payment
  method (pickers over 7.2–7.4's repositories), notes, url, notify + `notify_days_before`,
  `auto_renew`, `inactive`. Form state carried in `SavedStateHandle` as one JSON string, per 5.2's
  precedent. New route registered in `NavKeySerializers`, reached from a FAB on the subscriptions
  list (1.8 parked `FabConfig` for exactly this). **Watch detekt's `allowedConstructorParameters:
  6`** — this is the first ViewModel taking four repositories at once (subscriptions, categories,
  household, payment methods) plus `SavedStateHandle`; if a sixth lands, split it the way 3.4 split
  `SubscriptionsCache` rather than widening the rule. No logo field and no delete yet — both are
  separate steps so this one stays reviewable on its own.
  *Verify:* on the emulator, against `wallos-scratch` (port 8284) rather than the live instance —
  open the FAB, fill every field with `input keyevent KEYCODE_TAB` between them, submit, and see
  the new subscription in the list.  ·  *Ref:* plan §7.3, `CLAUDE.md`'s SavedStateHandle note
  (5.2), `WALLOS_API.md` §3.4
  **Note:** the ViewModel took exactly the five dependencies the step text itself counted (four
  repositories plus `SavedStateHandle`) — no `subscriptionId`, so this step is add-only; 7.7 is
  what turns it into the add/edit form the title promises, and it will have to decide how a sixth
  dependency lands without tripping the "split rather than widen" rule. Currencies, categories,
  household members and payment methods are each a picker built from a new, reusable
  `EditorPickerUiState` (selected id + options + callback) rather than three loose parameters per
  field — that is also what keeps `PickerField` at four Composable parameters instead of six.
  Pickers use `ExposedDropdownMenuBox`/`ExposedDropdownMenu`, new to this repo (no prior
  dropdown/menu component existed) — the precedent for 7.7's edit form and Phase 5's catalog
  screens. `next_payment`/`start_date` use Material3's `DatePicker`/`DatePickerDialog` rather than
  free text, converting through `kotlin.time.Instant` at UTC (`atStartOfDayIn`/`toLocalDateTime`)
  since `initialSelectedDateMillis` is UTC millis — no `DateFormatter` injection needed, so the
  field just displays the raw `YYYY-MM-DD` it will send. **`Icons.Filled.Add` is in
  `material-icons-core` after all** — CLAUDE.md's "not even `Add`" line (written for 1.8, before
  a FAB existed to need it) doesn't hold for the resolved `1.7.3` artifact
  (`unzip`-and-`grep` on `material-icons-core-1.7.3.jar` shows `AddKt.class`); no
  `material-icons-extended` needed, corrected in `CLAUDE.md`. `FabConfig` (`None`/`Standard`),
  parked as a `RouteConfig` field since 1.8, is now real: `AuthenticatedMainScreen`'s `Scaffold`
  reads `appState.currentRouteConfig.fabConfig` and the FAB is never offline-gated, since
  navigating to the editor is not itself a write — only the form's own Save button is.

- [x] **7.7 — feature:subscriptions:ui: edit entry point + delete**
  An edit action on the detail screen opens 7.6's form pre-filled from the subscription the screen
  already has loaded — no extra round trip, the same reasoning 2.5 used to justify the detail
  screen's own re-read. A delete action (confirmation dialog) calls
  `SubscriptionsRepository.delete` and navigates back to the list.
  *Verify:* on the emulator against `wallos-scratch` — edit a field on a scratch row and confirm it
  lands; delete a different row and confirm it leaves the list. Not against `gregorz`'s real 35
  rows.  ·  *Ref:* plan §7.3
  **Note:** "no extra round trip" needed the domain `Subscription` model widened first — it carried
  `categoryName`/`paymentMethodName`/`payerName` (resolved text) but never the *ids* the editor's
  pickers need to pre-select an option, nor `autoRenew`/`notify`/`notifyDaysBefore`, which 7.6's form
  also has switches for. Added all six (`categoryId`, `paymentMethodId`, `payerUserId`, `autoRenew`,
  `notify`, `notifyDaysBefore`) with no defaults, matching the model's existing style — every other
  call site is a named-argument construction, so the compiler found every one that needed updating.
  This is 2.1's rule ("domain model only what the screen renders") working as designed: the editor
  is now a screen that renders these, so they earned their place. Threaded the same six fields
  through `SubscriptionEntity`/`SubscriptionEntityMapper` (cached, not re-fetched) and bumped
  `WallosDB` to version 3 — pre-v1 destructive fallback, no migration. `SubscriptionEditorRoute`
  went from `data object` to `data class(subscriptionId: Int? = null)`; the editor ViewModel takes
  `subscriptionId` as a sixth `@InjectedParam`/constructor dependency (still within detekt's
  `allowedConstructorParameters: 6`) and pre-fills asynchronously from
  `SubscriptionsRepository.observeSubscription(id).first()` in `init`, guarded by a
  `hadStoredForm` flag read *before* `persistForm()` starts writing — otherwise every construction
  would see a non-null saved form and skip the pre-fill, add or edit alike. An edit resubmits every
  field rather than diffing against `EditSubscriptionParams`' "omitted fields keep their current
  value" semantics, since the form always holds a real value for each one. Delete is a top-bar
  trash icon on the detail screen, gated behind an `AlertDialog` (`CertTrustDialog`'s shape, 3.8);
  only the dialog's own confirm button is offline-gated, matching 7.6's FAB precedent that
  navigating to a write is not itself a write. `Icons.Filled.Edit`/`Delete` are both in
  `material-icons-core` — no `material-icons-extended` needed, checked the same
  `unzip`-the-jar way as 7.6's `Add`.

- [x] **7.8 — feature:subscriptions: logo via `logo_url`**
  A text field in the editor for a source URL; the server fetches it server-side (max 3 redirects,
  5 s timeout, SSRF guard — `WALLOS_API.md` §3.4). **Re-read after write to confirm the logo
  landed** (plan §8's own Enforce bullet for this phase) — the `add`/`edit` response carries no
  resolved `logo` filename, so `get_subscription.php` afterward is the only way to know the fetch
  succeeded.
  *Verify:* on the emulator against `wallos-scratch` — set a `logo_url`, submit, and see the
  fetched logo render on the detail screen without restarting the app.
  ·  *Ref:* `WALLOS_API.md` §3.4
  **Note:** the "re-read after write" concern turned out already satisfied by 7.5's own shape —
  `add`/`edit` both call `refreshSubscriptions()` (the full list, not a single-row `get_subscription`)
  before returning, and that response's `logo` field is already the server-resolved filename, so no
  new re-read logic was needed. `logoUrl: String? = null` added to `AddSubscriptionParams`/
  `EditSubscriptionParams`, forwarded to `logo_url` in `SubscriptionsRepositoryImpl.toFormParams()`
  the same `?.let` way as `notes`/`url`. Confirmed against the live `set_subscriptions.php` PHP
  (`docker exec wallos cat …`) that a blank/omitted `logo_url` on edit leaves the existing logo
  untouched — matches `EditSubscriptionParams`' existing "omitted fields keep their current value"
  contract, so no special-casing needed there either. UI: one more `OutlinedTextField` in
  `SubscriptionEditorScreen`, not pre-filled on edit (the domain model only carries the bare logo
  *filename*, not a re-fetchable URL). Verified both paths live: added a subscription with a
  GitHub-hosted PNG as `logo_url` and watched it render on the list and detail screens after
  `Save` with no restart, then edited it with a second image URL and watched the detail screen's
  logo swap — both via `wallos-scratch`, both confirmed against the `Ktor` REQUEST/RESPONSE log
  (`set_subscriptions.php` → 200, followed by the existing `refreshSubscriptions()` triad).

- [x] **7.9 — feature:subscriptions: logo via multipart upload**
  A device image picker (Android's `ActivityResultContracts` — the one platform seam this
  milestone needs, `expect`/`actual` per the no-`androidMain`-in-features rule, the same shape
  1.4's Keystore access and 3.7's trust manager already use) feeding a multipart `logo` field
  alongside the rest of the form (png/jpg/jpeg/gif/webp, resized server-side to 135×42).
  *Verify:* `adb push` a small jpg onto the AVD's gallery path first if it has none, then on the
  emulator against `wallos-scratch` — pick it from the form, submit, and see it render as the logo.
  ·  *Ref:* `WALLOS_API.md` §3.4, §4
  **Note:** the "1.4/3.7 shape" turned out to mean *this feature module gets its first
  `androidMain` directory*, not that those two are themselves `expect`/`actual` — `SecretCipher`
  and `NetworkMonitor`/`CompositeTrustManager` are plain interfaces with a `core`-module Android
  impl reached through Koin, which doesn't apply here since `rememberLauncherForActivityResult`
  needs to run inside a `@Composable`. `feature:subscriptions:ui` did have `androidMain` available
  the whole time — `configureKmp()`/`com.android.kotlin.multiplatform.library` declare the Android
  target for every KMP module — it had simply never been used by a feature module until now.
  `core:api` grew `WallosApiClient.postMultipart` (`submitFormWithBinaryData`, one more file part
  beside the same urlencoded fields `post` sends) and a `MultipartFile` carrier class (`fieldName`/
  `fileName`/`mimeType`/`bytes` — a plain `class`, not `data class`: a `ByteArray` property gives
  a `data class` a reference-equality `equals`/`hashCode` pair that looks structural and isn't).
  `feature:subscriptions:domain` mirrors it with `LogoFile`, threaded through `AddSubscriptionParams`/
  `EditSubscriptionParams` as one more optional field; `SubscriptionsApi.addSubscription`/
  `editSubscription` gained an optional `logo: MultipartFile?` parameter and switch to
  `postMultipart` only when it is set, so every existing non-logo call site (and its tests) needed
  no change. Picked bytes live in `SubscriptionEditorUiState.logoFile`, set by
  `SubscriptionEditorViewModel.onLogoFilePick` — deliberately **not** folded into `SavedFormState`,
  since a `SavedStateHandle` value has to be Bundle-safe and cheap, and raw image bytes are neither;
  a process death between picking and saving loses the pick, the same way it loses anything else
  `restoreForm` cannot represent. `compose:parameter-naming` caught `onPicked` as past tense on
  first run (`ktlintCheck`/`detekt` both failed) — renamed to `onPick`/`onLogoFilePick` throughout,
  present tense per the existing `onClick`-not-`onClicked` rule. Verified live against
  `wallos-scratch`: pushed a small red JPEG onto the AVD (`adb push` + a `MEDIA_SCANNER_SCAN_FILE`
  broadcast so the system Photo Picker could see it, since it wasn't in any gallery bucket yet),
  picked it from the add form, saved, and watched it render as the logo on both the list and detail
  screens — confirmed against the `Ktor` log (`set_subscriptions.php` → 200, the usual
  `refreshSubscriptions()` triad after it), not just a screenshot. **M7 is done — Phase 3 is
  complete.**

## M8 — Dashboard (plan §8, Phase 4)

Goal: a home screen showing this month's cost, the period budget, and the subscriptions coming due
soonest — the three things plan §8 names for Phase 4, composed from `get_monthly_cost`,
`get_period_budget` and the cache `SubscriptionsRepository` already keeps. **Done when** the screen
renders all three against the live instance (port 8282) and is the app's landing screen — Dashboard
moves to the top of the drawer per plan §5.4's sketch, ahead of Subscriptions.

Two things plan §10/§4.6 leave open are settled by this decomposition, the same way M7 settled
catalog granularity, so no step below has to re-derive them:

- **No `VersionStorage`, no version-string comparison.** Plan §4.6 reads as "store `version.php`'s
  result and gate `get_period_budget` on it ahead of calling it," but neither `WALLOS_API.md` nor
  the live PHP (`docker exec wallos cat api/subscriptions/get_period_budget.php`, checked while
  decomposing this) names a minimum version — there is no server-side version check to mirror, and
  no changelog in the container to read one off. Guessing a cutoff would be exactly the kind of
  unconfirmed claim `CLAUDE.md` rules out. The gate that already exists for free is reactive:
  `WallosEnvelopeParser` turns any 404 into `WallosError.UnsupportedEndpoint`
  (`core/api/.../WallosEnvelopeParser.kt:35`) for every endpoint, `get_period_budget` included —
  8.1's repository just has to let that surface untouched, and 8.4's UI turns that one specific
  error into "hide the budget card" instead of a banner. This closes the Phase-4 slice of "To
  review"'s unowned version-gating item without building storage nothing would read yet (7.4's
  precedent for not building unreached code). `set_budget`'s period fields, `logo_variant` and
  `square_icons` stay Phase 5's job, and whichever of those steps first needs a real stored version
  is where `VersionStorage` gets built.
- **Network-only, no Room cache.** Unlike `SubscriptionsRepository`, `get_monthly_cost` and
  `get_period_budget` are period-relative snapshots, not a list to page through offline — matching
  `feature:categories`/`household`/`paymentmethods`'s existing precedent (7.2's note) of a plain
  round trip wrapped in `resultOf`, no `observe*`/`refresh*`. Upcoming payments is the one card that
  stays available offline, because it reads `SubscriptionsRepository.observeSubscriptions()`, which
  already is a cache.
- **`feature:dashboard:domain` depends on `feature:subscriptions:domain`**, the second cross-feature
  dependency in the repo after 7.2's mapper one — upcoming payments is `Subscription` rows filtered
  and re-sorted, and duplicating that type here would fork it in two places for no caller.

- [x] **8.1 — feature:dashboard: dto + data + domain — monthly cost & period budget**
  `MonthlyCostDTO`/`PeriodBudgetDTO` (`WALLOS_API.md` §3.5–3.6); domain `MonthlyCost`/
  `PeriodBudget` trimmed to what the screen renders (2.1's rule) — `period_label` is already a
  human-readable string from the server, so check whether a `budget_period_type` enum earns its
  place before adding one. `DashboardRepository` (or a narrower name if one call turns out to want
  no companion) with `getMonthlyCost(month, year)` / `getPeriodBudget(referenceDate)`, hand-written
  against `WallosApiClient` like `SubscriptionsApi` — neither endpoint fits `CrudApi<T>`'s
  add/edit/delete shape, so no `core:crud` dependency (mirrors 7.5's note for the same reason).
  `monthly_cost` is a comma-grouped string, not a JSON number — reuse `MoneyFormatter.parse`
  (`utils:formatter:decimal`), don't write a second parser.
  *Verify:* `./gradlew :feature:dashboard:data:testAndroidHostTest` — both calls' happy path
  against `MockEngine` fixtures, the comma-grouped `monthly_cost` string parsed correctly, and a
  404 on `get_period_budget` surfacing as `WallosError.UnsupportedEndpoint` unchanged.
  ·  *Ref:* `WALLOS_API.md` §3.5–3.6, plan §4.6
  Remember the two easy misses 7.2/7.3 already paid for once: the new modules need a line in root
  `build.gradle.kts`'s `kover { }` block, and `DashboardDataModule`/`DashboardDomainModule` need
  `AppModule`'s `includes` even before anything calls them.
  **Note:** confirmed both endpoints against the live PHP (`docker exec wallos cat
  api/subscriptions/get_{monthly_cost,period_budget}.php`) rather than the doc summary alone —
  both matched exactly. No `feature:dashboard:mapper` module: unlike categories/household/
  paymentmethods, the DTO→domain step here is plain field selection plus one `MoneyFormatter.parse`
  call, with no HTML-unescaping and no second caller, so `MonthlyCostMapper`/`PeriodBudgetMapper`
  are `@Single` classes living in `feature:dashboard:data` itself (still one per file, still
  unit-tested) rather than a separate Gradle module — CLAUDE.md's "add a layer when a real
  repository or a second caller turns up" argues against a module neither condition asks for. No
  `DashboardDomainModule` either: every existing `domain` module (categories/household/
  paymentmethods/subscriptions) has zero `@Single`-annotated definitions to scan, and this one is
  the same — nothing in `domain` needs Koin. `DashboardRepository` came out as one interface with
  both calls, not the narrower split the step floated; nothing pushed the two apart.
  `MonthlyCost` kept `title` (the server's own "March 2025" label) despite the trim, since 8.4's
  card plausibly wants a month heading and it costs nothing to carry. `PeriodBudget` kept both
  `amountRemainingThisPeriod` and `amountOverBudget` rather than deriving one from the other — the
  server clamps the former to 0 once over budget, so an "over by X" display needs the latter
  separately. `composeApp/build.gradle.kts` needed its own new `implementation(projects.feature.
  dashboard.data)` line too (7.2/7.3's reminder didn't name it, but every prior feature's data
  module is wired there the same way).

- [x] **8.2 — feature:dashboard:domain: upcoming payments**
  A pure class (`UpcomingPaymentsCalculator` or similar) taking the cached `List<Subscription>`
  from `SubscriptionsRepository.observeSubscriptions()` and returning the active ones sorted by
  next occurrence. "Derived locally from `next_payment` + `cycle`" (plan §8) means more than a
  sort: the server's own cron keeps `next_payment` current, but this app's cache can lag behind a
  missed refresh, so a `nextPayment` already in the past has to be rolled forward by `cycle` +
  `frequency` until it's today or later before sorting — otherwise a stale row would show at the
  top as "due" when it's already renewed server-side. Excludes inactive rows and ones with a null
  `nextPayment` or an unrecognised `cycle` (`BillingCycle.fromCode` returning `null`), the same way
  the list screen already treats an unparseable row.
  *Verify:* `./gradlew :feature:dashboard:domain:testAndroidHostTest` — a future `nextPayment`
  passes through unchanged; a past one rolls forward the right number of cycles for each
  `BillingCycle` value; inactive/null-cycle rows are excluded; output is sorted ascending.
  ·  *Ref:* plan §8 Phase 4
  **Note:** the exclusion list above turned out to be incomplete — checked the live cron
  (`docker exec wallos cat endpoints/cronjobs/updatenextpayment.php`) that actually rolls
  `next_payment` forward server-side, and its query is `WHERE next_payment < :currentDate AND
  auto_renew = 1 AND inactive = 0`. A past-due row with `autoRenew == false` is never touched by
  that cron, so it stays stuck in the past on the server forever — there is no real "next"
  occurrence to invent client-side, and the user confirmed (asked, since this wasn't in the step
  text) to exclude such a row rather than roll it forward anyway. `BillingCycle.ONE_TIME` gets the
  same treatment when past-due, for the same reason (no periodicity to roll by, and a one-time row
  is never auto-renewing in practice) — a future one-time `nextPayment` still passes through
  unchanged. Also excludes a non-positive `frequency` defensively, since the roll-forward loop
  would never terminate otherwise (the DTO has no validation ruling this out).

- [x] **8.3 — feature:dashboard:domain: DashboardHomeUseCase**
  Composes 8.1's two repository calls and 8.2's calculator over
  `SubscriptionsRepository.observeSubscriptions()` into one result the ViewModel collects — the
  app's first real use case (plan §6: "Wallos has real use-case candidates — see §8 Phase 4").
  The three sources fail independently (`UnsupportedEndpoint` on the budget call must not blank out
  the other two), so the shape this step has to decide is per-source `Result`s rather than one
  failure sinking the whole screen.
  *Verify:* `./gradlew :feature:dashboard:domain:testAndroidHostTest` — all three sources present;
  a period-budget failure still leaves monthly cost and upcoming payments populated; an
  upcoming-payments feed with no active subscriptions renders empty, not failed.
  ·  *Ref:* plan §6 "Use cases only when a screen needs multiple repository calls"
  **Note:** `DashboardHomeUseCase.getDashboardHomeData(today: LocalDate): DashboardHomeData` —
  `today` is a parameter, not read from `Clock.System` internally, mirroring 8.2's calculator
  signature and keeping the use case deterministic to test. `getMonthlyCost` derives month/year
  from it and `getPeriodBudget` gets it as `referenceDate`; `UpcomingPaymentsCalculator` gets it
  unchanged. `DashboardHomeData` (three independent fields: `Result<MonthlyCost>`,
  `Result<PeriodBudget>`, `List<Subscription>`) lives directly as data, not wrapped in an outer
  `Result` — wrapping it would put the three sources back behind one failure, which is the thing
  this step exists to avoid. Upcoming payments comes from a single `observeSubscriptions().first()`
  snapshot rather than staying subscribed to the flow — nothing on this screen refreshes the cache
  itself (8.4's own "nothing on this screen writes"), so there is no second emission to react to
  yet; a future screen that adds pull-to-refresh would be the point to revisit this. This is the
  first domain module with any Koin content in the app (every prior one scanned to zero), so it
  needed `alias(libs.plugins.wallosmobile.kmp.di)` added to its `build.gradle.kts`, a new
  `DashboardDomainModule` (`@Module @Configuration @ComponentScan`), and that module wired into
  `AppModule`'s `includes` in `Koin.kt` — `KoinGraphTest` confirmed the wiring. `composeApp` also
  needed a direct `implementation(projects.feature.dashboard.domain)` line: it already had
  `feature.dashboard.data`, but that dependency is `implementation`, not `api`, so the domain
  module class wasn't visible transitively. `UpcomingPaymentsCalculator` stays unannotated and is
  constructed directly inside `DashboardHomeUseCaseImpl` rather than injected — it has zero
  dependencies of its own, the same "stop injecting it" case CLAUDE.md's cache-repository bullet
  describes, and it keeps the constructor at 2 params instead of 3 for no test benefit (8.2 already
  covers it in isolation). `LocalDate.monthNumber` is deprecated in kotlinx-datetime 0.8.0 but its
  replacement (`.month.number`) doesn't exist in this version's `Month` enum yet, so the deprecated
  member stays rather than reaching for an API that isn't there.

- [x] **8.4 — feature:dashboard:ui: the home screen, and it becomes the landing screen**
  `DashboardRoute`/`DashboardScreen`/`DashboardViewModel`/`DashboardUiState`: a monthly-cost card,
  a period-budget card that 8.1's `UnsupportedEndpoint` hides rather than errors (this milestone's
  version-gating decision, applied), and an upcoming-payments list whose rows navigate to the
  existing `SubscriptionDetailRoute(id)` — no new detail surface needed. New route registered in
  `DrawerDestination`/`DRAWER_NAV_ITEMS`/`NavKeySerializers` (miss any of the three and either the
  drawer entry does nothing or `NavKeySerializersTest`/process-death restore breaks silently, per
  `CLAUDE.md`'s nav3 rule). Drawer entry added **above** Subscriptions, matching plan §5.4's
  sketch, and **`START_DESTINATION` flips from `SubscriptionsRoute` to `DashboardRoute`** — settled
  here since a drawer ordering that puts Dashboard first only makes sense if it's also where the
  app opens. No FAB, no offline-write gating — nothing on this screen writes.
  *Verify:* on the emulator against the live instance (port 8282, `docs/local-info.txt`) — launch
  the app, land on Dashboard (not Subscriptions), see this month's cost and the upcoming-payments
  list with a real row, tap one and land on its detail screen, back, open the drawer and confirm
  Dashboard sits above Subscriptions. Confirm the period-budget card renders against this instance
  (v5.4.2) rather than only exercising its absence — the hide-on-404 path needs an older instance
  to actually prove, which `docs/local-info.txt`'s throwaway instances may not cover; note in this
  step whether one was available.
  ·  *Ref:* plan §5.4, §7.1 UI-state patterns, `CLAUDE.md`'s Screen/Content split
  **Note:** confirmed on device against the live v5.4.2 instance — Dashboard is the landing screen,
  both cards render with real data (`€711.39` monthly, `€0.00 remaining` budget), the
  upcoming-payments list shows real rows, a tap lands on that row's existing detail screen, back
  returns to Dashboard, and the drawer shows Dashboard above Subscriptions. No older instance was
  available in `docs/local-info.txt` to exercise the hide-on-404 path directly — covered instead by
  `DashboardViewModelTest`'s `WallosError.UnsupportedEndpoint` case. `feature:dashboard:ui` is a new
  module (route/screen/ViewModel/UI state + `MonthlyCostCard`/`PeriodBudgetCard`/
  `UpcomingPaymentRow` widgets), wired the same way 8.1/8.3 already needed: its own
  `settings.gradle.kts` include, root `kover{}` line, `composeApp` dependency line, and
  `DashboardUiModule` in `AppModule`'s `includes`.
  Two deviations from `subscriptions:ui`'s established shape, both required by this step's own
  version-gating decision: the ViewModel names `WallosError.UnsupportedEndpoint` directly to decide
  hide-vs-error on the budget card, so `core:domain` is a **main-source** dependency here, not the
  test-only one `subscriptions:ui` uses; and `feature:dashboard:ui` depends directly on
  `feature:subscriptions:domain` for the `Subscription` type (the same "`implementation`, not
  `api`, so it isn't visible transitively" reminder 8.1/8.3 already paid, paid a third time).
  Reused rather than duplicated: `RString.subscriptions_retry` and `.subscriptions_next_payment`
  (first cross-feature string reuse in the repo — `:strings` is common to every feature, so this is
  no different from any other shared module), and `SubscriptionDetailRoute` for row taps (8.4's own
  "no new detail surface" instruction). `Icons.Filled.Home` for the drawer entry is in
  `material-icons-core`'s existing ~50-icon set, confirmed before use. One `onRetryClick` on
  `DashboardUiState` reloads all three sources together rather than each card retrying
  independently — there is one use-case call behind all three, so a per-card retry would have
  nothing narrower to re-run. `kotlinx-datetime` 0.8.0 deprecated `kotlinx.datetime.Clock` to a
  typealias for `kotlin.time.Clock`; `Clock.System.todayIn(...)` needs the `kotlin.time.Clock`
  import now, not the `kotlinx.datetime` one every other date-handling file in this repo predates.
  This step's own `Verify:` line names only the on-device check, unlike every prior `ui`-layer
  step's `testAndroidHostTest` — `DashboardViewModelTest` was written anyway, matching the
  established per-feature convention, and passes alongside it.
  **M8 is done — Phase 4 is complete.**

## M10 — Dashboard: web parity (not in plan §8's phase order)

Goal: close the four gaps `docs/CHECKLIST.md`'s own "To review" filed against the dashboard (8.4)
after comparing it directly to the real Wallos web UI (`/home/gregory/proj/other/Wallos`, confirmed
`v5.4.2`, same as the docker instance) — `index.php` and `includes/stats_calculations.php`, not
`WALLOS_API.md` alone, are the source of truth for what's built here. **Takes priority over M9, and
keeps it as long as this milestone has open steps** (reaffirmed when reopened, below): decided with
the user 2026-08-08 — the dashboard doesn't ship as "done" while it disagrees with the one UI its
own numbers are supposed to describe. **Done when** the mobile dashboard's sections match the web
dashboard's *numbers*, not just its card set, verified card-by-card against the live instance.

**Reopened 2026-08-08, the same day it first closed.** 10.6/10.7's own on-device verification only
checked that each card *rendered*, not that its numbers matched the web's own — the user then asked
directly where Monthly Budget's number came from, having never seen it on the web, and flagged
Your Subscriptions' cost figures as different from the web's too. Logging into the live web UI with
a real session cookie (`curl`, not just reading PHP) and diffing its rendered dashboard against the
app confirmed two real gaps, filed as 10.8/10.9 below. **M9 (Management screens) is deliberately
pushed to last** — decided with the user: dashboard work doesn't hand off to M9 piecemeal one gap at
a time, to avoid tracking two active fronts at once; M9 starts only once this milestone has nothing
left in it.

Settled while scoping this milestone, each checked against the live PHP source rather than assumed:

- **`feature:profile` is pulled forward from M9, built minimal.** Decided with the user: dto +
  domain + data only, `getUser()` alone — enough for `user.budget`, which nothing on the dashboard
  can reach today (`get_period_budget.php`'s own SQL only ever selects `period_budget`,
  `budget_period_type`, `budget_period_anchor_date` — never `budget`; confirmed reading
  `api/subscriptions/get_period_budget.php` live). M9's 9.8 later *adds* `setBudget()` to this same
  repository rather than building a second `get_user.php` caller — the module isn't otherwise
  different from what 9.8 already specified, just built ahead of its number.
- **Everything else needs no new endpoint** — Overdue Renewals, the Upcoming cap, Your
  Subscriptions and Your Savings are all computable from `SubscriptionsRepository`'s existing cache
  plus `MonthlyCost` (already fetched). Confirmed reading `stats_calculations.php`'s own loop
  (lines ~195–262): active count, monthly/yearly cost and inactive count/savings are a single pass
  over the same subscription list this app already caches.
- **Both dashboard queries the web actually runs exclude `cycle = 5` (one-time) entirely** —
  `index.php:76` (Upcoming) and `:85` (Overdue) both carry `AND cycle != 5`. Our
  `UpcomingPaymentsCalculator.resolve()` does not: a *future* one-time subscription
  (`nextPayment >= today`) passes through unchanged regardless of cycle, so today's dashboard can
  show a one-time purchase the web's own dashboard never would. Found while scoping this milestone,
  not previously known.
- **Overdue Renewals is exactly the set `UpcomingPaymentsCalculator` (8.2) already excludes** —
  `index.php:85`'s query (`next_payment < today AND auto_renew = 0 AND inactive = 0 AND cycle !=
  5`, unlimited, no rolling) selects precisely the rows 8.2's own cron precedent
  (`endpoints/cronjobs/updatenextpayment.php`, `WHERE next_payment < :currentDate AND auto_renew =
  1 AND inactive = 0`) already established the server itself never advances. 8.2's calculator
  already computes and discards this exact set; it becomes a second output instead of a dropped
  one. The **roll-forward behavior stays** for auto-renewing past-due rows — that's this app's own
  compensation for a cache the web doesn't have (8.2's own reasoning), not something to remove for
  parity.
- **Upcoming Payments caps at 3** (`index.php:76`, `LIMIT 3`) — a `.take(3)` after sorting, since
  the cache is already local; no query-level limit needed.
- **"Budget" was one card; the web has two, gated differently.** `index.php:259–370` /
  `stats_calculations.php:295–334`:
  - **Monthly Budget** — shown whenever a monthly cost exists (i.e. almost always), and *contains*
    Monthly Cost as one of its rows rather than being a separate card. Its
    `budget`/`budget_used`/`budget_remaining`/`over_budget` sub-rows only appear when
    `user.budget > 0` (this app's own `monthlyBudget - totalCostPerMonth`, min/max-clamped exactly
    as `stats_calculations.php:301–304` does it — mirror that formula, don't re-derive one).
  - **Period Budget** — shown only when the active period is *not* the plain calendar month
    (`stats_calculations.php:290–293`: compares `period_start`/`period_end` against the calendar
    month's own start/end). `PeriodBudgetDTO`/`PeriodBudget` dropped `period_start`/`period_end` in
    8.1 ("8.4's card doesn't need them") — they need restoring for this gate to be computable at
    all. **10.9 found this gate was incomplete** — see below.
- **A known simplification, not yet decided**: the web's `totalSavingsPerMonth` subtracts the
  monthly cost of any inactive row's `replacement_subscription_id` (`stats_calculations.php:242–
  262`) — cancelling A for B nets the saving against what B now costs. `Subscription` (domain)
  dropped `replacementSubscriptionId` on purpose (2.1's trim), though `SubscriptionDTO` already
  carries it. The step that builds Your Savings decides whether to restore the field and mirror the
  offset, or ship the simpler "sum of inactive rows' prices" and say so in its `Note:`.
- **AI Recommendations is out of scope** — reads as a paid/hosted-only feature from its own name
  (`index.php:183–257`, a per-user `ai_recommendations` table with "savings" copy no endpoint in
  `WALLOS_API.md` describes), not a candidate for parity.

Settled while **reopening** this milestone 2026-08-08, root-caused by logging into the live web UI
(`curl` with a session cookie) and diffing its rendered dashboard against the app, not just reading
PHP:

- **`get_monthly_cost.php` and `stats_calculations.php`'s own `$totalCostPerMonth` are two different
  metrics that happen to share the name "monthly cost."** `get_monthly_cost.php` (`MonthlyCost`,
  fetched since 8.1, and what 10.6/10.7 both display) sums every billing *occurrence* landing within
  a named calendar month — a weekly subscription counts 4–5 times, confirmed both in its own PHP
  loop and in the `wallos` MCP tool's own description. The web's Dashboard cards instead read
  `$totalCostPerMonth`, which normalizes each active subscription to a single monthly-equivalent via
  `getPricePerMonth` — exactly what `SubscriptionStatsCalculator.pricePerMonth` already ports, today
  used only for `Your Savings`'s inactive-row sum. Confirmed live: web `€496.63`, mobile `€711.39`,
  same account, same moment. `get_monthly_cost.php`'s own metric isn't wrong, it's `stats.php`/
  `calendar.php`'s "amount due this month" (`grep amountDueThisMonth` — computed in
  `stats_calculations.php` but never rendered by `index.php`), a page this app has no equivalent of;
  not a candidate fix. Filed as **10.8**.
- **Period Budget's gate was missing a third condition.** The web's own gate (`index.php:317`) is
  `$periodDiffersFromCalendarMonth && isset($userData['period_budget']) &&
  $userData['period_budget'] > 0` — three conditions ANDed. `PeriodBudget.isRedundantWithCalendarMonth`
  (10.2) only encodes the first; nothing checks whether the budget amount itself is `> 0`. Confirmed
  live: this account's `period_budget` is `0` (`get_period_budget.php`'s own `notes` field already
  says `"Period budget is set to 0."`), so the web never renders a Period Budget section at all, but
  `get_period_budget.php` still answers `success: true` with a zeroed budget rather than an error, so
  10.6's card shows anyway. **Not** a case for deleting the card outright — a genuinely non-calendar
  period *with* a real period budget set should still show it. Filed as **10.9**.
- **Aside, not filed as a step**: `Your Savings`'s monthly figure also differs from the web
  (`€38.82` web vs. `€102.82` mobile) — but this is the *already-known, already-documented*
  `replacement_subscription_id` simplification from 10.4's own `Note:`, confirmed exactly on this
  account (Vattenfall id 12's replacement, eprimo id 37, costs €64.00/month; €102.82 − €64.00 =
  €38.82, the web's own number). 10.4 already decided and documented this gap; not re-opened here.
  The web's `Your Savings` card also has a "Yearly Savings" row (`€465.84`) mobile has no equivalent
  of — noted in case it's wanted later, not filed as its own step since nobody asked for it.

- [x] **10.1 — feature:profile: dto + domain + data — `getUser()` only**
  New module, minimal by design (this milestone's preamble). `UserDTO` (`WALLOS_API.md` §3.9 —
  `id`, `budget`, `period_budget`, `main_currency`; skip `password`/`api_key`, always masked, and
  anything M9's 9.9 needs that this card doesn't). `ProfileRepository.getUser(): Result<User>`,
  hand-written against `WallosApiClient` like `DashboardRepository` (neither `core:crud` nor a
  cache fit a single-row endpoint with no `add`/`edit`/`delete`).
  *Verify:* `./gradlew :feature:profile:data:testAndroidHostTest` — happy path against `MockEngine`
  fixtures, `budget`/`period_budget` parsed as numbers.
  ·  *Ref:* `WALLOS_API.md` §3.9, this milestone's preamble
  **Note:** `get_user.php` nests the row under a `"user"` key (`{"success":true,"user":{...}}`),
  unlike `get_monthly_cost.php`/`get_period_budget.php`'s flat envelope — confirmed against the
  live PHP shown in this milestone's preamble. Needed a `UserResponse(val user: UserDTO)` wrapper
  in `feature:profile:dto`, the same shape `get_subscription.php`'s own `SubscriptionResponse`
  already uses for its own nested `"subscription"` key; `ProfileApiImpl.getUser()` reads
  `apiClient.post<UserResponse>(...).user`. Domain `User` mirrors `UserDTO`'s four fields
  one-for-one (`id`, `budget`, `periodBudget`, `mainCurrencyId`) since none is dead weight at this
  trim. No `ProfileDomainModule`: `feature:profile:domain` has zero `@Single`-annotated
  definitions, the same as every other feature's `domain` module before 8.1's own
  `DashboardDomainModule` fixed on that fact (`docs/archive/CHECKLIST-DONE.md` 8.1's note) — its
  `build.gradle.kts` carries only `kmp.library`, no `kmp.di`. `feature:profile:data` needed its own
  `implementation(projects.feature.profile.data)` line in `composeApp/build.gradle.kts` and
  `ProfileDataModule::class` in `AppModule`'s `includes` even though nothing calls it yet — 8.1's
  same reminder, repeated here since nothing enforces it structurally.

- [x] **10.2 — feature:dashboard: budget domain rework**
  Restore `period_start`/`period_end` to `PeriodBudgetDTO`/`PeriodBudget` (dropped in 8.1). Add a
  derived `PeriodBudget.isRedundantWithCalendarMonth: Boolean` (or equivalent), computed by
  comparing `periodStart`/`periodEnd` against `today`'s calendar-month bounds — mirror
  `stats_calculations.php:290–293` exactly, don't re-derive the comparison from scratch. New
  `MonthlyBudget` domain model (`amount` from `feature:profile`'s `User.budget`, `used`/
  `remaining`/`overBudget` derived against `MonthlyCost.amount` the same clamped formula
  `stats_calculations.php:301–304` uses), built where `MonthlyCost`/`PeriodBudget` already live —
  a pure calculation, no new endpoint, so no new Koin-scanned class needed for it specifically.
  *Verify:* `./gradlew :feature:dashboard:domain:testAndroidHostTest` — `isRedundantWithCalendarMonth`
  true for a plain monthly period, false for a period anchored elsewhere (matching the live
  instance's own `Jul 18–Aug 17` period, `budget_period_anchor_date: "2026-07-18"`); `MonthlyBudget`
  derivation clamps `remaining` to 0 and only sets `overBudget` when cost exceeds budget, and is
  entirely absent (not zero) when `user.budget` is 0 — mirrors `isset($monthlyBudget) &&
  $monthlyBudget > 0`'s gate, not just its arithmetic.
  ·  *Ref:* `WALLOS_API.md` §3.6, §3.9, this milestone's preamble
  **Note:** `MonthlyBudget` is a plain `data class` with a `from(budget, monthlyCost): MonthlyBudget?`
  factory in its own file (`model/MonthlyBudget.kt`) — no `@Single`/`@Factory`, per the step's own
  "no new Koin-scanned class needed." `isRedundantWithCalendarMonth(today: LocalDate)` on
  `PeriodBudget` takes `today` as a parameter rather than reading a clock, matching
  `UpcomingPaymentsCalculator.calculate`'s existing shape. `PeriodBudgetDTO.periodStart`/
  `periodEnd` are `String` (mapper parses via `DateFormatter`, injected — `feature:dashboard:data`
  needed a new `implementation(projects.utils.formatter.datetime)` line) and required rather than
  nullable: unlike `next_payment`/`start_date`, the API doc gives no "unset" case for a period's own
  bounds, so the mapper throws (via `requireNotNull`, caught by `resultOf` same as `MonthlyCostMapper`'s
  `Malformed` case) rather than silently dropping the period. `used`/`overBudget` on `MonthlyBudget`
  mirror `stats_calculations.php`'s own variable names precisely (`$monthlyBudgetUsed` is a 0–100
  percentage, not an absolute amount) — worth flagging since "used" reads as an amount at first
  glance.

- [x] **10.3 — feature:dashboard:domain: Overdue Renewals + Upcoming Payments capped at 3**
  Extends `UpcomingPaymentsCalculator` (or splits it into a class that returns both lists in one
  pass over the same filtered/sorted sequence — decide here) to also return the past-due,
  non-auto-renewing rows it currently drops, and to exclude `BillingCycle.ONE_TIME` from the
  *upcoming* side unconditionally (this milestone's preamble — today it only excludes a *past-due*
  one-time row, not a future one). Both lists exclude inactive rows and mirror the web's `cycle !=
  5` filter; Overdue Renewals also excludes `nextPayment == null`/unrecognised-`cycle` rows the same
  way Upcoming already does, but is otherwise unlimited (`index.php:85`, no `LIMIT`). Upcoming caps
  at 3 after sorting.
  *Verify:* `./gradlew :feature:dashboard:domain:testAndroidHostTest` — a past-due, non-auto-renewing
  row lands in Overdue, not dropped; a past-due auto-renewing row still rolls forward into Upcoming
  exactly as 8.2 left it; a future one-time subscription is excluded from Upcoming (regression for
  the gap this milestone found); more than 3 eligible upcoming rows yields exactly 3, the 3 soonest.
  ·  *Ref:* `WALLOS_API.md` §3.1, this milestone's preamble
  **Note:** Kept a single `UpcomingPaymentsCalculator` class (the "decide here" this step left open)
  rather than splitting it — `calculate()` now returns a new `UpcomingAndOverdue(upcoming, overdue)`
  in one pass: a private `Eligible(subscription, nextPayment, cycle)` step filters inactive rows,
  null `nextPayment`/`cycle`, and `BillingCycle.ONE_TIME` up front (mirrors `cycle != 5` on *both*
  web queries, not just Upcoming's — the step text only called out excluding one-time from Upcoming,
  but the preamble's own "both dashboard queries... exclude cycle = 5 entirely" covers Overdue too,
  confirmed reading `index.php:76`/`:85` together), then a single `forEach` buckets each row into
  `upcoming` (future as-is, or past-due+auto-renew rolled forward, per 8.2's existing logic) or
  `overdue` (past-due+non-auto-renew, kept at its original date) before each list is sorted
  ascending and only `upcoming` is `.take(3)`. `DashboardHomeData` gained a required
  `overdueRenewals: List<Subscription>` field (no default, since `feature:dashboard:ui`'s
  `DashboardViewModel` already ignores fields it doesn't read yet) — this rippled into all seven
  `DashboardHomeData(...)` construction sites in `DashboardViewModelTest`, same shape 10.2's own
  `periodStart`/`periodEnd` addition already rippled into that file. `feature:dashboard:ui` itself
  is untouched; wiring `overdueRenewals` into `DashboardUiState`/the screen is 10.6.

- [x] **10.4 — feature:dashboard:domain: Your Subscriptions + Your Savings**
  A pure class over the cached subscription list (no new endpoint, this milestone's preamble):
  active count, monthly cost (already fetched — reuse it, don't resum), yearly cost
  (`monthlyCost × 12`, matching `stats_calculations.php`'s own `$totalCostPerYear`); inactive count,
  and a savings figure — decide and document here whether it includes the
  `replacement_subscription_id` offset (this milestone's preamble's open item) or the simpler sum,
  either way with a `Note:` saying which.
  *Verify:* `./gradlew :feature:dashboard:domain:testAndroidHostTest` — counts and both cost figures
  against a small fixed subscription list; savings excludes inactive one-time rows the same way
  the other two calculators do, if that's where the step lands.
  ·  *Ref:* this milestone's preamble
  **Note:** New `SubscriptionStatsCalculator` (`calculator/`) plus `YourSubscriptions`/`YourSavings`
  domain models (`model/`), following `UpcomingPaymentsCalculator`'s own shape — a plain class,
  constructed directly rather than Koin-injected, since `DashboardHomeUseCaseImpl` will build it the
  same way it already builds `UpcomingPaymentsCalculator` (that wiring is 10.5, untouched here).
  Went with the **simpler sum, no `replacement_subscription_id` offset**: `Subscription` (domain)
  doesn't carry that field (2.1's trim) and restoring it just for this one card's edge case (a
  cancelled row replaced by another) outweighs the parity gain — the number is real, just not
  identical to the web's when a replacement exists. `activeCount` mirrors
  `stats_calculations.php`'s own count exactly (excludes `cycle = 5` even though the row is active);
  `inactiveCount` has no such filter, matching the PHP, since a one-time row's `getPricePerMonth`
  already contributes 0 to savings regardless of whether it's counted.

- [x] **10.5 — feature:dashboard:domain: `DashboardHomeUseCase` recomposition**
  Composes 10.1's `ProfileRepository.getUser()` alongside 8.1's two calls and 10.3/10.4's
  calculators into a wider `DashboardHomeData` (independent `Result`s per source, same reasoning
  8.3 already established: a failed `getUser()` must not blank out monthly cost or the subscription
  lists). `feature:dashboard:domain` gains a dependency on `feature:profile:domain` the same shape
  it already has on `feature:subscriptions:domain`.
  *Verify:* `./gradlew :feature:dashboard:domain:testAndroidHostTest` — every source still present;
  a `getUser()` failure leaves the rest of `DashboardHomeData` populated.
  ·  *Ref:* plan §6, this milestone's preamble
  **Note:** `DashboardHomeData` gained `user: Result<User>` plus two *derived* fields —
  `monthlyBudget: MonthlyBudget?` and `subscriptionStats: SubscriptionStats?` (10.4's
  `SubscriptionStatsCalculator`, wired in here as this step's own text anticipated). Both derived
  fields are `null`, not zeroed, whenever `monthlyCost` itself failed — `MonthlyBudget.from` and
  `SubscriptionStatsCalculator.calculate` both need the unwrapped `Double` amount, and a fabricated
  0 would read as "no cost" rather than "unknown," so `monthlyCostAmount?.let { … }` gates both
  computations on that one `Result` rather than each guessing independently; `monthlyBudget`
  additionally needs `user` to have succeeded, since it reads `User.budget`. `DashboardHomeUseCaseImpl`
  gained a third constructor parameter (`profileRepository: ProfileRepository`) and a second
  calculator field (`subscriptionStatsCalculator`), fetched via a third `async` alongside the
  existing two. `KoinGraphTest` confirmed the wider constructor still resolves — `ProfileRepository`
  was already bound by 10.1's `ProfileRepositoryImpl`/`ProfileDataModule`, already in `AppModule`'s
  includes, so no DI wiring changed outside this module.
  Widening `DashboardHomeData`'s required-field list rippled into two test files exactly the way
  10.2/10.3's own field additions already did: `DashboardHomeUseCaseTest` (this module, three new
  cases — full population, a `getUser()` failure, and a `monthlyCost` failure proving both derived
  fields go absent) and `feature:dashboard:ui`'s `DashboardViewModelTest`, whose seven
  `DashboardHomeData(...)` construction sites all needed `user`/`monthlyBudget`/`subscriptionStats`
  added (`null`/a fixed success `User`, since the ViewModel doesn't read any of the three yet —
  that's 10.6/10.7). The `User` ripple needed a new `commonTest`-only
  `implementation(projects.feature.profile.domain)` line in `feature/dashboard/ui/build.gradle.kts`
  — `dashboard:domain`'s own dependency on `profile:domain` is `implementation`, not `api`, so the
  type wasn't visible there transitively (the same rule `CLAUDE.md`'s DI section already documents
  for plain types, paid a new time here in test scope rather than main-source).

- [x] **10.6 — feature:dashboard:ui: Overdue, Upcoming (capped), Monthly Budget, Period Budget**
  Rebuilds the screen's card set: an Overdue Renewals section above Upcoming Payments (only when
  non-empty), Upcoming Payments now genuinely capped, `MonthlyCostCardUiState` folded into a wider
  Monthly Budget card (cost always shown; budget/used/remaining/over-budget rows only when present,
  per 10.2's gate) and `PeriodBudgetCardUiState` hidden (not just its own `isHidden` from
  `UnsupportedEndpoint`, per 8.4) whenever 10.2's `isRedundantWithCalendarMonth` is true.
  *Verify:* `./gradlew :feature:dashboard:ui:testAndroidHostTest`, and on the emulator against the
  live instance — confirm Monthly Budget's Monthly Cost line matches the web's `Hello John` page
  reading side by side, Period Budget shows (this instance's period is `Jul 18–Aug 17`, genuinely
  not a calendar month), and Upcoming Payments shows at most 3 rows even with more than 3
  eligible — check whether an Overdue row exists on this account to actually exercise that section,
  and note in this step if none did.
  ·  *Ref:* this milestone's preamble, `CLAUDE.md`'s Screen/Content split
  **Note:** `MonthlyCostCard.kt` was renamed to `MonthlyBudgetCard.kt` (state renamed
  `MonthlyCostCardUiState` → `MonthlyBudgetCardUiState`, `amount` → `costAmount`); its "Monthly
  cost" row label is now a sub-row of the wider card rather than the card's own title, and the card
  title itself, plus `PeriodBudgetCardUiState`'s, come from two new strings
  (`dashboard_monthly_budget_title`/`dashboard_period_budget_title`) matching the web's own labels
  (`en.php`'s `monthly_budget`/`period_budget`) rather than the placeholder "Budget" both cards
  shared before. `dashboard_budget_remaining`/`dashboard_budget_over` are now reused by both cards
  rather than being period-budget-only. `UpcomingPaymentUiItem`/`UpcomingPaymentRow` are reused
  as-is for Overdue rows (identical shape — id/name/price/next payment — confirmed against
  `index.php:137–180`'s overdue markup), not duplicated into a second type. The redundancy check
  itself moved to the **domain** layer rather than being recomputed in the ViewModel: `today` is
  only available in `DashboardViewModel.load()` via `Clock.System.todayIn(...)`, a real wall-clock
  read, and computing `isRedundantWithCalendarMonth` there would have made `DashboardViewModelTest`
  depend on the actual current date (this environment's real "today" being 2026-08-08, itself
  inside the existing Aug-1–Aug-31 test fixtures' calendar month — the collision was not
  hypothetical, three tests would have started failing the moment this ran). `DashboardHomeData`
  gained a `isPeriodBudgetRedundant: Boolean`, computed once in `DashboardHomeUseCaseImpl` against
  the same deterministic `today` its own tests already fix, mirroring how `monthlyBudget`/
  `subscriptionStats` were already "derived, not fetched" there (10.5). Verified on the emulator
  against the live instance: Monthly Budget showed `€711.39` (matches `get_monthly_cost` exactly)
  with no budget sub-rows, since this account's `budget` is `0`; Period Budget showed and was not
  hidden, period label `Jul 18 - Aug 17` confirming a genuinely non-calendar period; Upcoming
  Payments showed exactly 3 of this account's 28 active subscriptions. **No Overdue row exists on
  this account** — confirmed via `wallos_list_subscriptions`, every one of the 28 active rows has
  `auto_renew: 1`, so none can ever land on the overdue side of 10.3's calculator; the section's
  absence is therefore correct, not unverified.
  **Correction, 2026-08-08 (10.9 filed against this)**: "Period Budget showed and was not hidden"
  above was verified as "renders without crashing," not as "matches the web" — the web never shows
  this section on this account at all (`period_budget` is `0`), so what 10.6 called a pass was a
  narrower check than it read as. See 10.9.

- [x] **10.7 — feature:dashboard:ui: Your Subscriptions + Your Savings**
  Two more cards from 10.4's stats, shown only when their counts are `> 0` — matching
  `index.php:373`/`:411`'s own gates, not shown unconditionally.
  *Verify:* `./gradlew :feature:dashboard:ui:testAndroidHostTest`, and on the emulator — confirm
  the active count and monthly/yearly cost match the web's "Your Subscriptions" card, and that
  Your Savings is absent if this account has no inactive subscriptions (check and note which it
  was).
  ·  *Ref:* this milestone's preamble
  **Note:** `YourSubscriptionsCardUiState`/`YourSavingsCardUiState` and their two widgets
  (`YourSubscriptionsCard.kt`/`YourSavingsCard.kt`) follow `MonthlyBudgetCard`'s own
  Card/Column shape exactly — title, then one `bodyMedium` row per field, each row a single
  combined "label: value" string (new `dashboard_active_subscriptions`/
  `dashboard_monthly_cost_amount`/`dashboard_yearly_cost`/`dashboard_inactive_subscriptions`/
  `dashboard_monthly_savings` strings, `%1$d`/`%1$s` printf style per `CLAUDE.md`). Both cards
  read `currencySymbol` off `data.monthlyCost`'s own `Result` (`getOrNull()?.currencySymbol`,
  falling back to `""`) rather than adding a new source — `SubscriptionStats` carries no currency
  of its own, and the web's own figures are in the same main-currency total `MonthlyCost.amount`
  already is. `DashboardScreen`'s `LazyColumn` appends both cards after Upcoming Payments,
  gated on `activeCount > 0`/`inactiveCount > 0`; this doesn't match the web's own card order
  (Monthly Budget/Period Budget sit *after* Overdue/Upcoming there, `index.php:184-411`) since
  10.6 already fixed this app's order the other way around and reordering existing cards was out
  of this step's scope.
  Verified on the emulator against the live instance: **Your Subscriptions** showed
  "Active subscriptions: 28", "Monthly cost: €711.39" (matching Monthly Budget's own figure),
  "Yearly cost: €8,536.68" (`711.39 × 12`, exact). **Your Savings** was present — this account
  has 7 inactive subscriptions (`wallos_list_subscriptions(state: "inactive")` confirmed the
  count) — "Inactive subscriptions: 7", "Monthly savings: €102.82"; hand-summing each row's
  `getPricePerMonth` (Fiton €2.6658 + Congstar €18 + Disney+ €8.99 + Flo €0.8325 + Praktika AI
  €8.3325 + Vattenfall €59 + Komoot €4.9992) lands on exactly €102.82, confirming
  `SubscriptionStatsCalculator`'s math on real data. One of the seven (Vattenfall, id 12) does
  carry a `replacement_subscription_id` (37) — 10.4's own known simplification (no offset) is
  therefore live on this account's real numbers, not just a hypothetical.
  **Correction, 2026-08-08 (10.8 filed against this)**: "Monthly cost: €711.39 (matching Monthly
  Budget's own figure)" above was true but not the point — that figure is `get_monthly_cost.php`'s
  own metric, matching Monthly Budget only because 10.6 fetched the same wrong source for both
  cards, not because either matches the web. Real web figure on this account: `€496.63`/`€5,959.57`.
  See 10.8.

- [x] **10.8 — feature:dashboard: active-subscription monthly cost, normalized like the web's — not `get_monthly_cost.php`'s**
  Replace `MonthlyCost.amount` as the source for `MonthlyBudget.from`'s cost figure and
  `YourSubscriptions.monthlyCost`/`.yearlyCost` with a locally computed sum of `pricePerMonth` over
  *active* subscriptions — mirrors the sum `SubscriptionStatsCalculator` already runs over inactive
  rows for `Your Savings`, just applied to the other side, and matches `stats_calculations.php`'s
  own `$totalCostPerMonth` (`inactive == 0`, unconditional on `cycle`, since `getPricePerMonth`
  already returns 0 for `cycle = 5`). This milestone's preamble ("Settled while reopening…") has the
  full root cause and the confirmed live numbers (web `€496.63`/`€5,959.57` vs. mobile's current
  `€711.39`/`€8,536.68`). **Decide here** whether `DashboardRepository.getMonthlyCost()`/
  `get_monthly_cost.php` stays fetched for anything afterward — `MonthlyCost.title` ("August 2026")
  and `.currencySymbol` are still read by the Monthly Budget card's sub-label, so the call may still
  be needed for those even once `.amount` isn't, or that label may be computable locally instead;
  say which in the `Note:`.
  *Verify:* `./gradlew :feature:dashboard:domain:testAndroidHostTest` — the normalized active sum
  matches `stats_calculations.php`'s formula against a fixed subscription list (extend 10.4's own
  fixtures rather than duplicating them), and excludes one-time rows the same way the existing
  savings sum does. On the emulator against the live instance: Monthly Budget's Monthly Cost row and
  Your Subscriptions' Monthly Cost/Yearly Cost rows read `€496.63`/`€5,959.57` — not `€711.39`/
  `€8,536.68` — matching the logged-in web dashboard read side by side.
  ·  *Ref:* `stats_calculations.php:195-224` (`$totalCostPerMonth`), this milestone's preamble
  **Note:** `SubscriptionStatsCalculator.calculate(subscriptions)` dropped its external
  `monthlyCost: Double` parameter and now sums `pricePerMonth` over active rows itself (the same
  private helper `Your Savings` already summed over inactive rows), so both
  `YourSubscriptions.monthlyCost` and `MonthlyBudget.from`'s cost argument read from that one local
  sum rather than `MonthlyCost.amount`. **Decided: `getMonthlyCost()`/`get_monthly_cost.php` stays
  fetched**, but purely for `MonthlyCost.title` ("August 2026") and `.currencySymbol` — nothing
  reads `.amount` any more (left in the domain model rather than removed, since it's still a
  correctly-mapped value from that endpoint, just unused by this screen now).
  `DashboardHomeUseCaseImpl.monthlyBudget`/`.subscriptionStats` **stay gated on
  `monthlyCost.getOrNull()`** even though neither needs its `.amount` any more — both format their
  own numbers using its `.currencySymbol`, so a failed fetch still means nothing to format with,
  not "no cost data" (documented on `DashboardHomeUseCase`'s own KDoc rather than left as a silent
  invariant). Verified on device (real AVD, not just unit tests): Monthly Budget's Monthly Cost row
  and Your Subscriptions' Monthly Cost/Yearly Cost rows all read **€496.63**/**€5,959.57** — the
  web's own figures, not the old €711.39/€8,536.68.

- [x] **10.9 — feature:dashboard:ui: Period Budget — hide when the account's own period budget is 0**
  Add a `periodBudget.periodBudget <= 0` check to `DashboardViewModel.toPeriodBudgetCardState`,
  alongside the existing `isRedundantWithCalendarMonth`/`UnsupportedEndpoint` gates — mirrors
  `index.php:317`'s third ANDed condition (`$userData['period_budget'] > 0`), which nothing here
  checks today. This milestone's preamble has the full root cause and the confirmed live account
  state (`period_budget: 0`, `get_period_budget.php`'s own `notes` already saying so).
  *Verify:* `./gradlew :feature:dashboard:ui:testAndroidHostTest` — a `PeriodBudget` with
  `periodBudget <= 0` hides the card even when `isRedundantWithCalendarMonth` is false and the fetch
  succeeded (a case 10.6's own tests never covered, since its fixture always used a positive
  `periodBudget`). On the emulator against the live instance, confirm Period Budget no longer
  renders at all — this account's real `period_budget` is `0`.
  ·  *Ref:* `index.php:317`, this milestone's preamble
  **Note:** The gate lives inside `onSuccess`, not as a third top-level early-return next to
  `isRedundant` — it needs `budget.periodBudget` from the unwrapped success value, so it wraps the
  existing `PeriodBudgetCardUiState(...)` construction in an `if (budget.periodBudget <= 0) hidden
  else { ... }` rather than sitting beside the `isRedundant` check the way that one does (it's
  evaluated before unwrapping `Result`). Verified on the emulator against the live instance: the
  Dashboard now shows Monthly Budget → Upcoming Payments → Your Subscriptions → Your Savings, no
  Period Budget card anywhere and no crash — matching the web, which never renders one for this
  account's `period_budget: 0`. **M10 is done — Dashboard: web parity is complete.**

## M9 — Management screens (plan §8, Phase 5)

Goal: full add/edit/delete UI for the four catalog resources (categories, household, payment
methods, currencies), plus a budget editor — everything plan §7.3's "Manage" row and "Profile +
budget" row sketch. **Done when** each of the four has a list screen (reachable from a new
*Manage* drawer group) with add/edit/delete against the live instance, and Settings has a Profile
sub-screen showing the budget and letting it be edited.

Server-side display settings (`get_settings.php`/`set_settings.php`) are **explicitly out of this
milestone** — decided 2026-08-08 with the user: it's an open ~12-field map (color theme, custom
CSS, `week_starts_sunday`, …) that mostly governs the Wallos *web* UI's own rendering, and nothing
in this app has a concrete reason to edit any of it yet. Revisit if one turns up.

Several things are settled here, checked against the live instance (port 8282) and live PHP source
(`docker exec wallos cat api/...`) while scoping this milestone, so no step below has to re-derive
them:

- **Categories, household and payment methods already have full add/edit/delete** — built in M7
  (`CategoriesRepository`/`HouseholdRepository`/`PaymentMethodsRepository`, all on `core:crud`),
  tested, and unused by any screen. This milestone's work on those three is **UI only**: a list
  screen and an add/edit form per resource, the same `SubscriptionEditorRoute(id: Int?)` shape
  7.6/7.7 already established for "one form, nullable id decides add vs. edit."
- **Currencies gets its own module, `feature:currencies`** (dto/domain/data/ui, on `core:crud`),
  rather than extending `feature:subscriptions`. §3.4's original note left this open ("a standalone
  module... can sit on `core:crud`... when it lands") but a stale aside in §7.2 said the opposite
  ("never earns its own module") — both now corrected in `IMPLEMENTATION_PLAN.md`. Decided with the
  user: `feature:subscriptions` keeps its existing trimmed, read-only `Currency` (no `rate`/`inUse`)
  for the price picker and list join, unchanged; the new module gets its own `CurrencyDTO` (with
  `rate`/`inUse` restored) and full CRUD. A small duplication of one DTO, accepted over a
  cross-feature reach into a module that has no business owning currency management.
  `WallosCrudApi.getAll()`'s generic shape drops `get_currencies.php`'s top-level `main_currency`
  field (it only reads `envelope[listKey]`), so **9.1 has a real choice to make**: skip surfacing
  "which currency is main" in the list (delete on it still fails cleanly — see below) or hand-write
  `getAll()` the way `SubscriptionsApi.getCurrencies()` already does or to get `main_currency`
  alongside the list, same as it does for `feature:subscriptions`.
- **Payment methods gets its icon multipart upload now too**, decided with the user: reuse 7.9's
  shape (`MultipartFile`, `postMultipart`, Android's `ActivityResultContracts`) rather than
  deferring again — `PaymentMethodsRepository.addPaymentMethod`/`editPaymentMethod` already carry an
  `iconUrl` param and a doc comment naming this exact gap ("no picker calls it before Phase 5"),
  and 7.9 already paid the one-time cost (the module's first `androidMain` directory, the
  `MultipartFile` carrier type in `core:api`). Confirmed live (`docker exec wallos cat
  api/payment_methods/set_payment_methods.php`): the upload resizes server-side to **70×48**, not
  135×42 like a subscription logo — a different constant, now in `WALLOS_API.md` §3.10.
- **A budget editor is a Settings sub-screen, not a new drawer destination** — decided with the
  user: `SettingsRow` → a new `ProfileRoute`, the same shape `InterfaceRoute`/`AboutRoute` already
  use, not a fifth top-level `DrawerDestination`.
- **The "Manage" drawer group is `DrawerItem.Group`'s first real use.** `WallosDrawerWidget`
  already renders it (a header plus its nested `Destination`s) — built, never exercised, since
  `DrawerItemsBuilder` has only ever emitted flat `Destination`s. Each of the four catalog screens
  is still its own `DrawerDestination` enum case with its own sub-stack, registered in
  `DRAWER_NAV_ITEMS`/`NavKeySerializers` exactly like Dashboard/Subscriptions/Settings — "Manage" is
  purely a `WallosDrawerWidget` grouping, not a different navigation shape.
- **Two delete guards are pre-existing defaults, not in-use rows**, confirmed in the live PHP:
  category id `1` and household member id `1` can never be deleted (`"Cannot delete category"` /
  `"Cannot delete member"`, `docker exec wallos cat api/categories/set_categories.php` /
  `api/household/set_household.php`), the same shape currency's "can't delete the main currency"
  guard already has (`"Cannot delete currency"`). All three already map to `WallosError.InUse` —
  `WallosErrorMapperTest` already covers all three titles — so no new error-handling code is
  needed; an editor screen *may* choose to disable Delete on these proactively for a better message
  than "that item is still in use elsewhere," but it isn't required for correctness. Payment
  methods has no such guard.
- **A real `WALLOS_API.md`/`IMPLEMENTATION_PLAN.md` bug, found and fixed while scoping this
  milestone**: three places (`WALLOS_API.md` §1 and §3.9, `IMPLEMENTATION_PLAN.md` §4.4, and a
  comment plus a test in `WallosEnvelopeParser.kt`) claimed `get_user.php` returns `notes` as an
  empty *string* rather than an array. A live `curl` against `get_user.php` on this instance
  returns `"notes": []` — a real array, same as every other endpoint checked. All four corrected;
  `WallosEnvelopeParser`'s defensive safe-cast is left in place (costs nothing) but its comment no
  longer claims the string case is observed. The source of the original wrong claim isn't known.

- [x] **9.1 — feature:currencies: dto + domain + data — full CRUD on `core:crud`**
  Mirrors 7.2's shape for `feature:categories` exactly: `CurrencyDTO : CrudResource` (`id`, `name`,
  `symbol`, `code`, `rate`, `inUse`) — `api(projects.core.crud)` in the `dto` module's
  `build.gradle.kts`, per 7.2's own reminder. `CurrenciesApi : CrudApi<CurrencyDTO>` delegating to
  `WallosCrudApi` (`get_currencies.php`/`set_currencies.php`, id param `currencyId`/`id`,
  `docs/WALLOS_API.md` §3.10). Domain `Currency` carries `rate`/`inUse` this time — unlike
  `feature:subscriptions`'s trimmed one, this screen is exactly where both matter.
  `CurrenciesRepository`: `getCurrencies()`, `addCurrency(name, symbol, code, rate)`,
  `editCurrency(id, name, symbol, code, rate)`, `deleteCurrency(id)` — decide here whether `getAll()`
  stays `WallosCrudApi`'s generic form (drops `main_currency`) or a hand-written `getCurrencies()`
  keeps it alongside the list, per this milestone's preamble.
  *Verify:* `./gradlew :feature:currencies:data:testAndroidHostTest` — happy path against
  `MockEngine` fixtures for all four calls, `rate` round-trips as a string on the wire and a
  `Double` in the domain model, and a delete on an in-use or main currency surfaces as
  `WallosError.InUse` unchanged.
  ·  *Ref:* `WALLOS_API.md` §3.10, plan §3.4, this milestone's preamble
  ·  *Note:* Took the hand-written path, mirroring `SubscriptionsApi.getCurrencies()`: `CurrenciesApi`
  is *not* `CrudApi<CurrencyDTO>` (that interface's `getAll(): List<T>` has no room for
  `main_currency`) — its `getAll()` returns a `CurrenciesPayload` decoded straight into a
  `CurrenciesResponse` DTO (`apiClient.post<CurrenciesResponse>`, no manual `JsonObject`/`JsonArray`
  parsing needed, since the envelope parser already decodes arbitrary shapes). `add`/`edit`/`delete`
  still delegate to a private `WallosCrudApi<CurrencyDTO>` instance held by composition rather than
  interface delegation. Domain `Currency` carries a fourth new field, `isMain` — computed by
  `CurrencyMapper.toDomain(dto, mainCurrencyId)` comparing each row's id against the payload's
  `main_currency` — rather than a separate `mainCurrencyId` sitting beside the list, so 9.6's list
  screen has one thing to render per row instead of two to correlate. `rate.toDoubleOrNull() ?: 1.0`
  in the mapper is a genuine fallback, not one hit by any live data: every row on both the live and
  the scratch instance already carries a plain decimal string (`"1"`, `"1.1000"`), confirmed via the
  `wallos` MCP and `set_currencies.php`'s live PHP source. Wired into `AppModule`/`Koin.kt` and
  `composeApp`'s `build.gradle.kts` now, same as 7.2/7.3/7.4/10.1 all did at their own data-only
  step rather than waiting for a UI step to land.

- [x] **9.2 — feature:categories:ui: list + add/edit/delete**
  `CategoriesRoute`/`CategoriesScreen` (list, FAB per 7.6's `FabConfig` precedent) and
  `CategoryEditorRoute(categoryId: Int?)`/`CategoryEditorScreen` (one text field: name). Delete
  behind a confirmation dialog, same shape as 7.7's subscription detail
  (`isDeleteDialogOpen`/`onDeleteClick`/`onDeleteConfirm`/`onDeleteDialogDismiss`). No cache (this
  milestone's whole surface is reference data, per 7.2's precedent) — a plain load-on-open list, a
  refresh after any successful write.
  *Verify:* `./gradlew :feature:categories:ui:testAndroidHostTest`, and on the emulator against the
  live instance — add a category, edit its name, delete it, and confirm deleting the default
  category (id 1) or one still referenced by a subscription surfaces the in-use error rather than
  crashing or silently failing.
  ·  *Ref:* `WALLOS_API.md` §3.10, `CLAUDE.md`'s Screen/Content split, this milestone's preamble
  ·  *Note:* Ran only the host-test half of Verify — the emulator half needs a way to reach this
  screen, and nothing does yet: the FAB is entirely shell-driven (`RouteConfigProvider.getConfig`
  in `composeApp`, unwired until 9.7), and the "Manage" drawer group that would open `CategoriesRoute`
  is also 9.7's own scope, by this milestone's own preamble ("Wires all four new routes..."). So the
  on-device add/edit/delete/in-use-error check for all four catalog screens is deferred to land right
  after 9.7 wires navigation, not skipped. Two other choices worth recording for 9.3–9.6, which
  will hit the same shape:
  (1) **One route pair, not three** — `CategoriesRoute` (list) and `CategoryEditorRoute(categoryId,
  name)` (add/edit/delete together). A category is one field, so there is no separate detail screen
  the way subscriptions has one; edit and delete both live on the editor, gated on `categoryId`.
  (2) **`CategoryEditorRoute` carries the row's `name` alongside `categoryId`**, populated straight
  from the list screen's own `uiState.items` at the tap — `CategoriesRepository` has no
  single-row fetch (only `getCategories()`), so this avoids a second full-list round trip just to
  prefill one field.
  (3) **`CategoriesViewModel` has no `init { load() }`** — deliberately, unlike every other
  no-cache ViewModel so far (`DashboardViewModel`, `SubscriptionDetailViewModel`). This list has to
  reload both on first open and on every return trip from the editor after a write, and Nav3
  disposes a covered entry's composition and restarts it when it comes back on top — so
  `CategoriesScreen`'s own `LaunchedEffect(Unit) { uiState.onRetryClick() }` is the single load path
  for both cases, and an `init` block would just double the first load. Data and DI wiring
  (`CategoriesUiModule`, `AppModule`'s `includes`, `composeApp`'s `build.gradle.kts`) landed now,
  same as 9.1 did for its data module — `KoinGraphTest` already resolves both new ViewModels.
  Nav wiring itself (`NavKeySerializers`, `DrawerDestination`, `RouteConfigProvider`, entry
  providers) stays 9.7's, per its own scope.

- [x] **9.3 — feature:household:ui: list + add/edit/delete**
  Same shape as 9.2, two fields (name, optional email) per `HouseholdRepository.addMember`/
  `editMember`. Reuses whatever generic list/editor/delete-dialog composables 9.2 produces if they
  turn out reusable across resources — worth checking before writing a second copy.
  *Verify:* `./gradlew :feature:household:ui:testAndroidHostTest`, and on the emulator — add,
  edit, delete a household member; confirm member id 1 (or one still referenced by a subscription)
  fails with the in-use error.
  ·  *Ref:* `WALLOS_API.md` §3.10, this milestone's preamble
  ·  *Note:* No reusable list/editor/delete-dialog composables came out of 9.2 to share — its
  `CategoriesScreen`/`CategoryEditorScreen` are plain, one-field-specific Composables with nothing
  factored out, so this step wrote its own `HouseholdScreen`/`HouseholdMemberEditorScreen` following
  the same shape rather than extracting a shared widget for a second, still-small user. Same
  deferral as 9.2's own note: the emulator half of Verify needs a way to reach this screen, and
  nothing does until 9.7 wires the "Manage" drawer group, so only the host-test half ran here — all
  14 tests pass (`HouseholdViewModelTest`, `HouseholdMemberEditorViewModelTest`), `detekt`/
  `ktlintCheck` pass project-wide, and `KoinGraphTest` resolves both new ViewModels after a clean
  `:androidApp:compileGplayDebugKotlin --rerun-tasks`. One field differs from 9.2's shape: `email` is
  optional (`HouseholdMember.email` is a plain, always-present `String` — blank reads as absent, same
  as the domain model's own doc comment), so `HouseholdMemberEditorViewModel.onSaveClick` validates
  only `name`, never `email`. Data/DI wiring (`feature:household:ui`'s `build.gradle.kts`,
  `settings.gradle.kts`, root `kover { }`, `composeApp`'s `build.gradle.kts` and `Koin.kt`) landed
  now, same as 9.1/9.2 did at their own step. Nav wiring itself (`NavKeySerializers`,
  `DrawerDestination`, `RouteConfigProvider`, entry providers) stays 9.7's, per its own scope.

- [x] **9.4 — feature:paymentmethods:ui: list + add/edit (name, enabled, icon_url) + delete**
  Same shape again: name, an enabled toggle, and `icon_url` as a text field (server-fetched,
  7.8's precedent) — the multipart picker is 9.5, not this step. Delete behind the same
  confirmation dialog.
  *Verify:* `./gradlew :feature:paymentmethods:ui:testAndroidHostTest`, and on the emulator — add a
  payment method with an `icon_url`, see the fetched icon render, edit its enabled state, delete a
  method not referenced by any subscription; confirm one that is referenced fails with the in-use
  error.
  ·  *Ref:* `WALLOS_API.md` §3.10, this milestone's preamble
  ·  *Note:* Same deferral as 9.2/9.3's own note: the emulator half of Verify needs the "Manage"
  drawer group, which is 9.7's scope, so only the host-test half ran here — all 15 tests pass
  (`PaymentMethodsViewModelTest`, `PaymentMethodEditorViewModelTest`), `detekt`/`ktlintCheck` pass
  project-wide, and `KoinGraphTest` resolves both new ViewModels (including the `Boolean`
  `@InjectedParam` on `PaymentMethodEditorViewModel` — not on `verify()`'s primitive whitelist, so
  this is the first step to actually exercise that path rather than rely on the whitelist) after a
  clean `:androidApp:compileGplayDebugKotlin --rerun-tasks`.
  One route choice worth recording: unlike `HouseholdMemberEditorRoute`, `PaymentMethodEditorRoute`
  does *not* carry `iconUrl` alongside `name`/`enabled` on the edit path — `PaymentMethod.icon` (the
  list's own field) is a server-*resolved* path, not the source URL a caller submits, and the two
  are never the same string. Leaving the field blank on open is exactly the value that already
  means "leave the icon untouched" per `PaymentMethodsRepository.editPaymentMethod`'s own doc
  comment, so there is nothing to prefill.
  The list row's icon reuses `BaseUrlProvider` (`core:api`) the same way `feature:subscriptions:ui`
  does for a logo, but through its own `toIconUrl` (`icon` is already root-relative, confirmed in
  `WALLOS_API.md` §4 — no `images/uploads/.../` segment to insert, unlike a subscription logo's
  bare filename) and its own small `PaymentMethodIcon` composable — a cross-feature reach into
  `feature:subscriptions:ui` for a private, non-`api` widget isn't a seam this codebase uses.
  `PaymentMethodIcon` skips `SubscriptionLogo`'s `logoRefreshToken` (5.6): that token exists to
  retry a request Coil considers already `Error` without changing its cache key, needed because a
  subscription's `logo` field can go from unreachable to reachable while staying the *same*
  filename (a flaky server); here every reload is a fresh `getPaymentMethods()` call building a
  brand new `PaymentMethodUiItem` list from whatever `icon` the server currently reports, so a
  request that resolves differently is already a different request. Data/DI wiring
  (`feature:paymentmethods:ui`'s `build.gradle.kts`, `settings.gradle.kts`, root `kover { }`,
  `composeApp`'s `build.gradle.kts` and `Koin.kt`) landed now, same as 9.1–9.3 did at their own
  step. Nav wiring itself (`NavKeySerializers`, `DrawerDestination`, `RouteConfigProvider`, entry
  providers) stays 9.7's, per its own scope.

- [x] **9.5 — feature:paymentmethods: icon via multipart upload**
  Mirrors 7.9 exactly: an Android image picker (`ActivityResultContracts`, this module's first
  `androidMain`) feeding a multipart `paymenticon` field. Server resizes to 70×48 (confirmed live,
  this milestone's preamble) — different from a subscription logo's 135×42, so don't assume the
  picker's crop/preview aspect ratio without checking.
  *Verify:* on the emulator against the live instance — pick an image for a payment method's icon,
  save, and see it render on the list without restarting the app (7.9's own verify shape).
  ·  *Ref:* `WALLOS_API.md` §3.10, §4; archive `CHECKLIST-DONE.md` 7.9
  ·  *Note:* Ran only the host-test half of Verify — same deferral as 9.2–9.4's own notes:
  `PaymentMethodEditorRoute` still isn't registered in `NavKeySerializers`/`DrawerDestination`
  (confirmed by grep, still 9.7's scope), so nothing in the running app can reach this screen yet.
  The on-device pick/save/render check is deferred to land right after 9.7, alongside the other
  three catalog screens' own deferred checks.
  One infra choice worth recording: `WallosCrudApi` (`core:crud`) gained `addWithFile`/
  `editWithFile` — two new methods on the *class*, not on the generic `CrudApi<T>` interface, so
  categories and household (which never take a file) are untouched. `PaymentMethodsApi` switched
  from `CrudApi<PaymentMethodDTO> by WallosCrudApi(...)` (interface delegation) to composition — a
  private `crud` field — mirroring 9.1's `CurrenciesApi` precedent, since `addWithFile`/
  `editWithFile` aren't part of `CrudApi` and so aren't reachable through a `by` delegate.
  `feature:paymentmethods:domain` gained `IconFile` (bytes/fileName/mimeType), a plain, non-`data`
  class mirroring `feature:subscriptions`' `LogoFile` for the same reason (a `ByteArray` property
  would give `equals`/`hashCode` a structural look they don't have). `PaymentMethodsRepository.
  addPaymentMethod`/`editPaymentMethod` gained an `iconFile: IconFile? = null` parameter alongside
  the existing `iconUrl`; the repository impl branches to `api.addWithIcon`/`editWithIcon` only
  when a file is present, leaving every existing no-icon call site unchanged. UI side mirrors 7.9's
  `LogoPicker`/`LogoFilePicker` shape exactly: an `expect`/`actual` `rememberIconFilePickerLauncher`
  (this module's first `androidMain`, `feature:paymentmethods:ui/build.gradle.kts` gained the same
  `androidx.activity.compose` line 7.9 added to `feature:subscriptions:ui`), a
  `PaymentMethodEditorUiState.iconFile`/`onIconFilePick`, and an `IconFilePicker` composable (pick
  button + "Selected: <filename>" text) next to the existing `iconUrl` field. Confirmed
  `paymenticon` as the exact multipart field name via the live PHP (`docker exec wallos cat
  api/payment_methods/set_payment_methods.php`), not just the doc summary, per `CLAUDE.md`'s
  "a step that says it already checked an API still gets checked" rule.

- [x] **9.6 — feature:currencies:ui: list + add/edit/delete**
  Fields: name, symbol, code, rate (default `1.0`). Same list/editor/delete-dialog shape as
  9.2–9.4. Decide here whether the list marks the main currency (per 9.1's `getAll()` choice) and
  whether the editor disables Delete on it proactively, or leaves it to the server's
  `"Cannot delete currency"` error (this milestone's preamble covers both are already correct,
  just a UX choice).
  *Verify:* `./gradlew :feature:currencies:ui:testAndroidHostTest`, and on the emulator — add,
  edit, delete a currency; confirm the main currency and any currency still referenced by a
  subscription fail to delete with the in-use error.
  ·  *Ref:* `WALLOS_API.md` §3.10, this milestone's preamble
  ·  *Note:* Both open questions decided the same way: the list marks the main currency (a
  trailing "Main" badge on the row, mirroring `PaymentMethodRow`'s "Disabled" badge — the one
  precedent for a conditional label at the end of a catalog row), but the editor does **not**
  proactively disable Delete on it — left to the server's `WallosError.InUse`, same as categories'
  id 1 and household's member 1. Checked the actual web UI (`settings.php` around line 1044) before
  deciding, per `CLAUDE.md`'s rule: it *does* disable the button proactively there, for both the
  main currency and any currency still referenced by a subscription (`in_use`, the same field this
  app's own `Currency.inUse` already carries) — so the data to match it is free, no extra round
  trip. Went the other way anyway, for consistency with the three sibling screens already shipped
  (9.2–9.4) rather than making currencies the one catalog screen with special-cased delete UX; a
  server error already surfaces correctly (covered by `WallosErrorMapperTest`) and this is a UX
  choice, not a defect. Worth revisiting all four screens together if this ever comes up again,
  not currencies alone.
  `rate` is a `String` in `CurrencyEditorUiState`, the same shape
  `SubscriptionEditorUiState.price` uses for a decimal field (`KeyboardType.Decimal`), parsed with
  `toDoubleOrNull()` only on save; an unparseable rate joins a blank name/symbol/code under one
  `currency_editor_error_invalid` message. `CurrencyEditorRoute` carries `name`/`symbol`/`code`/
  `rate` alongside `currencyId`, prefilling from the list's own row exactly like
  `CategoryEditorRoute`/`HouseholdMemberEditorRoute` — `CurrenciesRepository` has no single-row
  fetch either. Same deferral as 9.2–9.5's own notes: the emulator half of Verify needs the
  "Manage" drawer group, which is 9.7's scope, so only the host-test half ran here — all 14 tests
  pass (`CurrenciesViewModelTest`, `CurrencyEditorViewModelTest`), `detekt`/`ktlintCheck` pass
  project-wide, and `KoinGraphTest` resolves both new ViewModels after a clean
  `:androidApp:compileGplayDebugKotlin --rerun-tasks`. Data/DI wiring
  (`feature:currencies:ui`'s `build.gradle.kts`, `settings.gradle.kts`, root `kover { }`,
  `composeApp`'s `build.gradle.kts` and `Koin.kt`) landed now, same as 9.1–9.5 did at their own
  step. Nav wiring itself (`NavKeySerializers`, `DrawerDestination`, `RouteConfigProvider`, entry
  providers) stays 9.7's, per its own scope.

- [x] **9.7 — composeApp: the "Manage" drawer group**
  Wires all four new routes into `DrawerDestination`/`DRAWER_NAV_ITEMS`/`NavKeySerializers`
  (miss one and `NavKeySerializersTest` catches it, per `CLAUDE.md`'s nav3 rule) and adds a
  `DrawerItem.Group("Manage", [...])` entry to `DrawerItemsBuilder`, below Settings — the first
  real use of `DrawerItem.Group`, which `WallosDrawerWidget` already renders correctly (this
  milestone's preamble). New entry providers per screen (`nav/entries/`), added to `MainNavHost`.
  *Verify:* on the emulator — open the drawer, confirm a "Manage" header with all four screens
  listed under it, open each one.
  ·  *Ref:* plan §5.4, this milestone's preamble
  ·  *Note:* Each list route also picked up a `RouteConfigProvider` `FabConfig.Standard` (add) and
  `DrawerConfig.GesturesDisabled` for its editor route, mirroring `SubscriptionsRoute`/
  `SubscriptionEditorRoute` — 9.2–9.6 built the FAB-driving `RouteConfig` shape but left it
  unwired for these four, per their own notes. **`Icons.Filled.List` does not actually resolve**
  despite a `ListKt.class` existing in the `filled` package of the `material-icons-core` jar (a
  same-named internal file, not the public API) — confirms CLAUDE.md's existing claim that `List`
  lives only under `Icons.AutoMirrored.Filled.*`; caught by `compileGplayDebugKotlin`, not by
  inspection. With no `Category`/`Label`/money icon in the ~50-icon core set, the four "Manage"
  icons are picked for distinctness rather than semantic fit: Categories → `Star`, Household →
  `Person`, Payment methods → `ShoppingCart`, Currencies → `Refresh`. On-device verify covered all
  four screens (list renders, row tap opens a pre-filled editor, FAB opens a blank "New …" editor)
  and closed out the emulator half of 9.2–9.6's own deferred `Verify:` lines — no crashes, no
  layout issues; `adb logcat` confirmed no `FATAL EXCEPTION` across the whole session.

- [x] **9.8 — feature:profile: dto + domain + data — `get_user` + `set_budget`**
  New module. `UserDTO` (`WALLOS_API.md` §3.9 — `id`, `username`, `email`, `main_currency`,
  `budget`, `period_budget`, `budget_period_type`, `budget_period_anchor_date`, `totp_enabled`;
  skip `password`/`api_key`, always masked). `ProfileRepository.getUser()` /
  `setBudget(monthlyBudget, periodBudget, periodType, anchorDate)` — **always send all three period
  fields together** when touching any of them (this milestone's preamble / `WALLOS_API.md` §3.8):
  sending `period_budget` alone silently resets type and anchor to `monthly`/today.
  *Verify:* `./gradlew :feature:profile:data:testAndroidHostTest` — `get_user`'s happy path against
  `MockEngine` fixtures, and `set_budget` sending all three period fields whenever any one of them
  changes, never a partial set.
  ·  *Ref:* `WALLOS_API.md` §3.8–3.9, this milestone's preamble
  ·  *Note:* Not a new module — `feature:profile` (dto/domain/data) already existed from 10.1's
  `getUser()`-only cut, so this step widened its existing `UserDTO`/`User`/`ProfileRepository`
  rather than creating anything, and added `setBudget`. Confirmed live via `get_user.php`'s own PHP
  source and the `wallos` MCP that all five new fields are always present on this instance (no
  blank/missing case to guard), so none of the new `UserDTO` fields carry a default. `budget_period_type`
  is a new domain enum, `BudgetPeriodType` (`WEEKLY`/`FORTNIGHTLY`/`MONTHLY`), mirroring
  `BillingCycle`'s wire-value shape but defaulting unknown values to `MONTHLY` rather than going
  nullable — confirmed against `set_budget.php`'s own `sanitizeBudgetPeriodType`, which does the
  same fallback server-side, so a value this field can never actually be absent for doesn't need a
  null case client-side either. `budget_period_anchor_date` maps to `LocalDate` (matching
  `Subscription`'s own date fields), confirmed always a valid `YYYY-MM-DD` string — the DB column's
  default is a literal install-date string, never blank. `totp_enabled`'s wire `Int` maps to a
  domain `Boolean` via `== 1`, the same shape `PaymentMethodMapper` already uses for `enabled`.
  `ProfileApi.setBudget`/`ProfileRepositoryImpl.setBudget` follow `CurrenciesRepositoryImpl`'s
  precedent (9.1): the repository builds `FormParams` from primitives and hands it to a thin API
  method, rather than modeling a request DTO for a write with no response body worth decoding.
  Widening `User`'s constructor required fixing two other call sites that built one directly
  (`DashboardHomeUseCaseTest.user()`, `DashboardViewModelTest`'s fixture) and adding a `setBudget`
  override to `DashboardHomeUseCaseTest`'s `FakeProfileRepository` — both pre-existing from M10,
  neither this step's own scope otherwise.

- [x] **9.9 — feature:profile:ui: a Settings sub-screen for the budget**
  `ProfileRoute`/`ProfileScreen`, reached from a new `SettingsRow` on `SettingsScreen` (this
  milestone's preamble — not a drawer destination). Shows the current budget and period budget,
  editable, saved through 9.8's `setBudget`.
  *Verify:* on the emulator against the live instance — open Settings, tap into Profile, change the
  budget, save, and confirm `get_user`/the Dashboard's period-budget card reflect the new value.
  ·  *Ref:* plan §7.3, this milestone's preamble
  ·  *Note:* New module, `feature:profile:ui` — 9.8 built the data/domain layers only. No cache
  behind `get_user`/`set_budget` (mirrors the four catalog screens' own reasoning), so the shape is
  `CategoriesScreen`'s: `isLoading`/`isFailed`/`error`/`onRetryClick`, fired from the screen's own
  `LaunchedEffect(Unit)` on every fresh composition rather than an `init { load() }`.
  `budgetPeriodType`/`budgetPeriodAnchorDate` are **not** in `ProfileUiState` at all — this step's
  own text is "budget and period budget, editable" only, so `ProfileViewModel` holds whatever
  `getUser()` last reported in two private `var`s and resends them unchanged on save rather than
  building a second form for fields nobody asked to edit; `setBudget` requires all three period
  arguments together (9.8's own doc comment) or the server resets type/anchor to monthly/today.
  `budget`/`periodBudget` are `String`s in the UI state, the same shape `CurrencyEditorUiState.rate`
  uses for a decimal field — parsed to `Double` only on save, with `isFailed` reusing
  `CurrenciesUiState`'s `error.isNotEmpty() && <nothing loaded>` shape (`budget.isEmpty()` here,
  since there's no list to check). `settings_profile`/`settings_profile_description` ("Profile" /
  "Budget") were added as a third `SettingsRow`, between Interface and About — matching plan §7.3's
  "Profile + budget" ordering — which pushed `SettingsScreen`/`SettingsContent` from two callbacks
  to three; `viewModel` still moves last since `compose:parameter-order` exempts exactly one
  trailing defaulted param regardless of how many callbacks precede it. `ProfileRoute` is a
  `data object` wired into `NavKeySerializers`/`SettingsEntryProvider` the same way
  `InterfaceRoute`/`AboutRoute` are — reached from a `SettingsRow`, not a drawer destination, so
  `NavKeySerializersTest` doesn't cover it (it walks `DrawerDestination`) and only a process-death
  restore on this screen can. Verified on the emulator against the live instance: Settings → Profile
  showed the account's real `budget`/`period_budget` (both `0.0`), editing the monthly budget to
  `55.0` and tapping Save updated it server-side (confirmed via the `wallos` MCP's `get_user`), a
  fresh cold start showed the Dashboard's Monthly Budget card picking up `Budget: €55.00` / `100.00%
  used` / `€441.63 over budget`, and a second Settings → Profile visit loaded `55.0` back from
  `get_user`, not a stale value — no crash in `logcat` across the session. Reset the account's budget
  back to `0`/`0` afterward via the `wallos` MCP's `set_budget`, since this is the user's real
  account, not a throwaway row like the catalog screens' own test data.
  **M9 is done — Phase 5 (Management screens) is complete.**

## M11 — Show the connected server (not in plan §8's phase order)

Goal: `SettingsScreen` stops being silent about which Wallos instance it's talking to. **Done
when** the URL from `BaseUrlProvider.getBaseUrl()` is visible somewhere on the screen.

Filed to "To review" 2026-08-08, decomposed 2026-08-09 — picked over the other backlog items
because it is a single-seam read with no new storage and no open design question, unlike the
start-destination item (needs a scoping decision) or the scroll-lag item (needs profiling before
any fix can be written).

- [x] **11.1 — feature:settings ui: show the connected server**
  A row (or a line above Disconnect) reading the instance root the app is actually talking to.
  `BaseUrlProvider.getBaseUrl()` (`core:api`) is the existing read path — already a dependency of
  `feature:subscriptions:ui` and `feature:paymentmethods:ui` for logo/icon URLs — so this is a
  single-seam read, no new storage: `feature:settings:ui` gains
  `implementation(projects.core.api)`, `SettingsViewModel` takes a `BaseUrlProvider` alongside
  `ApiKeyStorage`, and `SettingsUiState` gains a `serverUrl: String` field read once at
  construction — no loading/error state, since this is a synchronous, cached read (plan-documented
  on `ServerUrlStorage`), not a network call. `getBaseUrl()`'s trailing `/` is load-bearing for
  Ktor's `defaultRequest` (see its kdoc) but not for display — trim it before showing the value, so
  the screen reads the URL the way the user typed it during login.
  *Verify:* `./gradlew :feature:settings:ui:testAndroidHostTest` with a fake `BaseUrlProvider`, and
  on device: open Settings, confirm the shown URL matches `docs/local-info.txt`'s instance
  (`http://10.0.2.2:8282` from the emulator), and that the `am kill` cycle comes back on Settings
  with the URL still shown.
  ·  *Ref:* `core/api/.../BaseUrlProvider.kt`, `core/storage/.../ServerUrlStorage.kt`
  **Note:** implemented as designed — `SettingsViewModel` takes `BaseUrlProvider` alongside
  `ApiKeyStorage`, `SettingsUiState` gained `serverUrl`, and `SettingsScreen` gained a `Server` row
  (new string `settings_server`) above the Disconnect section, styled like `AboutScreen`'s
  title/value fields. Verified: `testAndroidHostTest` green (new fake `FakeBaseUrlProvider` in the
  test file, same private-fake precedent as `FakeApiKeyStorage`), `KoinGraphTest` green (the new
  constructor param resolves — `core:api`'s `NetworkModule` was already `@ComponentScan`ned and
  already in `AppModule`'s includes; the only gap was `feature:settings:ui`'s own Gradle edge, fixed
  by adding `implementation(projects.core.api)`), full `allTests`/`detekt`/`ktlintCheck`/both-flavor
  assemble green, and on-device: Settings shows `http://10.0.2.2:8282` with no trailing slash.
  **The `am kill` cycle surfaced a relaunch-technique pitfall, not a nav bug**: relaunching with
  `monkey -c android.intent.category.LAUNCHER` after `am kill` added a *second* activity instance to
  the task on the very first call (`dumpsys activity activities`'s `sz=` 1→2, confirmed with `pidof`
  genuinely empty after the kill), which reopened on `DashboardRoute` — indistinguishable from a
  broken back-stack restore until checked. `am start -n <package>/MainActivity` gets Android's
  task-reuse treatment for this app's `standard`-launch-mode root activity instead (`Warning:
  Activity not started, its current task has been brought to the front`, `sz=` stays 1) and
  correctly restored Settings. Folded into `docs/EMULATOR_TESTING.md` and the shared
  `emulator-testing` skill (edited uncommitted, for the user to review) — the skill's own kill-cycle
  recipe used `monkey` for the relaunch step, which this session showed is unreliable even on the
  first call, not only on repeats as previously documented.
  **M11 is done.**

---

## M12 — User-configurable start destination (not in plan §8's phase order)

Goal: replace the hard-coded `START_DESTINATION` constant with a stored preference the user can
change from Settings, covering any of the 7 drawer sections — Dashboard, Subscriptions, Settings,
Categories, Household, Payment methods, Currencies — not just Dashboard-vs-Subscriptions, which
was the open scoping question this item carried in "To review". **Done when** picking a different
start screen and cold-starting the app (not just backgrounding it) opens there instead, with the
back stack still restoring correctly on process death.

Filed to "To review" 2026-08-08, decomposed 2026-08-09 — picked over the other backlog items by
the user. `feature:settings:ui`'s existing "Interface" sub-screen (9.9-era work:
`appearance/ThemeMode` + `ThemeStorage`) is the precedent to copy directly: same `core/storage`
interface+impl shape, same ui-only-feature ViewModel taking the storage straight (no repository),
same `RadioButton` `Column` picker. `core/storage` cannot depend on any route type —
`DrawerDestination` and the `NavKey` subclasses live in `composeApp` and feature `ui` modules,
layers above it — so the stored value is a new, self-contained enum in `core/storage` (seven cases
mirroring `DrawerDestination`'s names), and `composeApp` is the only place that maps it to an
actual `NavKey`.

- [x] **12.1 — core/storage: `StartDestination` + `StartDestinationStorage`**
  Mirrors `ThemeMode`/`ThemeStorage` exactly: a `StartDestination` enum persisted by a stable
  `value: String` (not ordinal) with seven cases — `Dashboard`, `Subscriptions`, `Settings`,
  `Categories`, `Household`, `PaymentMethods`, `Currencies` — `default()` returning `Dashboard`,
  and a `StartDestinationStorage` interface (`val startDestination: Flow<StartDestination>`,
  `suspend fun setStartDestination(value: StartDestination)`) + `@Single`-bound `internal` impl
  sharing the module's one DataStore file, its own `stringPreferencesKey` in a
  `private companion object`, falling back to `default()` on an unset or unrecognized value.
  *Verify:* `./gradlew :core:storage:testAndroidHostTest` — round-trips a stored value, and falls
  back to `Dashboard` for an unset or unrecognized key (mirrors `ThemeStorageImplTest`).
  ·  *Ref:* `core/storage/.../theme/ThemeMode.kt`, `ThemeStorage.kt`, `ThemeStorageImpl.kt`
  ·  *Note:* Implemented exactly as specced, package `core/storage/.../startdestination/`. Five
  tests in `StartDestinationStorageImplTest` (default, observed write, pre-stored value, unrecognised
  value, dedup on identical write), all green — `testAndroidHostTest` green, `detekt`/`ktlintCheck`
  green, and `:composeApp:compileGplayDebugKotlin --rerun-tasks` + `KoinGraphTest` both confirm the
  new `@Single` resolves with no further wiring: `StorageModule`'s existing `@ComponentScan` over
  `core.storage` picked it up automatically, same as `ThemeStorageImpl`.

- [x] **12.2 — feature:settings:ui: a "Startup screen" picker**
  A new sub-screen off Settings, mirroring `appearance/InterfaceScreen`'s shape exactly (Route/
  Screen/UiState/ViewModel, `RadioButton` + `Column` picker — seven rows now, still fixed-item so
  still a `Column`, not a `LazyColumn`), reached the way Interface/Profile/About are: a new
  `SettingsRow` on `SettingsScreen` (pushing it to a fourth callback — still exempt under
  `compose:parameter-order`'s single-trailing-function rule, `viewModel` stays last), its `Route`
  registered in `NavKeySerializers.kt` and wired into `SettingsEntryProvider.kt`. This is a
  Settings sub-screen, not a drawer destination, so it needs no `DrawerDestination`/
  `DRAWER_NAV_ITEMS` entry — 9.9's `ProfileRoute` is the precedent for this whole shape, not
  9.1–9.7's drawer-destination one. `StartDestinationViewModel` takes `StartDestinationStorage`
  directly, no repository.
  *Verify:* `./gradlew :feature:settings:ui:testAndroidHostTest` (fake `StartDestinationStorage`,
  mirrors `InterfaceViewModelTest`), and on the emulator: Settings → the new row → pick each of the
  seven options, confirm the radio selection persists across leaving and re-entering the screen.
  ·  *Ref:* `feature/settings/ui/.../appearance/` (whole package), `.../SettingsScreen.kt`,
  `composeApp/.../nav/entries/SettingsEntryProvider.kt`, `composeApp/.../nav/NavKeySerializers.kt`
  ·  *Note:* Implemented exactly as specced, package `feature/settings/ui/.../startdestination/`.
  The seven row labels reuse the existing drawer-title strings (`dashboard_title`,
  `subscriptions_title`, `settings_title`, `categories_title`, `household_title`,
  `payment_methods_title`, `currencies_title`) rather than adding new ones — `RString` is one
  shared resource set and this module already depended on `:strings`. Only two new strings added:
  `settings_start_destination` ("Startup screen", reused for both the `SettingsRow` title and the
  picker's `TopBarConfig` title, same as `settings_interface`) and
  `settings_start_destination_description` ("Which screen opens first"). Four tests in
  `StartDestinationViewModelTest`, all green — `testAndroidHostTest`, `detekt`/`ktlintCheck`,
  `:androidApp:compileGplayDebugKotlin --rerun-tasks` and `KoinGraphTest` all confirm the new
  `@KoinViewModel` resolves with no further wiring. On-device: all seven rows render with Dashboard
  selected by default, picking Subscriptions persists across leaving and re-entering the screen.

- [x] **12.3 — composeApp: read the stored preference into the real start destination**
  Replaces the `START_DESTINATION` constant (`nav/DrawerDestination.kt`) with a small
  `StartDestination -> NavKey` mapper (a `when` over the seven cases, returning each
  `DrawerDestination` entry's own `.route`) and threads the stored value from `WallosAppContent`
  down to `rememberNavigationState`'s `startKey` **the same way `themeMode` already is** — the
  load-bearing precedent already in this file (`WallosAppContent.kt`'s own comment on
  `rememberNavBackStack` consuming `startKey` only on the first composition): read
  `startDestinationStorage.startDestination.collectAsState(initial = StartDestination.default())`
  at the `WallosAppContent` call site, pass the mapped `NavKey` into
  `rememberMainAppState(startDestination = ...)`, and thread it into `rememberNavigationState`'s
  `startKey` param. Never gate `AuthenticatedMainScreen`/`rememberMainAppState()`'s composition on
  the read finishing — `isConnected` is the one deliberate exception to that rule in this file,
  `themeMode` is not, and this follows `themeMode`.
  *Verify:* on the emulator against the live instance — set the picker to Subscriptions, force-stop
  and cold-start the app (`emulator-testing` skill's `am start -n` recipe, not `monkey`: 11.1 found
  `monkey` unreliable even on the first relaunch), confirm it opens on Subscriptions; then navigate
  a few levels deep into a different section, `am kill` + cold-start again, and confirm the back
  stack still restores correctly (the regression this design exists to avoid).
  ·  *Ref:* `WallosAppContent.kt`'s `themeMode` handling, `MainAppState.kt`, `NavigationState.kt`
  ·  *Note:* Implemented exactly as specced. The mapper is `StartDestinationMapper`, an `object`
  with one `toNavKey(StartDestination): NavKey` function — the same shape as `RouteConfigProvider`
  (a `when` over a small closed set, in its own file in `nav/`), not a mapper *class* per the
  general mapper rule, since that rule targets testable DTO↔domain mapping and this is a same-layer
  lookup with an existing in-module precedent. `rememberMainAppState` gained a required
  `startDestination: NavKey` parameter (no default — the one caller always has a real value by the
  time it composes) in place of the removed `START_DESTINATION` constant.
  `NavKeySerializersTest`'s `the start destination is a drawer destination` test referenced that
  removed constant, so it became `every stored start destination maps to a drawer destination`,
  asserting the mapper's output for all seven `StartDestination` entries lands in
  `DRAWER_NAV_ITEMS` — same intent, adapted to the dynamic mapping. On-device: cold-starting after
  picking Subscriptions opened there; navigating two levels into Categories → Edit category, then
  `am kill` + `am start -n` (confirmed via `pidof` returning empty first, and the
  "brought to the front" task-reuse message on relaunch) restored the exact same screen, and Back
  correctly popped to the Categories list — the back stack survived process death.
  **M12 is done.**


## M13 — Android Baseline Profile (not in plan §8's phase order)

Goal: close the JIT-compilation-floor half of the FAB-open and subscriptions-list-scroll backlog
items (both below, in "To review") by shipping a Baseline Profile that AOT-compiles the code paths
`docs/issues/2026-08-09-fab-open-and-list-scroll-jank.md` traced as cold-JIT'd on first use — app
start, the first subscriptions-list scroll, and the first add-subscription editor open. **Done
when** a release build carries a generated profile covering those three journeys, and a fresh
Perfetto trace of the same cold-scroll/FAB-open recipe that doc used shows the JIT lock-contention
numbers it recorded (Finding 5: 119 `Lock contention on Jit code cache for mutator` slices, 4.57ms
wait, cold) measurably lower — reported honestly per that doc's own precedent (a0cf54d found the
Coil fix didn't move aggregate frame-jank at all; this milestone's own verification step needs the
same honesty rather than assuming the mechanism this doc named is automatically the fix).

Filed to "To review" 2026-08-07 (FAB) and 2026-08-08 (scroll), investigated together 2026-08-09
(`docs/issues/2026-08-09-fab-open-and-list-scroll-jank.md`, option 3 in its Options section,
explicitly deferred there pending this decomposition), decomposed 2026-08-10 at the user's request,
alongside fixing this file's own stale "To review" entries for the two backlog items (they still
read as uninvestigated before this session). Two steps, not one: the plugin/version wiring is a
real unknown (androidx.benchmark's own docs claim support only "up to AGP 9.0.0-alpha01" against
this project's real AGP 9.3.1/Gradle 9.6.1, and a newer 1.5.0 line is still in beta as of
2026-08-10 — neither claim is trustworthy without a real compile, so 13.1 spends itself confirming
which version actually builds before any CUJ beyond cold-start is added), and the two extra CUJs
plus the actual before/after trace comparison are real, independent scope on top of that.

- [x] **13.1 — a new `:benchmark` module: baseline profile pipeline, cold start only**
  A vanilla (non-KMP) macrobenchmark module — `com.android.test` + the `androidx.baselineprofile`
  producer plugin, per Android's own current template (no `wallosmobile.*` convention plugin
  applies here; this is a one-off module type this project has never had, so its `build.gradle.kts`
  is written directly, the same way `androidApp`'s isn't KMP either). New `gradle/libs.versions.toml`
  entries: `androidx-benchmark-macro-junit4`, `androidx-test-ext-junit`, `androidx-test-uiautomator`,
  and the `androidx.baselineprofile` Gradle plugin — start from androidx.benchmark 1.4.1 (latest
  documented stable) and only move to a 1.5.0 beta if 1.4.1 actually fails to configure against this
  project's AGP 9.3.1; record which one worked and why in this step's `Note:`, since both this
  session's web research and next session's re-reading of it are guesses until a real
  `./gradlew :benchmark:connectedCheck`-equivalent run proves one. `:androidApp` gets the
  `androidx.baselineprofile` *consumer* plugin applied directly in its own `build.gradle.kts` (not
  folded into `AndroidApplicationConventionPlugin`, since this project has exactly one app module)
  plus `baselineProfile(project(":benchmark"))`. The generator itself covers cold start only —
  `BaselineProfileRule.collect { startActivityAndWait() }` against the `gplay` flavor's debug-signed
  benchmark build type (`androidx.baselineprofile` auto-creates a non-debuggable, non-minified
  `benchmark` build type from `release` for this) — proving the whole pipeline end to end before
  13.2 adds the two CUJs that are the actual point of this milestone. Touches
  `gradle/libs.versions.toml` and `settings.gradle.kts` → needs a `Gate-change:` line for the
  version-catalog edit (`.github/scripts/check-guardrails.sh HEAD~1..HEAD`).
  *Verify:* `./gradlew :androidApp:generateGplayReleaseBaselineProfile` completes and writes
  `androidApp/src/gplayRelease/generated/baselineProfiles/baseline-prof.txt` (or wherever the
  plugin actually places it — confirm the real path rather than assuming the docs' generic one);
  `unzip -l` the assembled `gplayRelease` APK/bundle and confirm it embeds a compiled
  `assets/dexopt/baseline.prof` (or equivalent — again, confirm the real artifact name against what
  actually gets produced, not the doc's claim). `detekt`/`ktlintCheck`/`allTests` stay green — this
  module adds no KMP source, so it shouldn't touch `commonMain` gates at all, and `:testing`-style
  fakes don't apply to a macrobenchmark test class (it's instrumentation, same category as
  `core:storage`'s `connectedAndroidDeviceTest`).
  ·  *Ref:* `docs/issues/2026-08-09-fab-open-and-list-scroll-jank.md` (root-cause finding this
  milestone is fixing), `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt`
  (the release build type this profile has to survive — minify + shrink resources both on),
  `emulator-testing` skill (device/AVD facts — profile generation needs a real or Gradle-managed
  device, not just `assemble`).
  ·  *Note:* androidx.benchmark **1.4.1 (the documented stable) failed**: `androidx.baselineprofile`
  applied but errored `Module :androidApp is not a supported android module` — its own detection
  logic predates AGP 9 entirely (the doc claim of "up to AGP 9.0.0-alpha01" was accurate, not
  conservative). **1.5.0-beta01 configures and runs cleanly against this project's real AGP
  9.3.1/Gradle 9.6.1**; recorded here since both versions were only guesses until this session's
  actual `:benchmark:tasks`/`generateGplayReleaseBaselineProfile` runs proved one. Two more real,
  non-obvious gaps the plan text didn't anticipate: (1) `com.android.test` and
  `androidx.baselineprofile` both needed `alias(...) apply false` added to the **root**
  `build.gradle.kts`'s existing "avoid the plugins being loaded multiple times" block — the same
  reason `com.android.application` is already there — or applying either from `:benchmark`'s own
  `plugins {}` block errored `already on the classpath with an unknown version` (build-logic's own
  AGP dependency puts every AGP plugin class on the shared classloader without a resolvable
  marker version the first time a subproject's `plugins {}` block asks for one with an explicit
  version). (2) `:benchmark` needed its own **"STORE" flavor dimension** (`gplay`/`fdroid`,
  mirroring `AppFlavors`/`FlavorDimensions` by name rather than importing them — a plain
  subproject script can't resolve a `com.grappim.wallosmobile.buildlogic` import the way
  `build-logic`'s own compiled Kotlin can) — without it, `generateGplayReleaseBaselineProfile`
  failed with a Gradle variant-attribute-ambiguity error between `:androidApp`'s two flavors'
  `RuntimeElements`. The step's guessed task name and output path were both right:
  `generateGplayReleaseBaselineProfile` writes
  `androidApp/src/gplayRelease/generated/baselineProfiles/baseline-prof.txt` (~25k rules,
  committed as source, not gitignored — that's the whole point of the plugin copying it there),
  and `unzip -l` on the assembled `gplayRelease` APK confirms `assets/dexopt/baseline.prof` +
  `.profm` embedded. `Quality.kt`'s `configureLinting()` also isn't callable from `:benchmark`'s
  plain script the same way (`import com.grappim.wallosmobile.buildlogic.*` doesn't resolve
  there), so detekt/ktlint are wired by hand in `benchmark/build.gradle.kts` instead — including
  the `composeRules-ktlint`/`composeRules-detekt` dependencies, needed by every module regardless
  of Compose content because the shared `config/detekt/detekt.yml`'s `Compose:` section is invalid
  detekt config without the plugin present (confirmed live: `:benchmark:detekt` failed
  `Property 'Compose' is misspelled or does not exist` until added). Full gate run green:
  `detekt ktlintCheck allTests` and `:androidApp:assembleGplayDebug :androidApp:assembleFdroidDebug`
  all pass. The plugin's own "no startup profile rules generated" warning (needs
  `includeInStartupProfile = true` on a `collect` call) is left as-is — out of this step's cold-
  start-only scope, not a failure. `Gate-change:` needed in the commit for `gradle/libs.versions.toml`.

- [x] **13.2 — extend the generator to the two CUJs that are the actual point, and measure**
  Adds two more `@Test` journeys to `13.1`'s generator class: a subscriptions-list fling (matching
  the 2026-08-09 doc's own swipe recipe, `input swipe 540 2000 540 300 150` reproduced via
  `UiDevice`/`device.swipe` inside the `profileBlock`, not `adb shell input`) and a first open of the
  add-subscription editor (FAB tap → editor screen drawn) — needing a logged-in, seeded app state
  before the journey starts, the same DataStore-planting recipe the `emulator-testing` skill already
  documents for other on-device verifies, since `BaselineProfileRule` starts from a cold, logged-out
  process otherwise. Regenerate, rebuild `gplayRelease`, then re-run this doc's exact Perfetto
  cold-scroll/FAB-open recipe (`emulator-testing` skill's Step 4b) against that release build and
  compare Finding 5's numbers (JIT lock-contention slice count/wait time) and Finding 3/4's frame
  numbers (worst-frame ms, jank-frame percentage) to the 2026-08-09 baseline. Report the result in
  this step's `Note:` the way `a0cf54d` did — a real drop, a partial one, or none — rather than
  assuming the mechanism the doc named is automatically fixed by the profile existing.
  *Verify:* the trace comparison above, captured and compared, is the actual verify — not just a
  green build. `detekt`/`ktlintCheck` still pass on the new generator code.
  ·  *Ref:* `docs/issues/2026-08-09-fab-open-and-list-scroll-jank.md` Findings 3–5 (the baseline
  numbers this step compares against) and its own Step 4b Perfetto recipe in the `emulator-testing`
  skill.
  ·  *Note:* The step's own "same DataStore-planting recipe" plan turned out not to work:
  `ApiKeyStorageImpl` encrypts the stored key with `KeystoreSecretCipher` (AES/GCM, Android
  Keystore-backed), so a raw plant can produce a value the app's own `decrypt()` never accepts
  — it just reads as "no key stored" (the catch arms are deliberately lenient, per that file's own
  comment). Seeding had to be a real login instead: install the `gplayNonMinifiedRelease` variant
  by hand and drive `LoginScreen` against the local instance. A second, non-obvious gap: **the
  target app is uninstalled at the end of every `connectedGplayNonMinifiedReleaseAndroidTest` run,
  pass or fail** — confirmed live (`adb shell pm list packages` lost `com.grappim.wallosmobile`
  between two back-to-back `generateGplayReleaseBaselineProfile` invocations) — so the seeded login
  survives every `@Test` *within* one invocation (no `pm clear` between them or between
  `BaselineProfileRule` iterations — only `killProcess()`) but not *across* invocations; re-login
  is needed before every fresh run of the generator, not just once. `openSubscriptionsList()` also
  needed a `device.wait(Until.hasObject(By.desc("Menu")), …)` before its first click —
  `startActivityAndWait()` only waits for the window to be idle, not for `WallosAppContent`'s
  `isConnected` flow to resolve past its first-frame blank `Box`, and skipping that wait made
  `addSubscriptionEditorOpen` flaky. With both fixed, all three `@Test`s passed and the profile
  grew 24,943 → 35,383 rules (+10,648 added, only 208 removed), confirmed covering
  `feature/subscriptions/ui/editor/*` and the list/card classes, not just cold start.

  **Measurement — a real, honest result, not the hoped-for one.** Two fresh cold-scroll Perfetto
  traces (`emulator-testing` Step 4b, this doc's exact `input swipe 540 2000 540 300 150` ×3
  recipe) against the signed `gplayRelease` build with the new profile embedded, each on a just
  re-seeded, never-before-scrolled login:

  | Metric | 2026-08-09 baseline (cold) | This session, run 1 | This session, run 2 |
  |---|---|---|---|
  | `Lock contention on Jit code cache for mutator` | 119 slices, 4.57ms | **0 slices** | **0 slices** |
  | Broad JIT activity (`Compiling optimized`/`baseline`/`JIT compiling`) | 1802 slices | 156 slices | 130 slices |
  | Jank-frame % (`actual_frame_timeline_slice`) | 81% (136/168) | 93% (54/58) | 88% (51/58) |
  | Worst frame | 69.74ms | 101.9ms | 107.3ms |

  The mechanism this milestone targeted is gone, cleanly and reproducibly: zero JIT-code-cache
  lock contention across both runs (was 119 slices), and broad JIT activity down ~91–93%. That
  part is a real, confirmed win. **The aggregate frame-jank numbers did not improve, and read
  worse than the pre-profile cold baseline in both runs** — this is not the Coil-fix precedent's
  "flat" result (`a0cf54d`), it's a regression on this specific metric, on this AVD. Two honest
  caveats, neither of which resolves the question: (1) the total frame count differs sharply
  between conditions (168 baseline vs. 58 here), so "81% vs. 93%" is not a like-for-like window —
  the two swipe sequences did not produce the same amount of on-screen motion, for reasons this
  session did not chase down; (2) this AVD's `-gpu swiftshader_indirect` software rendering and
  `Prediction Error` scheduling noise were already flagged by the 2026-08-09 doc as dominant,
  AVD-specific confounds *before* this change, and this session has no path to real hardware to
  separate "the profile made frame pacing worse" from "run-to-run noise on a software-rendered
  emulator, now dominated by a different, non-JIT bottleneck since the JIT one is gone." A FAB-open
  trace (fresh process, FAB tap → editor drawn) shows the same JIT pattern (2 lock-contention
  slices at 0.004ms total, 138 broad-JIT slices) but this doc never captured a comparable FAB
  baseline with this same methodology (4.4's "121.9ms first-frame cost" used a different technique)
  so there is nothing to diff it against — reported as a data point only, not a comparison.
  **Net:** M13's own goal — AOT-compile the three cold-JIT'd journeys — is done and verified at the
  mechanism level; its stronger claim ("and the doc's jank numbers go down") is **not** confirmed by
  this AVD, and the honest record of that non-confirmation is this Note, not a rewritten goal.

  **Addendum, same session, prompted by the user re-asking about the FAB specifically:** the
  measurement above used the scroll trace's methodology (`actual_frame_timeline_slice` aggregate
  jank), which never directly timed the one thing the original FAB complaint was actually about —
  tap-to-screen-visible latency for FAB→editor versus row-tap→detail. A direct `dumpsys gfxinfo
  <pkg> reset` / `framestats` measurement of exactly that (`HandleInputStart` of the tap's own frame
  to `FrameCompleted` of the last settled frame, discarding the ring-buffer-stale trailing row per
  the `emulator-testing` skill's own caveat), each from a freshly `am force-stop`'d cold process,
  two runs apiece: FAB 245ms/250ms, Detail 242ms/250ms — indistinguishable. **This is a materially
  more direct answer to the FAB item's own complaint than the scroll-based numbers above, and it
  reads as fixed**, not merely mechanism-level. Still only two runs on the same software-rendered
  AVD, so treat as encouraging rather than conclusive — but it meaningfully narrows the "not
  confirmed" verdict above to the scroll-jank metric specifically, not to the FAB-open complaint
  that started this milestone.

## M14 — CI tooling: Codacy, Codecov, Renovate (plan §3.8)

Goal: this repo's static analysis (Codacy), coverage reporting (Codecov) and dependency updates
(Renovate) are wired up the way `TaigaMobileNova` already has them, ported and adjusted for a
single-branch (`master`), single-workflow repo. **Done when** all three config files exist and a
push shows each app act on it — a Codacy analysis run, a Codecov coverage check, and (whenever
Renovate next runs its schedule) a dependency PR against `master`.

Planned 2026-08-10, not decomposed straight from "To review" like M11/M12: filed directly by the
user. Full rationale for each adjustment from Taiga's own files: plan §3.8.

- [x] **14.1 — root: add `.codacy.yml` and `renovate.json`**
  Both are GitHub-App-driven — the app itself acts on pushes/PRs once the repo is enabled (already
  done, per the user) and a config file exists; neither needs a workflow file, confirmed against
  Taiga's `.github/` having no Codacy or Renovate step anywhere.
  Port `.codacy.yml` from Taiga verbatim, minus its `PRIVACY_POLICY_GPLAY.md` exclude entry (no
  equivalent file here yet):
  ```yaml
  exclude_paths:
    - ".github/**"
    - "docs/**"
    - ".claude/**"
  ```
  Port `renovate.json` too, but **drop Taiga's `baseBranchPatterns: ["dev"]`** — WallosMobile's
  default branch already is `master`, so `config:recommended` targets it with no override:
  ```json
  {
      "$schema": "https://docs.renovatebot.com/renovate-schema.json",
      "extends": ["config:recommended"],
      "prHourlyLimit": 3,
      "osvVulnerabilityAlerts": true
  }
  ```
  *Verify:* both files parse (`python3 -c "import yaml;yaml.safe_load(open('.codacy.yml'))"`,
  `python3 -c "import json;json.load(open('renovate.json'))"`); since this is app-driven, not
  Gradle-driven, the real check is a push followed by a look at the Codacy and Renovate dashboards
  for this repo. Neither file is under a tripwire path (`.github/`, `build-logic/`,
  `config/detekt/`, `.editorconfig`, `gradle/libs.versions.toml`) — no `Gate-change:` line.
  *Note:* the system `python3` has no `yaml` module (`pip` refuses a bare install on this
  machine), so the `.codacy.yml` parse check ran from a scratch venv per `CLAUDE.md`'s Python
  rule; `renovate.json`'s `json` check needed no venv, stdlib only. Default branch confirmed
  `master` via `gh repo view --json defaultBranchRef`.

- [x] **14.2 — .github: wire Kover into Codecov**
  **First check `CODECOV_TOKEN` is actually set** (`gh secret list`) — as of 2026-08-10 this repo
  has no secrets at all despite the user's belief one was already added; add it (from the Codecov
  dashboard's repo settings) before the `Verify:` line below can pass.
  Add `codecov.yml` at root, ported from Taiga's with `branches: [master]` only (Taiga has
  `[master, dev]`) and the same `ignore:` path list (test/mock/fake/generated patterns) — a
  redundant-but-harmless second net over the root `build.gradle.kts` `kover { reports { filters {
  excludes … } } }` block that already keeps those out of the XML report. Coverage-status
  thresholds ported unchanged from Taiga (project: `auto` target, 1% threshold; patch: `80%`
  target, 5% threshold) — decided with the user 2026-08-10; see plan §3.8 for why this doesn't
  reopen the separate, already-declined Kover/Gradle floor decision.
  Extend `.github/workflows/ci.yml`'s single job with two steps after "Run detekt and ktlint":
  `./gradlew koverXmlReport`, then `codecov/codecov-action@v7` uploading
  `./build/reports/kover/report.xml` with `CODECOV_TOKEN`. **No standalone test run needed
  first** — unlike Taiga's `code_analysis.yml`, which runs `jvmTest` explicitly because its Kover
  aggregation skips modules outside the root `kover {}` block; this job already runs `allTests`
  (every module) earlier in the same job, so `koverXmlReport` has nothing left to execute.
  *Verify:* `./gradlew koverXmlReport` locally produces `build/reports/kover/report.xml`; after
  push, the Actions run's Codecov step succeeds and the commit shows a Codecov check.
  **Touches `.github/workflows/ci.yml` — needs a `Gate-change:` line**: plan §3.5 called the
  Codecov upload a *deliberate* omission for lack of a token; this step reverses that.
  *Note:* `gh secret list` confirmed the repo had no secrets at session start; the user added
  `CODECOV_TOKEN` directly through the GitHub UI (not `gh secret set`) mid-session, confirmed
  present by re-running `gh secret list`. The `codecov-action@v7` step's `with:` block otherwise
  mirrors Taiga's (`disable_search`, `flags: unittests`, `name: codecov-umbrella`,
  `fail_ci_if_error: false`, `verbose: true`) — Taiga's additional `override_branch`/
  `override_commit` lines weren't carried over; the step's own text didn't call for them and
  nothing here needs them since this workflow already scopes to `master` and PRs against it.

## M15 — Branch model (dev/master) + release automation, ported from TaigaMobileNova (plan §3.9)

Goal: `dev` is the default branch and receives ordinary work exactly the way `master` does today
(direct pushes, one step per commit — no PR required yet); `master` moves only on a release, via
three GitHub Actions workflows ported from Taiga (`release-prepare` → PR → `release-finalize` →
tag → `release`). **Done when** all four steps below are ticked; a *real* release is not part of
this milestone's own `Done when` — 15.4's own text says why.

Planned 2026-08-10, filed directly by the user, same as M14: the repo is about to go public
(user's decision, made this session), which unblocks the one thing that made a straight Taiga port
impossible before now — branch protection needs a public repo or a paid GitHub tier, confirmed via
a 403 on this repo back in M14 (plan §3.8). Full design and the two corrections this session's
research made to a straight port — Taiga's protection actually sits on `dev`, not `master`, and
this repo has no `signingConfigs` at all yet — live in plan §3.9. **Branch protection itself is
explicitly out of scope here** — the user's own instruction was to write it down as a follow-up
once the repo is public and nears its first release, not to turn it on now. Until then `dev`
behaves exactly like `master` does today.

- [x] **15.1 — Create `dev`, make it default, retarget `ci.yml` + `guardrails.yml`, update the docs**
  Branch `dev` off current `master` tip, push it, then flip the repo's default branch to `dev`
  (`gh repo edit --default-branch dev` or the GitHub UI). Retarget both `ci.yml` and
  `guardrails.yml`: `branches: [master]` → `branches: [dev, master]` on their `push` and
  `pull_request` triggers — mirroring Taiga's `code_analysis.yml` shape (runs on both branches),
  not `build.yml`'s dev-only PR trigger, since WallosMobile has one combined CI job rather than
  Taiga's two-workflow split, and `master` still needs both gates for 15.2's eventual release PR.
  **`.codacy.yml` and `renovate.json` need no edits** — both act on whichever branch GitHub reports
  as default, so flipping the default repoints them with no file change; only the *prose*
  describing that (plan §3.8) needs updating, not the config. Update `CLAUDE.md`'s "How work
  happens here" step 4 ("straight to `master`") to "straight to `dev`", and add a short paragraph
  stating `master` only moves via 15.2–15.4's automation, with branch protection deliberately not
  turned on yet. Update `README.md`'s CI line and any other `master`-as-the-branch text.
  *Verify:* `gh repo view --json defaultBranchRef` reports `dev`; a trivial push to `dev` shows
  both the CI and Guardrails checks running against it (`gh run list --branch dev`); grep confirms
  no remaining "straight to `master`" line in `CLAUDE.md`/`README.md`. **Touches `.github/` — needs
  a `Gate-change:` line.**
  Note: `codecov.yml` needed the same `dev` addition as the two workflows — its own
  `branches: [master]` allowlist isn't covered by the step's "`.codacy.yml`/`renovate.json` need no
  edits" claim (those two follow whichever branch GitHub reports as default; `codecov.yml`'s
  `branches` key is a separate, explicit list). Confirmed against Taiga's own `codecov.yml`, which
  already carries `[master, dev]`.

- [x] **15.2 — `release-prepare.yml` + `release-finalize.yml`: branch, version-bump and tag mechanics**
  Port both from Taiga near-verbatim: `release-prepare` (manual `workflow_dispatch`, takes
  version + version code) merges `dev` into `master` locally, cuts a `release/vX` branch, bumps
  `gradle/libs.versions.toml`'s `version-code`/`version-name`, opens a PR `release/vX` → `master`.
  `release-finalize` fires on that PR closing merged, tags `vX`, and back-merges `master` into
  `dev`. **Drop Taiga's F-Droid/Play changelog-stub steps** — `fastlane/metadata/android/en-US/
  changelogs/` and `playstore/changelogs/` don't exist in this repo, unlike Taiga's already-
  published one; note in the release PR body that changelog content is still manual, and leave the
  real scaffolding to whichever later milestone actually prepares the store listings. No
  `RELEASE_PAT` needed yet — plain `GITHUB_TOKEN` (`contents: write`, `pull-requests: write`)
  suffices while nothing is branch-protected; note in the workflow that a real admin PAT is needed
  once 15.1's deferred protection actually lands.
  *Verify:* both workflow YAMLs pass GitHub's own syntax validation (a push with no `on:` errors in
  the Actions tab); a full dry run is deliberately not part of this step — see 15.4. **Touches
  `.github/` — needs a `Gate-change:` line.**
  Note: this repo's two workflows use 2-space YAML indentation throughout (`ci.yml`,
  `guardrails.yml`), not Taiga's 4-space/tab style — both new files follow the local convention
  rather than the source's. Validated locally with `js-yaml` (already present system-wide, no venv
  needed) before pushing, since neither `actionlint` nor `PyYAML` is installed; the Actions tab is
  still the real check per this step's own `Verify:` line.

- [x] **15.3 — Release signing: `signingConfigs` + keystore secrets**
  `AndroidApplicationConventionPlugin.kt`'s `release` build type currently has **no
  `signingConfigs` at all** — confirmed via a grep across `build-logic/` and `androidApp/
  build.gradle.kts`, neither flavor can produce a signed release build today. Add a
  `signingConfigs` block reading keystore path/passwords from env (matching Taiga's
  `TAIGA_KEYSTORE_R`/`_D` + password secrets shape), wired per flavor (`gplay`, `fdroid`).
  **Generating the actual production keystore is the user's own call, not a session's** —
  `keytool -genkeypair` run locally, the resulting `.jks` plus its four passwords added as GitHub
  secrets, never committed. This step's job is the Gradle wiring and documenting the recipe, not
  fabricating a signing identity. No Google Services/Firebase restore step is needed — the
  `google-services`/`firebase-*` catalog entries are declared in `gradle/libs.versions.toml` but
  unapplied anywhere in this repo.
  *Verify:* `./gradlew :androidApp:assembleGplayRelease :androidApp:assembleFdroidRelease` builds
  and signs both, using a locally-provided test keystore. **Touches `build-logic/` — needs a
  `Gate-change:` line.**
  Note: generalized Taiga's `_R`/`_D` (release/debug) env-var suffix into a per-flavor one instead
  (`_GPLAY`/`_FDROID`) for the two release configs. `signingConfig` is assigned on each
  `ApplicationProductFlavor` (in `configureFlavors`'s `flavorConfigurationBlock`), not on the
  `release` build type itself: AGP's `debug` build type already carries its own non-null default
  signing config, which wins over a flavor-level one, so `debug` stays on the auto debug keystore
  while `release` (which sets none) picks up the flavor's. Env vars: `WALLOS_STORE_PASS_<FLAVOR>`,
  `WALLOS_ALIAS_<FLAVOR>`, `WALLOS_KEY_PASS_<FLAVOR>`.
  **Grew mid-step**: the user wants F-Droid's debug channel installable across CI builds too (a
  device upgrading in place needs the same signing identity every time, which AGP's own per-machine
  debug key can't give it), so a third config, `fdroidDebug`, was added with its own
  `WALLOS_*_FDROID_DEBUG` env trio. A flavor-level `signingConfig` would have applied to *both*
  `fdroidRelease` and the fdroid debug variant, so this one is wired through the Variant API instead
  — `androidComponents.onVariants(selector().withBuildType("debug").withFlavor("STORE" to "fdroid"))
  { variant.signingConfig.setConfig(signingConfigs.getByName("fdroidDebug")) }` — the only way to
  target one exact (flavor, build type) combination without the flavor-level DSL's bleed-through.
  Keystore files are root-relative and gitignored, but **named by the user, not the session**:
  `wallos_mobile_gplay.jks`, `wallos_mobile_fdroid.jks`, `wallos_mobile_fdroid_debug.jks` — the user
  had already generated the three real keystores through Android Studio before this step touched
  the code, so the naming in `AndroidApplicationConventionPlugin.kt` was written to match what
  existed rather than the other way around. `build-logic` has no `detekt`/`ktlintCheck` coverage
  (an included build, confirmed via `./gradlew -p build-logic tasks --all`, no matching tasks) —
  nothing to run there beyond `compileKotlin`. Verified against the **real** keystores (the user
  set the real passwords as local env vars): `assembleGplayRelease`, `assembleFdroidRelease` and
  `assembleFdroidDebug` each produced an APK signed with a distinct cert, `assembleGplayDebug`
  stayed on AGP's `CN=Android Debug` — all checked via `apksigner verify --print-certs`. GitHub
  secrets already exist too (`WALLOS_STORE_PASS_*`/`WALLOS_ALIAS_*`/`WALLOS_KEY_PASS_*` per config,
  plus `WALLOS_FILE_<FLAVOR>` holding each keystore's base64 content for 15.4 to decode) — 15.4
  does not need to treat them as missing.

- [x] **15.4 — `release.yml`: build and publish signed artifacts**
  Port Taiga's tag-triggered (+ `workflow_dispatch`) release workflow, scoped down: Android only
  (`assembleGplayRelease`/`assembleFdroidRelease` + `bundleGplayRelease`), no desktop `deb`/`rpm`
  packaging (no desktop target exists here), no `google-services.json` restore (per 15.3). Uploads
  to a GitHub Release via `softprops/action-gh-release@v3`, same as Taiga. **First check 15.3's
  keystore secrets actually exist** (`gh secret list`) before this step's `Verify:` line can pass —
  same shape as 14.2's `CODECOV_TOKEN` callout.
  *Verify:* `gh workflow run release.yml -f tag=v0.0.0-test` (or a real first tag, if the app is
  ready by then) produces a signed APK/AAB and a GitHub Release. **This milestone's own `Done
  when` does not require an actual release to exist** — only that the mechanism is provably wired;
  a real first release is a product decision for whenever the app is actually ready to ship, not a
  gate on closing M15. **Touches `.github/` — needs a `Gate-change:` line.**
  Note: `gh secret list` confirmed all six release secrets from 15.3 exist
  (`WALLOS_STORE_PASS_GPLAY`/`WALLOS_ALIAS_GPLAY`/`WALLOS_KEY_PASS_GPLAY`, same trio for
  `FDROID`) plus `WALLOS_FILE_GPLAY`/`WALLOS_FILE_FDROID` (base64 keystores this workflow decodes
  into the root-relative `.jks` paths `AndroidApplicationConventionPlugin.kt` reads). Scoped to
  exactly the step's own task list — `assembleGplayRelease`, `assembleFdroidRelease`,
  `bundleGplayRelease` — so only the two release keystores are restored; `WALLOS_FILE_FDROID_DEBUG`
  and its trio exist (15.3) but are unused here, since `fdroidDebug` signing was scoped to CI
  builds generally, not this tag-triggered release path. No composite setup action exists in this
  repo (unlike Taiga's `android-setup-composite-action`), so the Java/Gradle/Android SDK setup
  steps are copied inline from `ci.yml` instead. **The live dry run itself
  (`gh workflow run release.yml -f tag=v0.0.0-test`) was deliberately deferred, by the user's own
  choice** — it pushes a real tag and publishes a visible GitHub Release on the now-public repo, and
  the user asked to skip that for this session, consistent with M15's own "Done when" not requiring
  an actual release. What *did* run: `js-yaml` validated the file's syntax locally, and after
  pushing, `gh workflow list` reported it `active` (not `disabled_yaml_error`) — the same two-tier
  check 15.2 used. The end-to-end dispatch (does the build actually produce a signed APK/AAB and
  publish a release) is still unverified and is the user's own to trigger whenever they choose.

## M16 — Firebase Crashlytics + Play In-App Update, ported from TaigaMobileNova, `gplay` only (plan §3.10)

Goal: the `gplay` flavor gets crash reporting (with a user-facing opt-out) and a Play In-App
Update prompt, exactly as TaigaMobileNova has both; `fdroid` gets neither, structurally — not just
undeployed, but the Firebase Gradle plugins never applied to it at all (Taiga's own
`docs/build/fdroid-reproducibility.md` documents why a dependency-only gate isn't enough: applying
the Crashlytics plugin unconditionally shifts resource IDs on *every* flavor, breaking F-Droid's
reproducible build even when fdroid never uses the dependency). **Done when** all five steps below
are ticked. **Full design, all four open questions from planning answered, lives in plan §3.10** —
read it before starting any step here, the way M15's steps pointed at §3.9.

Planned 2026-08-10, filed directly by the user right after M15 closed. **The Firebase project
existed only as a future step at planning time — the user created it same-day, during 16.2**:
`google-services.json` is now real, registers both `com.grappim.wallosmobile` (release) and
`com.grappim.wallosmobile.debug` (the gplay flavor has no `applicationIdSuffix`, so the debug
build type alone produces that id — a Firebase project that only registered the release app fails
`processGplayDebugGoogleServices` with "No matching client found" until the debug one is added
too), lives at `androidApp/src/gplay/google-services.json` (gitignored, real machine only), and
`WALLOS_GOOGLE_SERVICES_GPLAY` is set as a GitHub secret. `assembleGplayDebug -PgplayBuild` builds
clean locally against it. **Crashlytics *delivery* is now verified, not just structural** — 16.3's
`CrashReporterImpl` landed and an on-device install showed `FirebaseCrashlytics: Initializing
Firebase Crashlytics` firing on cold start with no crash (16.3's own `Note:`); 16.4/16.5's own
`Verify:` lines should expect the real file to be present rather than assuming it's still missing.
And **since 16.3 landed, a plain
`./gradlew :androidApp:installGplayDebug` (no `-PgplayBuild`, no real `google-services.json`)
starts crashing on cold start** — `CrashReporterImpl` for the gplay flavor is chosen by *flavor*,
not by the `gplayBuild` property, so it's always compiled into a gplay build and always touches
`Firebase.crashlytics` the moment Koin constructs it in `WallosApp.onCreate()`; only the Gradle
*plugin* (which processes `google-services.json` into the resources `FirebaseApp` auto-init
reads) is gated by the property. This is the same latent risk Taiga accepted — its own CI always
passes `-PgplayBuild` for every gplay task, so the crash path is never actually hit there either.
`CLAUDE.md`'s "Build commands" needs a line about this once 16.1 lands, so a future session
building gplay locally doesn't lose time to it.

- [x] **16.1 — Gradle wiring: `gplayBuild` property, conditional Firebase plugins, gplay-scoped dependencies**
  `androidApp/build.gradle.kts` gains `alias(libs.plugins.google.services) apply false` and
  `alias(libs.plugins.firebase.crashlytics) apply false` in `plugins {}`, then, imperatively after
  the block, `if (project.hasProperty("gplayBuild")) { apply(plugin =
  libs.plugins.google.services.get().pluginId); apply(plugin =
  libs.plugins.firebase.crashlytics.get().pluginId) }` — Taiga's exact property name and
  mechanism, ported as-is (plan §3.10 has the reproducibility incident that makes this the
  non-negotiable part, not a simplification to skip). Dependencies via the `gplayImplementation`
  configuration AGP already generates from the `gplay` flavor: `platform(libs.firebase.bom)`,
  `libs.firebase.crashlytics`, `libs.google.inapp.update.ktx`. `gradle/libs.versions.toml` needs
  no edit — every version/library/plugin entry this step uses already exists there (confirmed
  15.3/15.4 sessions left them in place unused). `.gitignore` gains a line for
  `androidApp/src/gplay/google-services.json` (the real file, never committed — matching the
  existing `*.jks` pattern's reasoning, not its glob shape: this is one exact path, not a
  extension-wide pattern). Commit a placeholder `androidApp/src/fdroid/google-services.json` with
  fake `PLACEHOLDER_NOT_A_REAL_KEY` values (Taiga's exact file), purely defensive insurance for
  that flavor's source set.
  *Verify:* `./gradlew :androidApp:assembleFdroidDebug :androidApp:assembleFdroidRelease` build
  clean (plugins structurally absent from fdroid, provable by their absence changing nothing).
  `./gradlew :androidApp:assembleGplayDebug -PgplayBuild` fails with Gradle's own "File
  google-services.json is missing" error — a **real** green signal even without the file yet: it
  proves the property actually reaches the conditional `apply(plugin = ...)` calls rather than a
  typo'd property name silently no-op'ing. No `Gate-change:` line — `androidApp/build.gradle.kts`
  and `.gitignore` aren't tripwire paths (only `build-logic/`, not app modules).

- [x] **16.2 — CI: split flavor builds, restore `google-services.json`**
  `ci.yml`'s single `./gradlew :androidApp:assembleDebug` (both flavors in one invocation) can't
  pass `-PgplayBuild` to only one — split into `assembleFdroidDebug` (no property) and a
  `Restore google-services.json` step (`echo $ENCODED | base64 -d >
  androidApp/src/gplay/google-services.json`, new secret — name it
  `WALLOS_GOOGLE_SERVICES_GPLAY`, matching 15.3/15.4's `WALLOS_*` convention rather than Taiga's
  `GOOGLE_SERVICES_GPLAY`) followed by `assembleGplayDebug -PgplayBuild`. This also fixes a real
  gap 15.4 left standing (not a regression this step introduces): `release.yml`'s existing
  `assembleGplayRelease`/`bundleGplayRelease` step runs with no `-PgplayBuild` and no restore
  step today, so as shipped it would silently produce a release APK/AAB with neither feature the
  moment 16.1 lands — add both there too.
  *Verify:* `gh secret set WALLOS_GOOGLE_SERVICES_GPLAY` (once the user has a real or placeholder
  file to hand — a placeholder is enough to prove the CI plumbing, same reasoning as 16.1's own
  `Verify:`); a push shows both `ci.yml`'s split steps and `release.yml`'s restore step running
  (`gh run list`), and `js-yaml` validates both files' syntax locally first. **Touches `.github/`
  — needs a `Gate-change:` line.**
  Note: `js-yaml` validated both files locally. The secret was initially left unset (no file yet),
  then the user created the real Firebase project, downloaded `google-services.json`, and set
  `WALLOS_GOOGLE_SERVICES_GPLAY`. That first live CI run still failed, on a real bug this step's
  own restore step had: `androidApp/src/gplay/` has no other tracked file yet (16.3 is what first
  adds one, `CrashReporterImpl.kt`), so git doesn't materialize the directory on a fresh checkout at
  all — `echo ... > androidApp/src/gplay/google-services.json` fails with "No such file or
  directory" before the secret's contents ever matter. Fixed with a `mkdir -p androidApp/src/gplay`
  ahead of the redirect in both `ci.yml` and `release.yml`. This is also why the real
  `google-services.json` needed a debug-variant Android app added in the Firebase console
  alongside the release one: `gplay` has no `applicationIdSuffix` (`AppFlavors.kt`), so the debug
  build type alone produces `com.grappim.wallosmobile.debug`, which a Firebase project that only
  registered the release `com.grappim.wallosmobile` app doesn't recognize —
  `processGplayDebugGoogleServices` fails with "No matching client found" until both package ids
  are registered clients in the same file. `./gradlew :androidApp:assembleGplayDebug -PgplayBuild`
  now builds clean locally against the real file with both clients present.

- [x] **16.3 — `CrashReporter` seam: `core:crashreporting-api`, flavor impls, consent storage**
  New KMP module `core/crashreporting-api` (mirrors `core:appinfo-api`'s one-file interface
  shape): `CrashReporter` with `isAvailable: Boolean`, `setCollectionEnabled(enabled: Boolean)`,
  `recordException(throwable: Throwable)`, `log(message: String)`. Two implementations, both
  `@Single(binds = [CrashReporter::class])`, both in package `com.grappim.wallosmobile.di` (matches
  `AndroidModule`'s existing single-package `@ComponentScan`, no second scan path needed):
  `androidApp/src/gplay/kotlin/.../di/CrashReporterImpl.kt` (wraps `Firebase.crashlytics`,
  `isAvailable = true`) and `androidApp/src/fdroid/kotlin/.../di/CrashReporterImpl.kt` (every
  member a no-op, `isAvailable = false`). `WallosApp.kt` plants a second `Timber.Tree`
  unconditionally (beside the existing debug-only `Timber.DebugTree()`) that forwards
  `ERROR`-priority logs to the injected `CrashReporter` — ported from Taiga's `CrashlyticsTree`
  as-is; it's a no-op on fdroid via the impl, not a branch at the call site. New
  `core/storage/.../crashreporting/CrashReportingStorage.kt` (interface + `@Single` impl,
  DataStore-backed, mirrors `ThemeStorage`'s exact shape) holds `crashReportingEnabled:
  Flow<Boolean>` (default `false`, opt-in) + `suspend fun setCrashReportingEnabled(enabled:
  Boolean)`; `WallosApp.onCreate()` observes it and calls `crashReporter.setCollectionEnabled(...)`
  on every change. `FakeCrashReporter` in `:testing`, alongside the module's other fakes.
  *Verify:* `./gradlew allTests detekt ktlintCheck` passes; `KoinGraphTest` passes for both
  flavors (catches a missing binding on either side); a new module needs its own line in the root
  `kover {}` block (non-negotiable, easy to silently skip). Real Crashlytics delivery is not
  verifiable without 16.1's blocked Firebase project — say so rather than claiming it.
  Note: All ran clean — `./gradlew allTests detekt ktlintCheck` green, `compileFdroidDebugKotlin`
  and `compileGplayDebugKotlin -PgplayBuild` both clean with `--rerun-tasks` (forces the Koin
  compiler plugin to see the two new `@Single`s). Two corrections to this step's own text: (1)
  `KoinGraphTest` verifies only `AppModule` (`composeApp`'s graph) — `AndroidModule`, where both
  `CrashReporterImpl`s actually live, sits in `androidApp` above `composeApp` and was never
  reachable from it (same reason `AppInfoProvider` is in `KoinGraphTest`'s own `EXTERNALLY_SUPPLIED`
  list). It cannot catch a missing binding on either flavor; that only happens at runtime. Verified
  instead by installing both flavors on the emulator and confirming cold start — 16.1's Firebase
  project is real now (see M16 preamble), so this was a genuine check, not just a structural one:
  `logcat` showed `FirebaseCrashlytics: Initializing Firebase Crashlytics 20.1.0` for
  `com.grappim.wallosmobile.debug` (gplay) and a clean `Displayed …` line with no
  `FATAL EXCEPTION`/`AndroidRuntime` for both `com.grappim.wallosmobile.debug` and
  `com.grappim.wallosmobile.fdroid.debug`. (2) The "not verifiable without 16.1's blocked Firebase
  project" line was already stale by the time this step ran — 16.1's own preamble had already
  flagged that the project exists now; superseded by the above. Two other deviations, neither
  structural: `CrashlyticsTree` went in `com.grappim.wallosmobile` (top-level, alongside
  `WallosApp`/`MainActivity`) rather than Taiga's `data` package — no `data` package exists yet in
  `androidApp/src/main`, and it isn't a Koin definition itself so it doesn't need `di`. And
  `androidApp/build.gradle.kts` only needed an explicit `core:async-kmp` line (for
  `ApplicationScope`, used by `WallosApp`'s new consent-observing collector) — `core:storage`
  itself reaches `androidApp` transitively already, because `composeApp` declares it `api`, not
  `implementation` (composeApp/build.gradle.kts) — worth knowing before assuming every module
  androidApp touches needs its own explicit line.

- [x] **16.4 — Settings UI: crash-reporting toggle, privacy-policy link**
  `feature/settings/ui`'s existing `InterfaceScreen`/`InterfaceViewModel` (already home to the
  theme-mode picker) gains a toggle bound to `CrashReportingStorage`, gated on
  `crashReporter.isAvailable` so it doesn't render at all on fdroid — runtime-gated via DI, not a
  compile-time flavor check (testable with `FakeCrashReporter` from 16.3, same reasoning as
  Taiga's own `SettingsInterfaceViewModel`). `AboutScreen`'s `AboutContent` gains a second
  `Button` beside the existing "Project" one (`about_project_url`, already there — nothing to add
  for that link), opening a new `privacyPolicyLink` resolved in `AboutViewModel` the same way
  Taiga's `SettingsAboutScreenViewModel` does: `crashReporter.isAvailable` picks between two new
  `translatable="false"` string resources, `privacy_policy_url`/`privacy_policy_url_gplay`, both
  raw GitHub blob URLs. Two new docs at repo root, `PRIVACY_POLICY.md` and
  `PRIVACY_POLICY_GPLAY.md`, adapted from Taiga's own (plan §3.10 has the specifics — swap every
  Taiga.io/TaigaMobileNova reference for Wallos/WallosMobile, and rewrite the "Authentication"
  section for what this app actually stores: the API key and server URL via `core:storage`, not a
  username/password or OAuth token, and `ApiKeyStorageImpl`'s Keystore-backed encryption is a
  stronger claim than Taiga's plain "stored locally" that's worth stating).
  *Verify:* `InterfaceViewModelTest`/`AboutViewModelTest` cover both `isAvailable` states;
  `./gradlew detekt ktlintCheck`; on-device, `fdroidDebug` shows neither the toggle nor a
  gplay-flavored privacy link (only `PRIVACY_POLICY.md`) — full gplay-side confirmation waits on
  16.1's blocked Firebase project the same as 16.3.
  Note: All ran clean — `allTests detekt ktlintCheck` green (new `InterfaceViewModelTest`/
  `AboutViewModelTest` cases for both `isAvailable` states), both flavors compile
  (`compileFdroidDebugKotlin`, `compileGplayDebugKotlin -PgplayBuild --rerun-tasks`). One correction
  to this step's own text: 16.1's Firebase project was never actually blocking (M16's preamble
  already flagged this before 16.3 ran) — both flavors were installed and driven on-device for
  real, not just structurally. `fdroidDebug`: Interface shows only the theme radio group, no
  Privacy section. `gplayDebug`: Interface shows a Privacy section with the crash-reporting
  toggle, defaulting off, and the toggle survives `am force-stop` + relaunch (the DataStore write
  is real, not in-memory only). The About screen's "Privacy Policy" button renders on **both**
  flavors always (by design — only its target URL differs by `isAvailable`, per plan §3.10); tapping
  it on fdroid opened `github.com/.../blob/dev/PRIVACY_POLICY.md`, a 404 only because this commit
  hadn't been pushed yet, not a wrong URL. `KoinGraphTest` needed a new `EXTERNALLY_SUPPLIED` entry
  for `CrashReporter::class` (same shape as `AppInfoProvider`, documented in
  `KoinGraphTest.kt`'s own comment) — `AboutViewModel`/`InterfaceViewModel` are the first
  `composeApp`-graph consumers of a binding that actually lives in `androidApp`'s `AndroidModule`,
  which the test cannot see; this was not caught by the step's own `Verify:` line as written and
  only surfaced by running the repo-wide `allTests`.

- [x] **16.5 — `AppUpdateChecker` seam + a Compose snackbar shell surface**
  New `AppUpdateChecker` interface, living entirely in `androidApp` (no KMP module — nothing
  outside it needs this, unlike `CrashReporter`): `updateState: Flow<UpdateState>`,
  `checkAndRequestUpdate(activity: Activity)`, `checkUpdateStateOnResume()`,
  `registerUpdateListener()`, `unregisterUpdateListener()`, `completeUpdate()`; `sealed class
  UpdateState { data object UpdateDownloaded }`. Same flavor-swap trick as 16.3, same
  `com.grappim.wallosmobile.di` package: gplay wraps `AppUpdateManagerFactory.create(context)`
  (Play Core, **FLEXIBLE** type only), fdroid is every member a no-op / `flowOf()`. `MainActivity`
  wires the imperative half exactly like Taiga: `checkAndRequestUpdate(this)` once in `onCreate`,
  `registerUpdateListener()` + `checkUpdateStateOnResume()` in `onResume`,
  `unregisterUpdateListener()` in `onPause` — Play Core's `startUpdateFlow` needs a real
  `Activity`, so this part can't move into Compose. **Decided 2026-08-10, diverging from Taiga's
  plain View `Snackbar`**: build a real Compose snackbar surface, since WallosMobile has none
  today and it's worth having as reusable shell infra rather than a one-off. Mirrors
  `TopBarController`'s shape (`uikit/.../widgets/topappbar/`) — a remembered controller wrapping a
  `SnackbarHostState`, provided through a new `LocalSnackbarHostController`, wired into
  `AuthenticatedMainScreen`'s `MainScaffold` via `Scaffold`'s existing `snackbarHost = {
  SnackbarHost(...) }` slot. Scoped to the authenticated shell only — `LoginScreen` has no
  `Scaffold` and doesn't get one for this. `updateState` is collected wherever the new controller
  is reachable and shows the restart prompt on `UpdateState.UpdateDownloaded`, action calling
  `appUpdateChecker.completeUpdate()`.
  *Verify:* `./gradlew :androidApp:assembleFdroidDebug :androidApp:assembleGplayDebug` (plain,
  no property — both flavors' no-op-vs-real split compiles); `detekt ktlintCheck`; on-device,
  drive the snackbar surface with something already in the shell that can trigger it (or a
  temporary manual `show()` call removed before ticking) to confirm the `Scaffold` wiring actually
  renders one, since `AppUpdateChecker` itself can't be forced to emit `UpdateDownloaded` without
  a real Play Store release channel. **Touches `.editorconfig`** (new
  `compose_allowed_composition_locals` entry) **— needs a `Gate-change:` line.**
  Note: `composeApp` cannot depend on `androidApp` (Gradle dependency runs the other way), so
  `AppUpdateChecker`/`UpdateState` — both androidApp types — can never appear in `composeApp`
  code, unlike `CrashReporter` (a real KMP module 16.3 could inject straight into `AboutViewModel`).
  **First pass narrowed `appUpdateChecker.updateState` to a `Flow<Unit>` signal threaded down
  through `WallosAppContent`/`AuthenticatedMainScreen`** (mirroring `onDarkThemeChange`) — reverted
  same session after review: unnecessary indirection, and its `.filterIsInstance<>().map { Unit }`
  call sat directly inside `setContent`'s composable lambda, which Android Studio's Compose lint
  flags as `FlowOperatorInvokedInComposition` (a fresh `Flow` object every recomposition; neither
  `detekt` nor `ktlintCheck` runs Compose lint, so both gates missed it). **What actually shipped**:
  `SnackbarHostController` (`uikit/.../widgets/snackbar/`, `LocalSnackbarHostController` + a thin
  `SnackbarHostState` wrapper — legal to construct outside composition, since `SnackbarHostState`
  has no composition dependency, only a `mutableStateOf` read inside `SnackbarHost`) is now a plain
  `MainActivity` field, not `remember`ed in `AuthenticatedMainScreen`. `MainActivity` collects
  `appUpdateChecker.updateState` in `lifecycleScope` (not `LaunchedEffect`) and calls
  `snackbarHostController.show(...)` directly — closer to Taiga's own imperative structure than the
  reverted design. Only the plain `SnackbarHostController` instance crosses into
  `WallosAppContent`/`AuthenticatedMainScreen` as a parameter (default
  `remember { SnackbarHostController() }` for callers that don't pass one), which then provide it
  via `LocalSnackbarHostController` and thread it into `MainScaffold`'s new
  `snackbarHost = { SnackbarHost(...) }` slot — `WallosMobilePreviewTheme` now provides the local
  too, matching `LocalTopBarConfig`/`LocalIsOffline`. Because `AppUpdateChecker` is never injected
  into a `composeApp`-graph class either way, `KoinGraphTest` needed **no** new
  `EXTERNALLY_SUPPLIED` entry — 16.3/16.4's `CrashReporter::class` addition doesn't repeat here.
  `AppUpdateChecker`'s interface + `UpdateState` live in
  `androidApp/src/main/kotlin/.../di/AppUpdateChecker.kt`, package `com.grappim.wallosmobile.di`
  (not Taiga's `data` package) — colocated with both flavor impls, which the single
  `@ComponentScan("com.grappim.wallosmobile.di")` in `AndroidModule` already covers. Two new
  `:strings` entries, `app_update_downloaded`/`app_update_restart`, resolved from `androidApp`
  itself via the suspend `org.jetbrains.compose.resources.getString(RString.x)` accessor (not the
  `@Composable`-only `stringResource(...)`) inside `lifecycleScope` — which needed `androidApp` to
  gain its own `implementation` lines on `:strings`, `:uikit` and `jetbrains.compose.material3`
  (none reach it transitively: `composeApp` depends on all three via `implementation`, and
  `SnackbarHostController.show()` takes/returns `SnackbarDuration`/`SnackbarResult`, both material3
  types). `./gradlew :androidApp:assembleGplayDebug -PgplayBuild detekt ktlintCheck allTests` all
  green, both flavors' `compileXxxDebugKotlin` clean. `assembleFdroidDebug` compiles clean but fails
  at `packageFdroidDebug` in this session — `SigningConfig "fdroidDebug" is missing required
  property "storePassword"` — confirmed **pre-existing and unrelated to this step**: the same
  command fails identically against a clean `git stash`'d tree, because
  `WALLOS_STORE_PASS_FDROID_DEBUG` and its two siblings (`build-logic`'s
  `AndroidApplicationConventionPlugin`) are per-machine env vars this shell session doesn't have
  set, not a regression. On-device (`installGplayDebug -PgplayBuild`, `Medium_Phone_API_36.1` —
  the first attempt landed on a stray physical device, `SM-A920F` connected over USB, caught via
  `adb devices -l` per the `emulator-testing` skill's own warning before proceeding on unfamiliar
  hardware): a temporary `lifecycleScope.launch { showRestartSnackbar() }` in `onCreate`, removed
  before ticking, confirmed the full real path — the snackbar rendered over the Dashboard with its
  actual resolved strings ("An update has just been downloaded" / "Restart"), and tapping Restart
  (`uiautomator dump`'s real-pixel bounds, not a scaled screenshot coordinate) dismissed it cleanly
  with no `FATAL`/`AndroidRuntime` in `logcat` — `appUpdateChecker.completeUpdate()` doesn't throw
  with no real update session active. Real `UpdateDownloaded` delivery stays unverifiable without a
  Play Store release channel, as the step's own text anticipated.


## M17 — MASVS security review

Decomposed 2026-08-11 from the "To review" entry filed 2026-08-10 ("TaigaMobileNova recently did a
security review and a testing overhaul"). That entry asked two questions: does WallosMobile need a
MASVS review of its own, and does Taiga's testing overhaul teach anything new. Both were answered
before writing this milestone, not left for step 17.1 to discover:

- **Security: yes, a real gap, worth the same eight-category shape Taiga used**
  (`TaigaMobileNova/docs/security/masvs-review-plan.md`). `docs/security/masvs.md` doesn't exist
  here yet, and the `masvs-review` skill (`~/.claude/skills/masvs-review`) is already available —
  same skill, same mechanics, one MASVS v2 category per session, register-first. **WallosMobile
  starts from a materially better position than Taiga did on the categories most likely to matter,
  confirmed by reading the source, not assumed from the parallel:**
  - **Storage/Crypto**: `core/storage/.../SecretCipher.kt` + `KeystoreSecretCipher.kt` already
    encrypt the API key (AES/GCM, Keystore-resident key, fresh IV per encryption, `base64(iv ||
    ciphertext)`) before `ApiKeyStorageImpl` writes it to DataStore — Taiga had *no*
    application-level cryptography at all when its own task 1 started and had to design this from
    scratch. 17.2 is mostly confirmation, not implementation.
  - **Network**: `core/api/.../CompositeTrustManager.kt` already exists — the same TOFU
    trust-manager pattern Taiga's own task 2 reviewed, ported per this repo's reference-project
    convention (`CLAUDE.md`'s TaigaMobileNova row). 17.3 runs the same three TOFU questions
    `kmp-checks.md` names against *this* copy rather than assuming Taiga's review still describes
    it — the bound could have drifted since the port.
  - **Auth/Platform — a different shape than Taiga's, not the same finding.** WallosMobile's
    username/password onboarding (plan §1.1, `CLAUDE.md`'s "Wallos login bridge") drives a plain
    Ktor POST/GET against `login.php`/`profile.php` and regex-scrapes the key out of the HTML
    response (`feature/setup/data/.../WebLoginApi.kt`, `ApiKeyScraper.kt`) — confirmed **no
    `WebView` anywhere in the repo** (`grep -rln 'WebView\|javaScriptEnabled\|addJavascriptInterface'`
    is empty). Taiga's AUTH-1/PLATFORM-2 finding was specifically about an embedded WebView
    rendering a third-party login page; that shape doesn't exist here. What *does* need checking,
    per `kmp-checks.md`'s own "scraping a credential by driving a web login" note: is the password
    ever stored or logged beyond the POST call, and is the scraped page fixed to the user's own
    configured host (it structurally is — there's only one server URL in play — but 17.4 confirms
    from source rather than asserting it).
  - **Code**: `renovate.json` already has `"osvVulnerabilityAlerts": true` — the exact fix Taiga's
    own task 5 had to add. 17.6 confirms it's still there and checks the other CODE controls
    (`minSdk = 24`, JSON deserialization tolerance, `LocalUriHandler` call sites — `AboutScreen.kt`
    has two, both fixed strings from `RString`, not user/server-supplied, unlike Taiga's
    custom-field-URL finding).
  - **Privacy**: crash-reporting disclosure infra already shipped in M16
    (`PRIVACY_POLICY_GPLAY.md`, the Settings opt-out toggle), and `ApiKeyStorage.clear()` already
    drops both the key and the cache in one place (`CLAUDE.md`'s storage Non-negotiable) — the
    exact shape Taiga's MASVS-PRIVACY-4 asked for. 17.7 is confirming an already-stated design
    decision holds in the register, not designing one.
  - **What hasn't been checked at all**: `allowBackup="true"` on the main manifest with **no**
    `dataExtractionRules`/`fullBackupContent` anywhere in the repo (confirmed by grep — unlike
    Taiga, there's no debug-vs-release inversion here since the debug manifest only touches the
    app label, but the release side still has no backup-exclusion XML at all). Bounded somewhat by
    the cipher already handling "ciphertext restored onto another device" as a decrypt failure
    (`KeystoreSecretCipher`'s doc comment), but 17.1 should confirm that bound holds rather than
    assume it from this note. `FLAG_SECURE` is also absent (`grep -rn 'FLAG_SECURE'` empty) and
    `LoginScreen.kt` has a password field — 17.5 checks whether it has a reveal toggle the way
    Taiga's did.
  - **Pre-v1 changes what CRYPTO/STORAGE fixes cost.** Taiga is live, so its task 1 had to design a
    plaintext→ciphertext migration for already-installed users. WallosMobile isn't
    (`CLAUDE.md`'s pre-v1 no-backcompat rule, still in force per the "To review" entry above) — if
    17.1/17.2 find something to change about the cipher scheme itself, no migration path is needed,
    just a changed-and-say-so per the standing rule.

- **Testing: next after M17, not folded into it.** Taiga's entire Compose UI test sweep
  (`docs/testing/improvement-plan.md` tasks 10–21, `compose-ui-test-spike.md`) runs
  `runComposeUiTest` inside a **`jvmTest`** source set, backed by
  `compose.dependencies.desktop.uiTestJUnit4`/`currentOs` (Compose Desktop test artifacts).
  WallosMobile declares **no `jvm()` target** (`KmpLibraryConventionPlugin.kt`'s own comment on why
  `androidHostTest` exists instead — "There is no `jvmTest`" is already stated in `CLAUDE.md`'s
  Build commands), so Taiga's exact mechanism has nothing to attach to today. **That is a setup
  gap, not a reason to skip Compose UI testing** — the milestone after this one should scope
  whatever setup WallosMobile actually needs (a `jvm()` target the way Taiga has one, so
  `runComposeUiTest` can run in `jvmTest` the identical way, or a build-out of the
  `androidDeviceTest` route the checklist's own "To review" FAB/scroll-jank entries already point
  at, since 3.3 paid part of that setup cost already) rather than assuming either is already in
  place. Kover-coverage-heuristics and the rest of Taiga's survey don't surface any new reasoning
  to reopen the settled no-Kover-floor decision. The existing "Compose UI test setup" entry under
  **To review** is the seed for that milestone — scope it once M17 closes.

**How to run a step:** invoke the `masvs-review` skill (`~/.claude/skills/masvs-review`), scoped to
the one MASVS v2 category the step names — don't let it default to a whole-app pass. It reads
`docs/security/masvs.md` first (17.1 creates it from the skill's own template) and separates
verified-statically / needs-a-device-or-APK / not-checked, per the skill's own Step 3. Any Open
finding worth fixing now: fix it if small and isolated (per this repo's own gate rules — check
`ktlintCheck`/`detekt` still pass, and whether the change touches a tripwire path needing a
`Gate-change:` line); if bigger, write it into a durable place (this repo has no `docs/revisit.md`
yet — start one, matching Taiga's shape, if a step needs to defer a finding rather than fix it).
`Verify:` for every step below is the same shape: `docs/security/masvs.md` gained the named
category's section (Accepted/Open/Needs-a-device rows), and any code changed passes
`./gradlew detekt ktlintCheck`.

Order follows Taiga's own rationale (`masvs-review-plan.md`'s "Order rationale"): Storage first
since the stored credential is the asset the skill's framing centers on and scoping above already
found where it lives; Crypto immediately after since it's the same question one layer down; Network
next for the trust-manager read; Auth before Platform since the login bridge is both an AUTH and a
PLATFORM concern on the same code; Code and Privacy after, being smaller and more mechanical;
Resilience last, a scope decision rather than an audit.

- [x] **17.1 — Storage.** Confirm the cipher-before-DataStore path for the API key, decide whether
  the `allowBackup`/no-extraction-rules gap above is an Open finding or an Accepted deviation (state
  the actual bound, don't just copy this preamble's guess), confirm `ServerUrlStorageImpl` holds
  only a bare URL (no embedded credential, matching Taiga's own MASVS-STORAGE-2 row for the same
  shape), and grep for any log call site near auth/key handling.
  *Verify:* `docs/security/masvs.md` exists with a Storage section; both leads above resolved one
  way or the other.
  Note: both leads resolved as **Accepted**, not Open — unlike Taiga, WallosMobile's Keystore cipher
  already existed *before* this review (17.2 confirms it, not designs it), so the `allowBackup` gap
  never carried a plaintext credential the way Taiga's pre-fix state did; excluding the shared
  DataStore file from backup would cost real UX (losing theme/start-destination/server URL on every
  restore) for a property the cipher already provides. One "Needs a device" row added for hardware-
  backing and real restore-onto-second-device confirmation. No code changed, no `Gate-change:`
  needed.

- [x] **17.2 — Cryptography.** Review `KeystoreSecretCipher`'s actual `KeyGenParameterSpec` (block
  mode, IV reuse across encryptions, padding) against `kmp-checks.md`'s CRYPTO checks; confirm no
  other key material exists in source/build config/version catalogue.
  *Verify:* register gains a Cryptography section — concrete findings, or an explicit "reviewed,
  bounded, here's why" note.
  Note: clean bill, no code changed. AES/GCM, no padding (correct for GCM), 128-bit tag, key never
  exported. IV is not merely defaulted-fresh but platform-*enforced* fresh — `encrypt()` never
  supplies its own IV and `setRandomizedEncryptionRequired(false)` is never called, so a fixed/reused
  IV isn't reachable through this code even by mistake. No key/secret literal found anywhere in
  source, `build-logic`, or `gradle/libs.versions.toml` (signing passwords come from `System.getenv`;
  the only string-literal hits were test fixtures and one Compose preview default). One real gap
  that needed a device rather than a source read: `KeyGenParameterSpec` never calls `.setKeySize()`,
  so the actual generated AES key size depends on the Keystore provider's default, not a value in our
  code — added as a second row to the existing Needs-a-device table alongside the hardware-backing
  one from 17.1, rather than asserted as 256-bit from Android's documented default.

- [x] **17.3 — Network.** Run the three TOFU questions from `kmp-checks.md` against
  `CompositeTrustManager` as it stands today (don't assume Taiga's own review of the pre-port
  version still describes it); record the `usesCleartextTraffic="true"` deviation with its actual
  bound (is the API key ever sent in the clear — check `AuthHeaderPlugin`-equivalent call sites).
  *Verify:* register gains a Network section covering both.
  Note: both Accepted, no Open findings. TOFU trust manager: falls through to the platform default
  before ever offering trust, requires a hostname/SAN match, pins per-`(host, fingerprint)` not
  per-host, still checks expiry on a pin hit, and only ever activates from the user's explicit
  Confirm tap in `LoginViewModel.onCertTrustConfirm()` — all three `kmp-checks.md` TOFU questions
  answered by existing `CompositeTrustManagerTest` cases, no gap found. Cleartext: the API key
  (form-body, never a URL param) does travel unencrypted on an `http://` instance since there's no
  `networkSecurityConfig` scoping it, bounded by `RedactingLogger` keeping it out of logcat and by
  the existing `LoginUiState` cleartext warning on the password path — accepted, same reasoning as
  the STORAGE `allowBackup` row. One real gap found, not a live security hole: no way to revoke an
  accepted TOFU pin from inside the app (no `untrust` method, no settings screen) — filed as
  `docs/revisit.md` #1 (new file), not fixed here since it's a new storage method plus a UI screen,
  not a small isolated change. No code changed, no `Gate-change:` needed.

- [x] **17.4 — Authentication.** Verify the login-bridge shape from this preamble with file:line
  (`WebLoginApi.kt`/`ApiKeyScraper.kt`/`LoginThrottle.kt`) — confirm the password never persists or
  logs beyond the POST call, and that the scrape target is always the user's own configured server.
  Confirm MASVS-AUTH-2/3 (local auth, step-up) are N/A — no biometric anywhere
  (`grep -rln 'biometric\|Biometric\|BiometricPrompt'` already confirmed empty during this
  milestone's scoping; re-confirm rather than trust the preamble).
  *Verify:* register gains an Auth section.
  Note: clean bill, no code changed. Password lives only in `LoginUiState.password`
  (`MutableStateFlow`, not `SavedStateHandle` — login isn't a route), reaches the network exactly
  once as a form-body param on the same `RedactingLogger`-covered engine, and is cleared on both
  success and the TOTP handoff (not on a refused attempt, which is the field staying visible for
  the user to correct, not a persistence gap). Scrape target confirmed always the user's own
  configured host: `BaseUrlProviderImpl` reads `ServerUrlStorage.serverUrl`, which
  `SetupRepositoryImpl` saves from the typed URL *before* any web call. No `WebView` anywhere
  (re-confirmed empty), so RFC 8252 doesn't apply. AUTH-2/3 re-confirmed N/A (biometric grep still
  empty). One real, bounded design tradeoff recorded as Accepted rather than Open: `LoginThrottle`
  is a client-side-only backoff — Wallos itself has no server-side lockout, and the counter resets
  on process death and is bypassable by calling the API directly, so it's a courtesy against this
  app being turned into a brute-force tool, not an access control. No `Gate-change:` needed.

- [x] **17.5 — Platform.** Confirm the manifest's IPC surface (only `MainActivity` exported, plain
  `MAIN`/`LAUNCHER`, no deep link); check `LoginScreen.kt`'s password field for a reveal toggle and
  decide whether the `FLAG_SECURE` absence is a finding or an accepted product tradeoff (same
  question Taiga's maintainer answered for itself — don't assume the same answer applies here
  without asking).
  *Verify:* register gains a Platform section.
  Note: clean bill, no code changed. IPC surface is `MainActivity` alone — no other component in
  either manifest, plain `MAIN`/`LAUNCHER`, no deep-link `<data>`. Password field has a working
  Show/Hide toggle, hidden by default. Asked the user about `FLAG_SECURE` rather than assuming:
  they don't want it, for the same reason Taiga's maintainer declined it — it would block
  screenshots app-wide, not just on login, and they use their own app. Recorded as Accepted, same
  shape and same reasoning as Taiga's row for this control.

- [x] **17.6 — Code quality.** Confirm `minSdk = 24`'s rationale (or lack of one, same as Taiga's
  finding) is stated plainly; confirm `renovate.json`'s `osvVulnerabilityAlerts` is still set and
  check `gh api repos/<owner>/<repo>/vulnerability-alerts` for GitHub's native alert status; confirm
  `WallosEnvelopeParser`/the app's JSON config tolerates unknown/null server fields; check both
  `LocalUriHandler.openUri()` call sites in `AboutScreen.kt`.
  *Verify:* register gains a Code section; the dependabot/renovate question has a stated answer,
  not a guess.
  Note: clean bill, all four CODE controls Accepted, no Open findings. `minSdk = 24` traced by
  `git log --follow -p` to this project's first commit ("replace the wizard's catalog with
  TaigaMobileNova's ... minSdk 24") — ported, no independent rationale documented, same absence
  Taiga's own row recorded for the value it came from. `renovate.json` still sets
  `osvVulnerabilityAlerts: true` (present since M14). GitHub's native Dependabot alerts are OFF
  (`gh api repos/Grigoriym/Wallosmobile/vulnerability-alerts` → 404), same result Taiga's review
  found for its own repo — an optional, separate lever, not required now that Renovate's OSV
  coverage is on. `WallosEnvelopeParser` is the app's *only* JSON config (`NetworkModule.kt`
  installs no Ktor `ContentNegotiation` plugin), already `ignoreUnknownKeys = true` /
  `isLenient = true`. Both `LocalUriHandler.openUri()` call sites (`AboutScreen.kt:81,88`) resolve
  to fixed `RString` resources, never user/server-supplied text — no markdown renderer or
  user-editable URL field exists anywhere in the repo, so this doesn't reproduce Taiga's own
  MASVS-CODE-4 finding (a `SafeUriHandler` allowlist has nothing to guard here today). Also
  recorded MASVS-CODE-2 (Play `FLEXIBLE`-only update on gplay, none on fdroid — M16's existing
  shape, Accepted) while in the category, since the skill scopes to the whole control set, not
  just the step's named checks. No code changed, no `Gate-change:` needed.

- [x] **17.7 — Privacy.** Confirm both flavors' crash-reporting posture per M16 (Gplay real,
  fdroid no-op) is disclosed correctly in the register; confirm `ApiKeyStorage.clear()`'s
  cache-plus-key eviction actually satisfies MASVS-PRIVACY-4 by reading its three call sites
  (disconnect, both login paths); diff declared permissions (`INTERNET`,
  `ACCESS_NETWORK_STATE`) against actual call sites.
  *Verify:* register gains a Privacy section.
  Note: clean bill, all four PRIVACY controls Accepted, no Open findings. Both declared
  permissions (`INTERNET`, `ACCESS_NETWORK_STATE`) trace to real call sites (the Ktor OkHttp
  engine; `NetworkMonitorImpl`'s `ConnectivityManager` callback), no unused or missing one. No
  analytics/ad-ID dependency anywhere in the catalogue — only `firebase-crashlytics`, not
  `firebase-analytics`, and it's `gplay`-only. Crash-reporting disclosure confirmed structurally
  real, not just a flag: fdroid's `CrashReporterImpl` is a total no-op (`isAvailable = false`),
  and `InterfaceScreen.kt` gates the whole settings row on that flag, so the toggle is *absent* on
  fdroid rather than present-and-inert; both `PRIVACY_POLICY*.md` docs mirror the same split.
  `ApiKeyStorage.clear()` deletes all three Room tables the app has (confirmed against
  `WallosDB.kt`'s own entity list — no other cache exists) before removing the key, and all three
  call sites (`SettingsViewModel` disconnect, both `SetupRepositoryImpl` login paths) confirmed —
  no account's cache survives into the next login. No code changed, no `Gate-change:` needed.

- [x] **17.8 — Resilience (scope decision only).** Confirm the self-hosted-FOSS-client reasoning
  that makes MASVS-RESILIENCE out of scope actually holds for WallosMobile specifically — check for
  any embedded secret the way Taiga's task 7 did (`grep -rln 'client_secret\|CLIENT_SECRET'` and
  equivalents; WallosMobile has no OAuth flow at all, so this should be an even faster N/A than
  Taiga's). Write the exclusion into the register's header. No code review beyond that — this closes
  the milestone once done.
  *Verify:* register header states the Resilience exclusion with its reason.
  Note: confirmed N/A, even faster than Taiga's own task 7 — grep for
  `client_secret|CLIENT_SECRET|client_id|OAuth` across source, `build-logic` and
  `gradle/libs.versions.toml` found nothing, and the app has no OAuth flow at all (the login bridge
  is a plain POST/GET + regex scrape against the user's own server, not a third-party exchange with
  a registered client secret). Register header rewritten to state all eight MASVS categories were
  addressed (seven reviewed, RESILIENCE excluded with its reason inline). No code changed, no
  `Gate-change:` needed. M17 done.



## M18 — Trusted certificate revocation (not in plan §8's phase order)

Decomposed 2026-08-11 straight from `docs/revisit.md` #1 (filed 2026-08-11 during 17.3's
MASVS-NETWORK review). `TrustedCertStorage` (`core/storage/.../cert/TrustedCertStorage.kt`) only
exposes `isTrusted`/`trust` — no `untrust`, and no Settings screen listing accepted pins, unlike
TaigaMobileNova's own `feature/settings/ui/.../trustedcerts/`. Not a live security hole: the pin is
per-`(host, sha256Fingerprint)`, so a legitimate cert rotation on an already-trusted host just fails
the match and re-triggers TOFU rather than silently keeping the old trust — this is a hygiene/UX
gap (no way to *proactively* clean up a mistaken accept or a decommissioned instance), not an
escalation path. Two steps, storage then UI, the same split M12 used for its own storage-then-picker
shape.

- [x] **18.1 — core/storage + core/domain: `untrust`/`getAllFlow` on `TrustedCertStorage`**
  Store the full `PendingCertTrust` per pin, not just `host|fingerprint` — `PendingCertTrust`
  (`core/domain/.../PendingCertTrust.kt`) already carries subject/issuer/validity, and
  `SetupRepositoryImpl.trustCertificate` already has the full value in hand, currently narrowing it
  to two strings before it reaches storage. Same shape as TaigaMobileNova's own
  `core/storage/.../cert/TrustedCertStorage.kt`: a JSON-encoded `List<PendingCertTrust>` behind one
  `stringPreferencesKey`, replacing the current `Set<String>` of `"$host|$fingerprint"` entries.
  Interface gains `fun getAllFlow(): Flow<List<PendingCertTrust>>` and
  `suspend fun untrust(host: String, sha256Fingerprint: String)`; `trust` takes a `PendingCertTrust`
  instead of two strings. `core/domain` and `core/storage` both need
  `alias(libs.plugins.wallosmobile.kmp.serialization)` added (neither has it yet) for
  `@Serializable` on `PendingCertTrust` and for the `Json` instance the impl encodes with. Update
  `FakeTrustedCertStorage` (`:testing`) and `TrustedCertStorageImplTest` to the new shape — read
  Taiga's own `TrustedCertStorageImplTest` for what cases it covers before reinventing them.
  *Verify:* `./gradlew :core:storage:testAndroidHostTest`
  ·  *Ref:* `TaigaMobileNova/core/storage/src/commonMain/kotlin/com/grappim/taigamobile/core/storage/cert/TrustedCertStorage.kt`,
  `feature/setup/data/.../SetupRepositoryImpl.kt`'s `trustCertificate`
  **Note:** implemented as designed, `Json { ignoreUnknownKeys = true }` instantiated locally in
  `TrustedCertStorageImpl` rather than injected — no `StorageJsonQualifier`-style DI exists here,
  and the same local-`Json` pattern is already used by `WallosCrudApi`/`WallosEnvelopeParser`, so
  adding DI infra for one class would be unrequested flexibility. `core:storage` needed a direct
  `implementation(projects.core.domain)` (previously had none — `core:api` was the only module
  depending on both), and `:testing` needed its own `api(projects.core.domain)` for
  `FakeTrustedCertStorage`'s public surface, since `core:storage`'s dependency on it is
  `implementation`, not transitive (`CLAUDE.md`'s own rule, confirmed again). `CompositeTrustManagerTest`
  (`core:api`) was the one other caller of the old two-string `trust()` — five call sites updated to
  build a `PendingCertTrust` via a small local test helper. Full suite green:
  `:core:storage:testAndroidHostTest` (11 cert tests, 4 new), `:core:api:testAndroidHostTest`,
  `:feature:setup:data:testAndroidHostTest`, `detekt ktlintCheck` across all four touched modules.

- [x] **18.2 — feature/settings/ui: a "Trusted certificates" screen**
  New sub-screen off Settings, same shape as `startdestination`/`about` (Route/Screen/UiState/
  ViewModel, registered in `NavKeySerializers.kt` and wired into `SettingsEntryProvider.kt`, reached
  via a new `SettingsRow` — a fifth callback on `SettingsScreen`, still exempt under
  `compose:parameter-order`'s single-trailing-function rule as long as `viewModel` stays last).
  Lists each trusted `PendingCertTrust` (host, issuer, valid-until, fingerprint) with a delete
  action per row, confirmed via an `AlertDialog` before it calls `TrustedCertStorage.untrust`
  (`SubscriptionDetailScreen`'s delete-confirm shape is the local precedent — this repo has no
  shared `ConfirmActionDialog`/`EmptyStateWidget` the way Taiga does, so plain `AlertDialog` plus a
  centered `Text` for the empty state, matching `CategoriesScreen`). `TrustedCertsViewModel` takes
  `TrustedCertStorage` directly, no repository — single-seam case like `StartDestinationViewModel`.
  *Verify:* `./gradlew :feature:settings:ui:testAndroidHostTest`, and on the emulator: trust a
  certificate (or seed one via DataStore), open Settings → the new row, confirm it lists the pin
  with correct details, delete it with the confirm dialog, and confirm the next connection attempt
  to that host re-triggers the TOFU prompt rather than silently trusting it.
  ·  *Ref:* `feature/settings/ui/.../startdestination/` (whole package),
  `TaigaMobileNova/feature/settings/ui/.../trustedcerts/TrustedCertificatesScreen.kt`,
  `feature/subscriptions/ui/.../detail/SubscriptionDetailScreen.kt`'s `DeleteConfirmDialog`
  **Note:** implemented as designed — `TrustedCertsRoute`/`Screen`/`UiState`/`ViewModel`, a
  `ListItem`-per-pin `LazyColumn`, `AlertDialog` revoke confirm, wired as a fifth `SettingsRow` and
  registered in both `NavKeySerializers.kt` and `SettingsEntryProvider.kt`. `testAndroidHostTest`
  green (6 new ViewModel tests reusing `:testing`'s `FakeTrustedCertStorage`), `KoinGraphTest` green
  with no `EXTERNALLY_SUPPLIED` entry needed, both flavors compile/assemble, `detekt ktlintCheck`
  green. **On-device end-to-end, against a throwaway TLS front** (the `docs/local-info.txt` recipe,
  built fresh against the live `wallos` container on `wallos_default`): connected to
  `https://10.0.2.2:8443` with a fresh install, accepted the real "Untrusted certificate" TOFU
  prompt, opened Settings → Trusted certificates and confirmed the listed host/issuer/valid-until/
  fingerprint matched the prompt exactly, tapped the delete icon, confirmed the "Revoke this
  certificate?" dialog names the host, confirmed the list went back to the empty state after
  revoking, then reconnected to the same host — the identical "Untrusted certificate" prompt
  reappeared rather than silently trusting it. TLS front (`wallos-tls` container) and self-signed
  CA removed afterward; nothing installed on the emulator's own trust store.

Once 18.2 verified clean on-device, `docs/revisit.md` #1 was deleted — the gap it filed is closed.
**M18 is done.**
