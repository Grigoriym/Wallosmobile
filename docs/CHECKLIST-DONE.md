# WallosMobile — Build Checklist, completed steps

The ticked half of [`CHECKLIST.md`](./CHECKLIST.md), moved here so a session reading the plan for
step N isn't reading a thousand lines of steps that are already in the code. Nothing was reworded
on the way over.

**Read it for precedent, not for instructions.** Each step's `Note:` records what that step
learned, and by the close-out ritual in `CLAUDE.md` anything *structural* in one has already been
folded into [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md) — the plan is canonical where the
two disagree, and a note that contradicts it is a missed fold worth fixing. The Deviations log
stays in `CHECKLIST.md`: it is the table the close-out ritual still writes to.

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

