# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `8/11` · M2 `0/7`
**Current step:** 1.9

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
  Plus the **Koin graph test**: `koin-test` is in the catalog and unused; this is the first step
  where every definition resolves, so add a host test that runs `checkModules()`/`verify()` over
  the app's module set. Missing definitions are otherwise a launch-time crash no gate catches —
  and `composeApp` picks up `kmp.di` here anyway, for `DrawerItemsBuilder` (1.8).
  *Verify:* the graph test passes, **and** fresh install → log in against
  `https://demo.wallosapp.com` (demo/demo) → drawer shell
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

  **Two things are deliberately deferred to here rather than done early:**
  - **Delete the pre-v1 backward-compatibility bullet from `CLAUDE.md`'s Non-negotiables** the
    moment this app is installed by anyone but us. From then on the stored API key and the
    serialized nav back stack are real user state: a renamed DataStore key or a moved route class
    needs a migration, not a shrug.
  - **Verification we chose to grow into, not front-load** (one per step that needs it, never a
    big-bang): a Kover floor that fails under the current number, and a Compose UI test setup —
    Robolectric vs. instrumentation vs. staying on previews. That last one is a real fork with
    real cost; decide it when a screen's logic first outgrows its ViewModel test, not before.
  - **Agent guardrails — investigate and pick one or two.** The gates in this repo constrain the
    *code* an agent writes; nothing yet constrains an agent from weakening a gate to make its own
    step pass. The tripwire set is small and mechanizable: a step commit touching
    `.github/workflows/ci.yml`, `build-logic/**`, `config/detekt/**`, `.editorconfig`,
    `gradle/libs.versions.toml`, or introducing `@Ignore` / `@Suppress` / a detekt baseline /
    a widened `paths-ignore`, should have to say so out loud. `CLAUDE.md` and this file belong on
    that list too — they set the rules every future session runs under, so an unannounced edit to
    them is the highest-leverage change an agent can make. First candidate: a CI job that diffs
    those paths and fails unless the commit message opts in.

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
| 1.6 | `NativeText` created here, in `utils:ui`, which no step owns | `TopBarConfig` is built on it; only the three variants the top bar needs exist, `getErrorMessage` waits for 1.10 — *now in plan §3.3, §5.4* |
| 1.6 | `configureKmpCompose()` gains `jetbrains.compose.icons` | material3 doesn't bring material-icons-core transitively, and every `ui` module will want `Icons.Filled.*` — *now in plan §3.3* |
| 1.7 | `SavedStateConfiguration.DEFAULT` throws at first composition, not on process death | `rememberNavBackStack` `require`s a `serializersModule` that isn't the default — *now in plan §5.5* |
| 1.7 | `NavigatorTest` written from scratch; there is none in MealieMobile to port | Plan §5.6 called it a template, but no such file exists in either reference project — *now in plan §5.6* |
| 1.8 | `SubscriptionsRoute`/`SettingsRoute` start in `composeApp/nav/`, not in a feature `ui` module | The shell can't be stood up without them and neither feature exists; 2.4/2.6 move them to their screens — *now in plan §5.3* |
| 1.8 | `RouteConfig` carries no `FabConfig`, and `DrawerConfig` has no `Hidden` | v1 has no write screens and no fullscreen route — both arrive with the feature that needs them — *now in plan §5.4* |
| 1.8 | `DrawerItemsBuilder` is constructed, not `@Factory`-injected | `startKoin` doesn't exist until 1.11, so `koinInject()` would throw at first composition — *now in plan §5.4* |
| 1.8 | No snackbar host and nothing offline-aware in the shell (Mealie has both) | There is no `NetworkMonitor` (1.4) and no `LocalIsOffline`; errors go to UI state — *now in plan §5.4* |
| 1.5 | No dynamic colour, so no `expect`/`actual` `colorScheme()` and no `androidMain` in `uikit` | Mealie's only reason for the `expect` is `dynamicDarkColorScheme(LocalContext)`; a static palette seeded from the logo navy keeps the brand and the module common — *now in plan §3.3* |
