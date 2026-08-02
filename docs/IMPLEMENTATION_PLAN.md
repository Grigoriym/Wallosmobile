# WallosMobile — Implementation Plan

An unofficial Kotlin Multiplatform client for [Wallos](https://github.com/ellite/Wallos), a
self-hosted subscription tracker. Structure and conventions are ported from
`TaigaMobileNova`, **except navigation, which follows `MealieMobile`'s Navigation 3 setup**
(see [§5 Navigation](#navigation--navigation-3)). The API surface is described in
[`WALLOS_API.md`](./WALLOS_API.md).

**Scope for now:** Android only. The project is built as KMP from day one — all logic lives in
`commonMain`, and enabling iOS/JVM later is a change to one function in `build-logic`, not a
refactor.

> **Building it?** This document is the reference — the *why*. The step-by-step, tickable version
> is [`CHECKLIST.md`](./CHECKLIST.md), where each step is self-contained enough to run in a fresh
> context.

---

## 1. What is different about Wallos

Before the module list, the four facts that shape every architectural decision below. None of
them are how Taiga works, so the ported conventions need real adaptation, not copy-paste.

| Wallos reality | Consequence for the client |
|---|---|
| **Two independent auth systems.** The web UI uses username/password → PHP session cookie; the JSON API uses a static per-user API key and ignores the session entirely. | The app's *runtime* credential is always the API key — no token refresh, no `TokenRefreshPlugin`. But onboarding can still be username/password, by driving the web login once and bridging to the key (§1.1). |
| **The key goes in the form body, not a header.** | No `AuthHeaderPlugin`. Every call is a form-encoded POST built through one place that injects `api_key`. |
| **Everything returns HTTP 200**, including auth failures — and PHP `display_errors` can prepend HTML *before* the JSON. | Ktor's `ContentNegotiation` alone cannot be trusted to decode a response. A sanitize → envelope-check → decode pipeline sits in front of every deserialization. |
| **No pagination anywhere.** `get_subscriptions` returns the entire list. | Filtering, sorting and paging are client-side. This makes an offline-first Room cache cheap and obvious rather than an optimization. |

Two more that bite later: `cycle=5` (One-time) exists in the data but is **rejected on write**,
and `Unauthorized or Not Found` is a per-row ownership error that must **not** clear the stored
key despite its name.

### 1.1 What "login" means here

There is no username/password endpoint in the **JSON API**, but the server has a real password
login on its **web** surface, and API doc §9 documents a working bridge between them. So the app
*can* offer a normal login screen — the user types username and password once and never sees an
API key.

`feature:setup` therefore has **two paths, and needs both**:

**Path A — credential bridge (primary UX).** Drive the web login once, then hand off to the key:

1. `POST /login.php` with `username` + `password`, **redirects disabled**.
   `302` = success, `200` = failure (the page HTML is re-rendered; the reason is not
   machine-readable, so the UI can only say "login failed").
2. If `Location: totp.php`, prompt for the 6-digit code and `POST /totp.php` with
   `one-time-code` on the same cookie jar.
3. `GET /profile.php`, scrape the key out of `id="apikey"`.
4. **Validate the scraped key against `api/status/version.php`, persist it, and discard the
   session cookie and the password.**

**Path B — manual key entry (mandatory fallback).** Base URL + pasted key, with help text naming
the exact location (Profile → API Key). This is not a nice-to-have: it is the recovery route when
Path A breaks, and Path A *will* break — see below.

In code it is `SetupRepository.connectWithApiKey(serverUrl, apiKey): Result<Unit>` — the tail of
Path A with steps 1–3 removed: persist the URL, `clear()` the stored key, validate, store. No web
session is involved, so there is no `LoginOutcome` to report; a rejected key is an
`Unauthenticated` failure and a wrong address is a `Malformed`/`UnsupportedEndpoint` one. Both
paths share one screen and one state, switched by a flag — the reveal is a mode, not a second
route.

**The screen offers the stored URL back** (3.1). `ApiKeyStorage.clear()` keeps the server URL by
design (§4.7), so a re-login after Disconnect is one field — but only because `LoginViewModel`
seeds its state from `SetupRepository.getStoredServerUrl(): Result<String>` on construction. That
read goes through the **repository**, not through `ServerUrlStorage`: this feature has a real
`data` layer, so a `ui` module naming a `core` seam would be reaching past it — the exception
`feature:settings` gets (§7.1) is for a feature that has no such layer at all. It is a `Result`
because a DataStore read can throw and an exception out of `viewModelScope` is a crash. The value
arrives after the first composition, so it fills the field only if the user hasn't typed one.

**Neither path reports success to anyone.** A stored key *is* the outcome, and
`ApiKeyStorage.isConnected` is what the startup branch (§7.1) watches, so the ViewModel raises no
completion event and the screen takes no `onConnectSuccess` — a second owner of that fact could
only ever disagree with the first. The same property is what makes Disconnect free: `clear()`
flips the flow and the shell is replaced, with no navigation involved (1.11).

Consequences for the design:

- **The bridge needs its own HTTP client**, and it is the third one in the app. It speaks HTML not
  JSON, so it must **not** install `ContentNegotiation`, the envelope parser, or api-key
  injection, and it must set `followRedirects = false` plus a cookie store:

  ```kotlin
  @[Factory WebSessionHttpClient]   // Factory, not Single — one session per onboarding attempt
  fun provideWebSessionClient(...): HttpClient = HttpClient(engine) {
      followRedirects = false
      expectSuccess = false
      install(HttpCookies) { storage = AcceptAllCookiesStorage() }
  }
  ```

  This mirrors TaigaMobileNova's `@AuthHttpClient` qualifier split. Making it a `@Factory` means
  the session cookie dies with the onboarding attempt rather than lingering in a singleton.

  **The `@Factory` has to run the whole depth of the chain**, or it buys nothing: a `@Single`
  `WebLoginApi` resolves the client once and holds that cookie jar for the life of the process,
  and a `@Single` `SetupRepository` does the same one level up. `WebLoginApiImpl` and
  `SetupRepositoryImpl` are therefore both `@Factory` — the one place in the app where a
  repository isn't a singleton, and the reason is the session, not the state.

- **Clear the stored key before starting an attempt.** `WallosApiClient` injects the *stored* key
  over any `api_key` the caller put in the `FormParams` (§4.1), so a leftover key is what the
  validation call would actually validate — and a newly scraped key would then be stored on the
  strength of the old one. `clear()` keeps the server URL (§4.7), so this costs nothing.

- **Detect whether password login is even available.** `password_login_disabled` (admin setting or
  the `OIDC_DISABLE_PASSWORD_LOGIN` env var) strips the credential fields from the form, and
  OIDC-only instances can't be bridged at all. GET `login.php` during setup and check for the
  password input; if absent, show Path B only. **OIDC cannot be bridged** — it is a redirect dance
  against a third-party IdP whose registered `redirect_url` points at the Wallos web app.

- **Never persist the password.** Exchange it for the key and drop it. Only the API key reaches
  storage.

- **Warn on non-HTTPS base URLs.** Self-hosted instances are often plain HTTP on a LAN, and
  POSTing a password over cleartext is materially worse than pasting a pre-existing key. Path B
  should be the recommended option on an insecure origin.
  **The manifest carries `usesCleartextTraffic="true"` (1.11)** — plain HTTP is the common
  deployment and the app cannot reach one without it — so nothing in the platform is holding this
  line. The warning is the only mitigation there will be.
  **Shipped in 3.1**, as `LoginUiState.isCleartextWarningVisible`: it is **advisory** — it never
  disables Connect, because the instance every on-device `Verify:` line uses is plain HTTP — and it
  is shown on the **password path only**, since taking the steering to Path B is what resolves it.
  Only a literal `http://` prefix counts; the app infers no scheme anywhere.

- **Client-side backoff on failed logins.** The server has no rate limiting and no lockout
  anywhere. Without our own throttle we are shipping a brute-force tool pointed at the user's
  server.

- **Don't use `endpoints/user/regenerateapikey.php`** even though it returns clean JSON: it mints
  a *new* key and silently invalidates the user's other integrations (Home Assistant, iCal
  subscriptions, a second device), and it additionally requires a scraped CSRF token. Scraping
  `profile.php` is both less work and non-destructive.

Unchanged by all of the above, because the runtime credential is still just the key:

- **No logout, only "disconnect"** — clear the stored key. Nothing is invalidated server-side.
- **Key rotation is invisible to the app.** If the user regenerates the key in the web UI, the only
  signal is the next call failing. That is why the `Unauthenticated` vs `Forbidden` vs `NotFound`
  split in §4.3 must be right — it is the sole trigger for sending the user back to setup.
- **Errors still attribute to a field by failure layer** (API doc §5.1): a non-JSON body / 404 /
  HTML means the **URL** is wrong; a well-formed `{success: false, title: "Invalid API key"}`
  means the URL is right and the **key** is wrong.
- **Multi-instance support stays nearly free** — an account is a (base URL, key) pair with no
  session state.

---

## 2. Target project structure

Mirrors `TaigaMobileNova`: `androidApp/` (Android entry point) → `composeApp/` (KMP library that
aggregates features and owns navigation) → `feature/` → `core/` → `utils/`.

```
build-logic/convention/          Gradle convention plugins
androidApp/                      Android application module (entry point, Application class)
composeApp/                      KMP library: DI root, drawer shell, NavDisplay + entry providers
uikit/                           Shared Composables, theme, widgets/topappbar (§5.4)
strings/                         CMP string resources (strings.xml + RString)
testing/                         Fakes, model factories, MainDispatcherRule
core/
  api/                           Ktor client, WallosApiClient, envelope parsing, error mapping
  domain/                        WallosError, Result extensions, platform network errors
  storage/                       DataStore (base URL, API key), Room cache, NetworkMonitor
  crud/                          Generic add/edit/delete contract shared by catalog resources
  navigation/                    nav3: NavigationState, Navigator, toEntries() — see §5
  serialization/                 Custom serializers (see §4.4)
  async-kmp/                     Dispatcher qualifiers
  appinfo-api/                   Build info (isDebug, version, debug host)
  logger/                        logcat() + platform loggers
utils/
  ui/                            NativeText, ObserveAsEvents, SnackbarDelegate
  formatter/decimal/             Money parsing & currency formatting
  formatter/datetime/            YYYY-MM-DD parsing & formatting
feature/
  setup/          data domain dto ui      Onboarding: login bridge + manual key entry (§1.1)
  subscriptions/  data domain dto mapper ui   List, detail, add/edit/delete
  dashboard/      data domain dto ui      Monthly cost, period budget, upcoming payments
  categories/     data domain dto ui      \
  currencies/     data domain dto ui       |  Reference data. Identical CRUD shape —
  paymentmethods/ data domain dto ui       |  see §3.4 on core:crud
  household/      data domain dto ui      /
  settings/       ui (data domain dto later)  Disconnect stub in v1; display settings in Phase 5
  profile/        data domain dto ui      get_user, set_budget
  notifications/  data domain dto ui      Read-only channel config
  admin/          data domain dto ui      Deferred — see §8, Phase 6
```

### Modules to delete in Phase 0

The current tree is the untouched KMP wizard output. `desktopApp/`, `iosApp/` and the
`Greeting`/`Platform` sample files carry no content worth keeping; `shared/` becomes
`composeApp/`. Re-adding the Desktop and iOS entry points later is one wizard run each.

---

## 3. Build setup

### 3.1 Convention plugins (`build-logic/`)

Port the six from TaigaMobileNova, renamed to a `wallosmobile.*` prefix:

| Plugin | Role |
|---|---|
| `wallosmobile.android.application` | AGP application module + Compose + Koin compiler plugin |
| `wallosmobile.kmp.library` | KMP base: targets, coroutines, collections, datetime, `core:logger`, tests, linting |
| `wallosmobile.kmp.library.compose` | Adds Compose Multiplatform + enables `androidResources` for the CMP asset pipeline |
| `wallosmobile.kmp.di` | `io.insert-koin.compiler.plugin` + Koin BOM/annotations. **Never KSP for DI.** |
| `wallosmobile.kmp.network` | Ktor with per-platform engines |
| `wallosmobile.kmp.serialization` | kotlinx.serialization |

The **single edit point for platform targets** is `configureKmp()` in
`build-logic/convention/src/main/kotlin/.../KmpConfiguration.kt`. `jvm()`, `iosArm64()` and
`iosSimulatorArm64()` go there, and nowhere else, when those apps come back. Nothing outside
`build-logic` mentions a target.

For the Android-only phase there is **no literal `androidTarget()` call**: KMP library modules get
their Android target from `com.android.kotlin.multiplatform.library`, applied and configured in
`KmpLibraryConventionPlugin`. So "Android-only" means `configureKmp()` declares *no* targets at
all — it is still the single edit point, but the Android one arrives via the AGP plugin.

Two things that plugin does **not** do for free, both of which silently disable a quality gate
rather than failing (and both latent in the reference projects, which don't hit them because they
have a `jvm()` target):

- **It creates no host-test compilation.** Without `withHostTestBuilder {}.configure { … }` in
  `KmpLibraryConventionPlugin`, `commonTest` belongs to no compilation, the test dependencies are
  inert, and **no test task exists at all** — not a failing one, none. The unit test task is
  `testAndroidHostTest`; there is no `jvmTest` and no `testDebugUnitTest`.
- **detekt's default source set is `src/main/{java,kotlin}`**, which no KMP module has, so every
  `:module:detekt` reports `NO-SOURCE` and lints nothing. `configureLinting()` sets
  `source.setFrom(layout.projectDirectory.dir("src"))` to cover `commonMain`, `commonTest` and any
  platform source set.

**Discipline that makes this cheap:** no `androidMain` source set in feature modules. If a feature
needs a platform capability, it declares an `expect` in `commonMain` and the `actual` lives in
`androidMain` of that module — so adding a target surfaces as compile errors listing exactly what
is missing, rather than silently-Android code that has to be untangled.

### 3.2 Versions

Adopt TaigaMobileNova's `libs.versions.toml` wholesale rather than the wizard's — it is newer and
the conventions are written against it. Notable deltas from the current scaffold:

- AGP `9.0.1` → `9.3.1`, Java 11 → **JDK 21** (`jvmToolchain(21)`), `compileSdk` 36 → 37
- **Gradle wrapper `9.1.0` → `9.6.1`** — AGP 9.3.1 requires Gradle ≥ 9.5, and both reference
  projects are already on 9.6.1
- Add: Koin + koin-compiler-plugin, Ktor, Room + BundledSQLiteDriver, DataStore, Coil 3,
  BuildKonfig, Timber, detekt, ktlint, kover, compose-rules
- **Navigation 3 comes from MealieMobile's catalog, not Taiga's** — Taiga is on nav2. Take
  `jetbrains-navigation3-ui`, `jetbrains-lifecycle-viewmodelNavigation3` and
  `jetbrains-androidx-savedstate` (§5.1), and do *not* port Taiga's `jetbrainsNavigationCompose`.
- `minSdk` stays 24

Keep the `# https://github.com/.../releases` comment above each version — Taiga's catalog does
this and it makes upgrades a skim rather than a search.

### 3.3 Module templates

Every module of a given layer gets the same plugin set. This is identical in TaigaMobileNova and
MealieMobile, so it ports verbatim — only the plugin prefix changes.

| Module | Plugins |
|---|---|
| `feature:*:ui` | `kmp.library` + `kmp.library.compose` + `kmp.di` + `kmp.serialization` |
| `feature:*:data` | `kmp.library` + `kmp.di` + `kmp.network` (+ `kmp.serialization` if it builds JSON itself) |
| `feature:*:domain` | `kmp.library` (+ `kmp.di` only if it holds injected use cases) |
| `feature:*:dto` | `kmp.library` + `kmp.serialization` |
| `feature:*:mapper` | `kmp.library` + `kmp.di` |
| `uikit`, `strings` | `kmp.library` + `kmp.library.compose` |
| `utils:ui` | `kmp.library` + `kmp.library.compose` + `kmp.di` + `kmp.serialization` |
| `core:api` | `kmp.library` + `kmp.di` + `kmp.network` + `kmp.serialization` |
| `core:navigation` | `kmp.library` + `kmp.library.compose` |
| `testing` | `kmp.library` + `kmp.serialization` + `kmp.network` |

Standard dependency sets:

```kotlin
// feature/NAME/ui
commonMain.dependencies {
    implementation(projects.strings)
    implementation(projects.uikit)
    implementation(projects.utils.ui)
    implementation(projects.core.navigation)
    implementation(projects.feature.NAME.domain)
    implementation(libs.jetbrains.compose.icons.extended)   // only for icons outside the core set
}

// feature/NAME/data
commonMain.dependencies {
    implementation(projects.core.api)
    implementation(projects.core.domain)
    implementation(projects.core.storage)
    implementation(projects.core.asyncKmp)
    implementation(projects.feature.NAME.domain)
    implementation(projects.feature.NAME.dto)
}

// feature/NAME/mapper
commonMain.dependencies {
    // `api`, not `implementation`: the two ends of every mapper signature (2.3). A `data` module
    // that has the mapper can then name both without re-declaring either.
    api(projects.feature.NAME.domain)
    api(projects.feature.NAME.dto)
    implementation(projects.core.domain)
}
```

Four details that are easy to miss and annoying to diagnose:

- **`ui` modules need `kmp.serialization`** even when they parse nothing — routes are
  `@Serializable ... : NavKey`.
- **`uikit` and `strings` must expose their generated resource class.** Without this, `RDrawable`
  and `RString` aren't visible to other modules:
  ```kotlin
  compose.resources {
      packageOfResClass = "com.grappim.wallosmobile.uikit.generated.resources"
      generateResClass = always
      publicResClass = true
  }
  ```
  **Both** modules additionally need `api(libs.jetbrains.compose.components.resources)`, and `api`
  rather than `implementation` so consumers can resolve `StringResource` / `DrawableResource`
  themselves. It is not optional in either module: `generateResClass = always` emits a `Res` class
  referencing `org.jetbrains.compose.resources.*`, so without the dependency the module fails to
  compile on its own *generated* source.
- **`:strings` exposes `RPlurals` (`Res.plurals`) beside `RString`.** CMP supports `<plurals>`,
  and `pluralStringResource(RPlurals.x, count, count)` takes the count **twice** — once to select
  the form, once as the `%1$d` argument (2.4).
- **`core:navigation` takes the Compose plugin here**, unlike Taiga's (which holds only
  extension functions). Ours holds `NavigationState` and `toEntries()`, which are `@Composable`.
- **`uikit` has no `androidMain`.** Mealie's `expect fun colorScheme(darkTheme)` exists only to
  reach `dynamicDarkColorScheme(LocalContext)`, which is Android-only. WallosMobile takes a static
  Material 3 palette seeded from the logo navy `#233E67` instead — it keeps the brand, and the
  whole module stays in `commonMain`. Dynamic colour means putting the `expect`/`actual` back.
  Note also that the generated `Res` class declares empty `drawable`/`string`/`array`/`plurals`/
  `font` objects whether or not the module has any such resource, so `RDrawable` compiles in a
  `uikit` with no drawables at all.

- **`uikit` depends on `utils:ui` as `api`.** `TopBarConfig` carries `NativeText` in its public
  signature (§5.4), so every consumer of `uikit` resolves `NativeText` transitively and should not
  list `utils.ui` a second time.

- **`utils:*` is the bottom of the stack, so it cannot see a domain type.** Anything phrased as
  "format *this domain enum* for the user" belongs to the screen, not to a formatter: the text for
  `BillingCycle` + `frequency` ("every 6 months") started life in `utils:formatter:datetime` and
  moved to `feature:subscriptions:ui` in 2.2, because `BillingCycle` lives in that feature's
  `domain`. Duplicating the enum down here to keep the function would buy one string and a mapping
  to maintain; a plural resolved with `pluralStringResource` where it is rendered costs nothing.
  The formatters take primitives and `kotlinx.datetime` types only.

Everything else — coroutines, immutable collections, datetime, `core:logger`, `kotlin("test")`,
Turbine and `:testing` — arrives through `kmp.library`/`configureTests()`. Modules never declare
those by hand. The same goes for the Compose set in `configureKmpCompose()`, which carries
**material-icons-core**: material3 does not bring it transitively, so without it `Icons.Filled.*`
is unresolved — while `ui-graphics` and `animation` *do* arrive via `compose.ui` and
`compose.foundation` and need no entry of their own. That core artifact is only ~50 icons, and
several obvious ones are absent (`Subscriptions`, `Payment`, `Add`, `Visibility`/`VisibilityOff`);
`ArrowBack`, `List` and `Send` are `Icons.AutoMirrored.Filled.*`. Three of three screens so far
wanted an icon that isn't there, so **assume it's missing and check** — a `TextButton` with a word
in it is often the cheaper answer (the login password toggle is Show/Hide text for exactly this
reason). A module that genuinely needs more declares
`jetbrains.compose.icons.extended` itself (the `feature/NAME/ui` block above), rather than
growing the convention plugin for one screen.

### 3.4 `core:crud` — the one deliberate deviation

Categories, currencies, household members and payment methods are four separate feature modules
(matching Taiga's granularity, since each gets its own screen), but their API contract is
byte-identical: `get_*.php` returning `[{id, name, …, in_use}]`, and `set_*.php` with
`action=add|edit|delete`, differing only in field names and the ID parameter alias.

`core:crud` holds that shape once:

```kotlin
interface CrudResource {
    val id: Int
    val name: String
    val inUse: Boolean
}

interface CrudApi<T : CrudResource> {
    suspend fun getAll(): List<T>
    suspend fun add(fields: FormParams): Int
    suspend fun edit(id: Int, fields: FormParams)
    suspend fun delete(id: Int)
}
```

Each feature supplies its endpoint path, ID parameter alias and DTO. This keeps four modules'
data layers at roughly 30 lines each instead of 150, and gives one place to encode the
"deleting an in-use item fails with `<Resource> in use`" rule.

### 3.5 CI

The build workflow — `.github/workflows/ci.yml`, one job, on push and PR to `master` (§3.6 adds a
second workflow, which runs no Gradle at all):

```
./gradlew :androidApp:assembleDebug
./gradlew allTests
./gradlew detekt ktlintCheck
```

Setup is JDK 21 (temurin) + `gradle/actions/setup-gradle` (cache *and* wrapper validation in one
action, unlike Taiga's separate `wrapper-validation` + `actions/cache` steps) +
`android-actions/setup-android`. No composite action: Taiga extracts one because it runs two
workflows, and there is nothing here to share it with. No secrets either — WallosMobile has no
flavors, no signing configs and no `google-services.json`, so a debug build needs nothing
restored.

Two deliberate omissions: **Kover/Codecov is not in CI** (the upload wants a `CODECOV_TOKEN` this
repo doesn't have; `koverXmlReport` stays a local command), and `paths-ignore` skips `**.md` and
`docs/**`, so a **docs-only commit produces no run** — an absent run is not a failed one.

A third omission arrived with the Room cache and was not planned: **`allTests` does not fan out to
device tests**, and this job has no emulator, so `:core:storage:connectedAndroidDeviceTest` (§4.7)
is not a CI gate. It is the first suite in the project that isn't. Growing an emulator job is the
obvious answer and is deferred to whenever the second such suite lands — the instrumented Compose
tests §6.1 parks.

### 3.6 Guardrails — the second workflow

Everything in §3.4 constrains the *code* a session writes. Nothing constrains a session from
widening a gate so its own step passes: relaxing a detekt rule, adding an `@Ignore`, editing a
convention plugin, deleting a rule from `CLAUDE.md`. `.github/workflows/guardrails.yml` runs
`.github/scripts/check-guardrails.sh` over the pushed range and fails any commit that trips a
wire without carrying a `Gate-change: <what, and why>` line in its message. It is an **opt-in,
not a veto** — widening a gate is often correct, and the point is only that it can't happen
silently.

Three things about its shape, each of which the alternative gets wrong:

- **It is a separate workflow because `ci.yml` has `paths-ignore`.** A commit that only edits
  `CLAUDE.md` or `docs/CHECKLIST.md` produces no CI run at all (§3.5) — and those two files are
  the highest-leverage thing a session can quietly change, since they set the rules every later
  session runs under. So `guardrails.yml` carries no `paths-ignore`.
- **Paths trip on any touch; the two rule documents trip on *structure*.** `.github/`,
  `build-logic/`, `config/detekt/`, `.editorconfig` and `gradle/libs.versions.toml` are files
  ordinary feature work never opens, so touching one is signal on its own. `CLAUDE.md` and
  `docs/CHECKLIST.md` are the opposite — every step edits them — so gating any edit would fire on
  every reflowed bullet and train everyone to add the marker reflexively. Instead the script
  counts the `- ` bullets under `## Non-negotiables` and the `- [ ]`/`- [x]` step boxes, and trips
  only when a count **drops**. Over all 35 commits of history to v1 those counts are monotonic —
  0→10 rules, 25 steps throughout — so the check has no false positives on real work, while
  ticking a box, appending a `Note:` and rewording a rule all stay free.
- **It cannot survive its own deletion.** GitHub runs the workflows present *in the pushed
  commit*, so a commit that deletes `guardrails.yml` produces no guardrails run to object. Branch
  protection requiring the check is the only fix, and this repo pushes straight to `master`. What
  the job actually buys is the realistic failure mode — a session taking the path of least
  resistance — not an adversarial one.

Locally: `.github/scripts/check-guardrails.sh HEAD~1..HEAD`.

---

## 4. `core:api` — the load-bearing module

### 4.1 `WallosApiClient`

Ktor's `ContentNegotiation` deserializes straight from the byte stream, which cannot survive
either the PHP diagnostics prefix or a `200 OK` carrying `success: false`. So feature APIs never
call `HttpClient` directly. They call `WallosApiClient`, a `@Single` that owns the key injection
and the parse pipeline:

```kotlin
@Single
class WallosApiClient(
    private val httpClient: HttpClient,
    private val apiKeyStorage: ApiKeyStorage,
    private val envelopeParser: WallosEnvelopeParser,
) {
    suspend inline fun <reified T> post(path: String, params: FormParams = FormParams()): T {
        val response = httpClient.submitForm(
            url = path,
            formParameters = params.withApiKey(apiKeyStorage.getKey()).build(),
        )
        return envelopeParser.parse(response.status.value, response.bodyAsText())
    }

    // logo / icon upload only
    suspend inline fun <reified T> postMultipart(path: String, params: FormParams, file: FileUpload): T
}
```

**That sketch doesn't compile as written.** A public `inline` member cannot touch `private`
constructor properties, so `httpClient`, `apiKeyStorage` and `envelopeParser` must be
`@PublishedApi internal val`, or `post` must split into a non-inline core taking a
`DeserializationStrategy<T>` with a thin reified wrapper. `WallosEnvelopeParser` hit the same
wall in 1.2 and took the second route (§4.2); `WallosApiClient` followed it.

Everything is a form-encoded POST — one shape for the whole API, and the key never reaches
server access logs, browser history or referrers.

`path` is **relative and carries no leading slash** (`api/status/version.php`) — a leading slash
discards whatever subpath the user's install lives under. `withApiKey(null)` **omits** the
parameter rather than sending an empty one, so a caller with no stored key gets the server's
`Missing API key` → `Unauthenticated` → back to setup, which is where it belongs. A corollary the
onboarding bridge depends on: while nothing is stored, a `FormParams` carrying its own `api_key`
survives, so a freshly scraped key can be validated through this client before it is persisted.
**"While nothing is stored" is load-bearing** — `withApiKey` `put`s, so a stored key *overwrites*
a caller's own `api_key` rather than losing to it. `SetupRepository` therefore clears the stored
key before it starts an attempt (§1.1); without that, a re-login validates the stale key and
stores the new one on the strength of it.

The `NetworkModule` itself follows MealieMobile's
(`MealieMobile/core/api/.../core/api/NetworkModule.kt`) — `@HttpJson` and client qualifiers,
`@Module @Configuration @ComponentScan`, `expectSuccess = false`, `defaultRequest` off
`BaseUrlProvider`, logcat-backed `Logging`. **Five things change for Wallos**, and each is a bug
if it's copied over unaltered:

1. **No `AuthHeaderPlugin`, and no second "auth client".** There's no auth endpoint and no header
   credential. The two clients are the API client (`@Single`) and `@WebSessionHttpClient`
   (`@Factory`, onboarding only — §1.1).

2. **Drop `ContentNegotiation` entirely.** Requests are form-encoded (`submitForm` sets its own
   content type) and responses are read via `bodyAsText()` so the envelope parser can strip the
   PHP diagnostics prefix. Nothing in the pipeline decodes from the byte stream, so
   `ContentNegotiation` is dead weight — and leaving it installed invites someone to call
   `.body<T>()` and bypass the parser. For the same reason, **do not** set
   `defaultRequest { contentType(ContentType.Application.Json) }`; Wallos wants
   `application/x-www-form-urlencoded`.

3. **`Logging` at `LogLevel.ALL` leaks the API key.** Mealie and Taiga carry their token in a
   header, which Ktor can sanitize; Wallos puts `api_key` in the **body**, which
   `LogLevel.ALL`/`BODY` writes to logcat verbatim on every single request. Use a logger that
   redacts `api_key=[^&]*`, and gate the level on build type:

   ```kotlin
   install(Logging) {
       logger = RedactingLogger(tag = "Ktor")   // strips api_key= and password=
       level = if (appInfoProvider.isDebug()) LogLevel.ALL else LogLevel.NONE
   }
   ```

4. **Scope `HttpRequestRetry` to reads.** Mealie retries every request once on exception. For
   Wallos that is unsafe: `set_subscriptions.php` with `action=add` is a non-idempotent POST, and
   a retry after a response-side failure creates a duplicate subscription. Restrict the retry
   predicate to `get_*.php` paths, or install it only where reads happen. In Ktor 3.5.1 that is
   `retryOnExceptionIf(maxRetries = 1) { request, _ -> … }` — it has **no `retryOnTimeout`**
   parameter, and `request` is an `HttpRequestBuilder` whose `url` is a `URLBuilder`, which
   exposes `encodedPathSegments` and **not** `encodedPath`. Keep the predicate in a top-level
   `internal` function: the `NetworkModule` itself is untestable and Kover-excluded, so anything
   left inside the config lambda is unverifiable.

5. **`BaseUrlProvider` returns the instance root**, not an `/api/` prefix — the login bridge hits
   `/login.php` and `/profile.php` at the root while API calls use `api/…` paths. Normalize
   trailing slashes and preserve subpaths (`https://host/wallos/`). The **trailing slash is
   load-bearing**: Ktor's `DefaultRequest` only appends a relative request path to the default
   URL's path when that path ends in `/`, so without it `https://host/wallos` + `api/…` resolves
   to `https://host/api/…`.

`core:api` therefore depends on `core:storage` for two interfaces — `ApiKeyStorage.getKey()` and
`ServerUrlStorage.serverUrl` — and on `core:appinfo-api` for `AppInfoProvider.isDebug()`.
`serverUrl` cannot be `suspend`, because `defaultRequest`'s block is not a suspend context; the
DataStore implementation has to keep it cached. `ktor-client-logging` is declared by this module
rather than by the `kmp.network` convention plugin, since nothing else installs `Logging`.

The explicit platform engine (`HttpClient(createPlatformHttpClientEngine(...))`, as in Taiga
rather than Mealie's autodiscovery) is only needed once certificate trust lands — Phase 2b. v1 can
use `HttpClient { }`, but expect that signature to change then.

### 4.2 `WallosEnvelopeParser`

A pure class with no Ktor dependency, which makes it trivially unit-testable — the same role
`ErrorResponseParser` plays in TaigaMobileNova, and the highest-value test target in the project.
It implements, in order:

1. HTTP `404` → `WallosError.UnsupportedEndpoint` (endpoint absent on this Wallos version — a real
   nginx 404, not an API error). HTTP `5xx` → `WallosError.Server`.
2. Find the first `{`. Nothing before it is JSON. If the prefix is non-empty, **log it and
   continue parsing the remainder** — this is the `display_errors` case (§Layer 2 of the API doc),
   where valid JSON follows an HTML warning. No `{` at all → `WallosError.Malformed`.
3. `success != true` → build the detail as `message ?: notes?.firstOrNull() ?: title` (the field
   carrying the human-readable text differs per endpoint) and map via §4.3.
4. Otherwise decode `T` from the envelope. A body that parses as JSON but doesn't match `T` —
   a self-hosted instance at an older migration level, say — becomes `WallosError.Malformed` as
   well, so that **everything leaving `core:api` is a `WallosError`** and no raw
   `SerializationException` reaches a repository.

The signature is `parse(statusCode: Int, body: String, deserializer: DeserializationStrategy<T>)`,
with a top-level `inline fun <reified T> WallosEnvelopeParser.parse(statusCode, body)` for the
call sites. The reified form cannot be a *member*: a public `inline` function may not read the
class's private state, and the parser holds its `Json` privately. The same constraint applies to
`WallosApiClient.post` in §4.1.

Because the parser decodes the **whole envelope**, `T` is an envelope type — `SubscriptionsResponse`,
`CurrenciesResponse` — and each feature's `dto` module owns its own. Those wrappers get **no default
values**, unlike the row DTOs, which default every column that a migration might not have added yet
(2.1). The distinction is what the field means: a missing *column* is an older instance, while a
missing *envelope key* is a response this app doesn't understand. An empty user really does get
`"subscriptions":[]`, so `Malformed` is a truer answer there than a silently empty screen.

That `Json { ignoreUnknownKeys = true; isLenient = true }` belongs to the parser and is not a DI
binding. With `ContentNegotiation` dropped (§4.1), the parser is the only JSON consumer in the
app — Mealie's `@HttpJson` qualifier exists to configure `ContentNegotiation` and has nothing to
configure here.

### 4.3 Error mapping

`WallosError` as a sealed class in `core:domain`, mapped by title per §5.6 of the API doc. The
**mapping function is not in `core:domain`** — it ships next to `WallosEnvelopeParser` in
`core:api`, the only caller, so `core:domain` stays free of API knowledge. Two rules are worth
encoding as tests, because getting either wrong is a user-visible bug:

- `Invalid API key`, `Unauthorized`, `Missing API key` → **`Unauthenticated`**: clear the stored
  key and return to setup.
- `Invalid user`, `Forbidden`, `Denied. Not admin user` → **`Forbidden`**: keep the key.
- `Unauthorized or Not Found` → **`NotFound`**. Despite the name this is a per-row ownership
  check on catalog resources. Clearing the key here logs the user out at random.

Turn the auth-failure table (API doc §5.3) into a parameterized test — it is eleven rows of
endpoint-specific inconsistency and the only defense against regression is asserting it directly.

`resultOf` and `mapResult` (also `core:domain`) are Taiga's with two fixes: `mapResult` goes
through `fold`, because Taiga's `getOrNull() != null` check turns a *successful* `null` into a
failure; and there is no separate `catch (TimeoutCancellationException)`, since it is a
`CancellationException` and the earlier clause already rethrows it.

### 4.4 Encoding quirks worth a type

Three inconsistencies in the API become three silent bugs unless they are made unrepresentable:

```kotlin
class FormParams {
    fun flag(key: String, value: Boolean)          // "1" / "0"    — notify, inactive, auto_renew
    fun literalTrue(key: String, value: Boolean)   // "true" only  — convert_currency, disabled_to_bottom
    fun date(key: String, value: LocalDate)        // strict YYYY-MM-DD
}
```

`Boolean.toString()` is never correct for this API — except, by coincidence, for `literalTrue`,
which writes `"false"` rather than omitting the key (the API doc reads anything but `"true"` as
false). Alongside those three: a plain `put(key, value)`, and `asMap()`. `withApiKey()` and
`build()` from §4.1 are **not** part of this class in `core:api`'s pure layer — they return Ktor
`Parameters`, so they arrive with the HTTP clients.

Similarly on the decoding side (no `core:serialization` module exists — 1.2 needed none): `notes` is
an array everywhere except `get_user.php`, where it is `""` — either a lenient serializer or
`ignoreUnknownKeys` plus not modelling it. `Json { ignoreUnknownKeys = true; isLenient = true }`
is **mandatory**, not a nicety: responses are raw DB rows and self-hosted instances sit at
different migration levels.

### 4.5 Self-signed certificates

Port `CompositeTrustManager` + `TrustedCertStorage` + the trust-prompt flow from TaigaMobileNova
(`docs/private-cert-trust` there) essentially unchanged. This matters *more* for Wallos than for
Taiga: nearly every instance is self-hosted behind a homelab certificate. Certificate pinning is
not an option; a trust prompt on first connect is.

### 4.6 Version gating

Store the `api/status/version.php` result at setup and after each successful reconnect. Gate
`get_period_budget`, `set_budget`'s period fields, `logo_variant` and `square_icons` behind it —
older servers 404 or silently omit fields. `WallosError.UnsupportedEndpoint` is the runtime
backstop when the stored version is stale.

### 4.7 `core:storage`

Grouped here because §4.1 defines the two interfaces `core:api` consumes and §4.5 adds a third.
One `PreferenceDataStoreFactory` store, file `wallos_storage`, shared by every storage class in
the module; each owns its keys, and **`ApiKeyStorage.clear()` removes its own key rather than
clearing the file**, so disconnect leaves the server URL the user typed.

- **The API key is encrypted before it reaches DataStore**, which writes plaintext to disk. The
  seam is a `SecretCipher` *interface* (`encrypt`/`decrypt`), not an `expect`/`actual`: the
  Android implementation is AES/GCM against a key that never leaves the Android Keystore, and the
  Keystore does not exist in a host test — as an interface it can be faked, which keeps
  `ApiKeyStorageImpl` in `commonMain` and testable. `decrypt` returns `null` rather than throwing
  when the ciphertext no longer reads back (restored backup, invalidated key); that surfaces as
  "no key stored", and the startup branch sends the user to onboarding.
- **The module's `@Module @Configuration @ComponentScan` class lives in `androidMain`**, unlike
  every other module's, because the DataStore file path needs a `Context`. Android being the only
  target, the `androidMain` compilation contains the `commonMain` sources too, so that one scan
  picks up the `commonMain` implementations — verified in the generated bytecode. It does mean
  `startKoin` must install `androidContext()`. A second target splits this into an
  `expect class PlatformStorageModule` the way TaigaMobileNova does.
- **`NetworkMonitor` is a seam of the same kind as `SecretCipher`**, and for the same reason:
  `ConnectivityManager` does not exist in a host test. Interface in `commonMain`, a `@Single`
  `NetworkMonitorImpl(context)` in `androidMain`, `FakeNetworkMonitor` in `:testing`. It tracks
  the **set** of networks with `NET_CAPABILITY_INTERNET` rather than `activeNetwork`, because
  `onLost` for Wi-Fi while mobile data is up is not offline — the emulator reproduces exactly that
  on an airplane-mode flip. `ACCESS_NETWORK_STATE` is declared in `:androidApp`'s manifest, the
  only one in the repo, hence the `@SuppressLint("MissingPermission")` on a class that cannot see
  it. It answers "is there a network at all", never "is the Wallos instance reachable" — a
  self-hosted server on a LAN the phone has left is online here and still fails.
- **`ServerUrlStorage.serverUrl` is not `suspend`** (§4.1): the implementation blocks on the first
  read via `runBlocking`, then serves an in-memory cache that `saveServerUrl` keeps current.
  `runBlocking` is reachable from `commonMain` here only because Android is the sole target and
  the metadata compilation therefore resolves the JVM variant of coroutines.

#### The Room cache

`WallosDB` (`db/`) holds `SubscriptionEntity` and `CurrencyEntity` with a DAO each — a snapshot of
the server, never a source of truth, so there is no dirty-write state to reconcile. `version = 1`,
`exportSchema = true`, schema JSON committed under `core/storage/schemas/`, and the builder
drops the tables on a schema change: pre-v1 there is nothing to migrate from, and afterwards the
cache is still one refresh away from being rebuilt. KSP and the Room Gradle plugin are applied to
this module alone, and Android being the only target reduces Taiga's four `add("ksp…")` lines to
one `add("kspAndroid", …)`.

- **Entities are SQLite primitives only — no `TypeConverter`.** `cycleCode` is the raw wire code
  rather than a `BillingCycle`, and the two dates are ISO-8601 text. That is what lets
  `core:storage` depend on **no feature module**: TaigaMobileNova's `core:storage` takes
  `feature:projects:domain` for its entity field types, which here would invert the
  `feature/` → `core/` direction §2 sets out. The entity↔domain mapper lives in
  `feature:subscriptions:mapper` with the wire mappers.
- **`currencySymbol` is stored resolved on the subscription row**, denormalised from the currency
  table, so reading the cached list is one query with no join. The currency table is cached for
  the *next* refresh's resolution — §7.2's second round trip per call — not for this read.
- **`replaceAll` is a `@Transaction` delete-then-insert on both DAOs**, a snapshot rather than a
  merge: the API sends the whole list in one response, so a row missing from a fresh fetch has
  been deleted server-side and must not survive locally.
- **A DAO test cannot be a host test, and this module therefore has a device-test compilation.**
  Two independent blockers: on the Android target the only `Room` builders take a `Context`, and
  `BundledSQLiteDriver`'s `libsqliteJni.so` ships inside the aar's `jni/`, which is not on a host
  test's classpath. Robolectric would bridge both and is ruled out (§6.1), so `core:storage`
  declares `withDeviceTestBuilder { sourceSetTreeName = null }` in its own build file — not in
  `build-logic`, because it is the only module that needs one. `sourceSetTreeName = null` is
  load-bearing: the default puts `androidDeviceTest` in the `test` source-set tree, which would
  compile and run `commonTest` on the device as well. The task is
  `:core:storage:connectedAndroidDeviceTest`, and §8 covers what that costs.

---

## 5. Navigation — Navigation 3

TaigaMobileNova is on Navigation 2 (`NavController` + `NavHost` + nav graphs). **This project uses
Navigation 3**, ported from `MealieMobile`, whose reference doc lives at
`/home/gregory/proj/grappim/MealieMobile/docs/kmp-nav3.md`. Read that first — this section covers
only what is specific to WallosMobile plus the corrections found while reading Mealie's actual
sources.

### 5.1 Artifacts

**Rule: never put `androidx.navigation3:*` in `commonMain`.** Nav3 has two artifact families
publishing under the *same* package names — imports are identical, only the Gradle coordinate
differs:

| Group | Platforms | Source set |
|---|---|---|
| `org.jetbrains.androidx.navigation3` | Android + iOS + Desktop | `commonMain` |
| `androidx.navigation3` | Android only | `androidMain` only |

Add to `configureKmpCompose()` in `build-logic`, so every feature `ui` module gets nav3 by
applying `wallosmobile.kmp.library.compose`:

```kotlin
implementation(libs.findLibrary("jetbrains.navigation3.ui").get())
implementation(libs.findLibrary("jetbrains.lifecycle.viewmodelNavigation3").get())
implementation(libs.findLibrary("jetbrains.androidx.savedstate").get())
```

Versions: `jetbrainsNav3 = "1.1.1"`, `jetbrainsComposeLifecycle = "2.11.0"`,
`jetbrainsSavedState = "1.4.0"` — matching MealieMobile's current catalog.

### 5.2 Where the pieces live

MealieMobile keeps all of nav3 inside `composeApp/nav/`. WallosMobile splits it, because
`Navigator` and `NavigationState` are pure Kotlin with no route imports and are therefore directly
unit-testable:

```
core/navigation/                  NavigationState, Navigator, toEntries(), rememberNavigationState
composeApp/.../
  WallosAppContent.kt             theme + the startup branch; MainActivity calls only this
  MainAppState.kt                 back stacks + drawer state + current RouteConfig
  AuthenticatedMainScreen.kt      the shell: drawer, top bar, NavDisplay, back handler
  widget/WallosDrawerWidget.kt    the drawer sheet
  nav/
    NavKeySerializers.kt          polymorphic SerializersModule + SavedStateConfiguration
    DrawerDestination.kt          top-level routes + start destination
    DrawerItem.kt                 Destination / Group / Divider + IconSource
    DrawerItemsBuilder.kt         the drawer's item list
    RouteConfig.kt                per-route shell config (drawer gestures; FAB from Phase 3)
    MainNavHost.kt                NavDisplay + entryProvider wiring
    entries/                      one file per feature: subscriptionsEntry(), settingsEntry(), …
```

`rememberNavigationState` takes the `SavedStateConfiguration` as a parameter rather than building
it, which is what keeps `core:navigation` free of route imports.

The dual back stack is in from v1, because the drawer needs it (§5.4) — a single
`rememberNavBackStack` would not survive the second drawer destination. It is a port, so the cost
is low.

### 5.3 Routes and entry providers

Routes are `@Serializable` `NavKey` values declared **in the feature's own `ui` module**, next to
the screen:

```kotlin
// feature/subscriptions/ui/.../detail/SubscriptionDetailRoute.kt
@Serializable
data class SubscriptionDetailRoute(val subscriptionId: Int) : NavKey
```

Each feature contributes an `EntryProviderScope<NavKey>` extension in `composeApp/nav/entries/`.
This is the one place that knows both the route and the screen, so features never depend on each
other:

```kotlin
fun EntryProviderScope<NavKey>.subscriptionsEntry(navigator: Navigator) {
    entry<SubscriptionsRoute> {
        SubscriptionsScreen(
            onSubscriptionClick = { navigator.navigate(SubscriptionDetailRoute(it.id)) },
            onAddClick = { navigator.navigate(SubscriptionEditRoute(null)) },
        )
    }
    entry<SubscriptionDetailRoute> { route ->
        SubscriptionDetailScreen(
            subscriptionId = route.subscriptionId,
            onBackClick = { navigator.goBack() },
            onEditClick = { navigator.navigate(SubscriptionEditRoute(route.subscriptionId)) },
        )
    }
}
```

Route arguments reach the ViewModel through the screen via
`koinViewModel(parameters = { parametersOf(subscriptionId) })`. There is no `SavedStateHandle`
route extraction — that is the nav2 pattern and does not apply here. The receiving constructor
property takes **`@InjectedParam`**, which `KoinGraphTest` cannot enforce (§6.1).

`NavKeySerializersTest` walks `DrawerDestination.entries`, so it checks the *top-level* routes and
nothing else. A detail or editor route left out of `navKeySerializersModule` passes every gate in
the repo and only shows up as a lost back stack after process death (2.5) — the `am kill` cycle in
`CLAUDE.md` is the check.

The shell was built (1.8) before either of its two sections existed, so `SubscriptionsRoute` and
`SettingsRoute` started life in `composeApp/nav/Routes.kt` against a placeholder screen; each moved
into its feature's `ui` module with the screen that replaced the placeholder (2.4 and 2.6), which
each time was an import change in `DrawerDestination.kt`, `RouteConfig.kt`, `NavKeySerializers.kt`
and `MainNavHost.kt`. **`composeApp` now declares no route of its own, and that is the resting
state** — a route parked here is a route without a home, which is a smell, not a pattern. The
module keeps `kmp.serialization` for `navKeySerializersModule` alone, and `MainNavHost` names no
screen: every entry comes from an `entries/` extension.

**Not everything on screen is a route.** Login isn't: the startup branch (§7.1) renders it
*instead of* the whole shell, so it has no `NavDisplay` around it, no back stack entry and nothing
to register in `NavKeySerializers`. `feature:setup:ui` accordingly declares no `NavKey` and no
serialization plugin. The test for "is this a route?" is whether anything can navigate *back* to
it — a screen the app is either on or not is state.

### 5.4 Shell: navigation drawer + top app bar

Same shell as MealieMobile (and TaigaMobileNova): a **`ModalNavigationDrawer`** for top-level
sections, and a **controller-driven top app bar**. Both are ports, not new designs — the reference
implementations are:

```
MealieMobile/composeApp/.../composeapp/AuthenticatedMainScreen.kt      the whole shell
MealieMobile/composeApp/.../composeapp/widget/MealieDrawerWidget.kt    drawer
MealieMobile/composeApp/.../composeapp/MainAppState.kt                 drawer + route config state
MealieMobile/uikit/.../uikit/widgets/topappbar/                        TopBarConfig/Controller/Action
```

**Drawer.** `NavigationState`'s dual back stack — a `topLevelStack` for which section is active,
plus one independent sub-stack per section — is exactly the drawer model: each drawer destination
keeps its own history. Ports unchanged, along with single-top, re-tap-to-root, and back stepping
through the sub-stack before falling back to the section stack.

Supporting types come with it: `DrawerDestination` (enum of top-level routes, each typed `NavKey`
so the drawer's click site needs no cast), `DrawerItem` (`Destination` / `Group` / `Divider`, so
sections can be grouped with headers), `IconSource` (`Vector` or `Resource`), `DrawerItemsBuilder`
(a class, so the item list can later depend on state), and `MainAppState` exposing
`currentRouteConfig`, `drawerGesturesEnabled` and `currentDrawerDestination` as `derivedStateOf`.
`DrawerItemsBuilder` is a `@Factory` reached with `koinInject()`; it was *constructed* by the
shell until the Koin graph started in 1.11, which is also when `composeApp` gained `kmp.di`.

Wallos's drawer, grown across phases:

```
Dashboard
Subscriptions
── Manage ──────────
Categories
Currencies
Payment methods
Household
────────────────────
Settings
```

The `DrawerItem.Group` type is what makes the *Manage* grouping free — it is the reason Mealie's
drawer takes a list of items rather than a flat list of destinations.

`RouteConfig`/`RouteConfigProvider` carries over, trimmed to what v1 can actually produce:
`DrawerConfig` is `Enabled` on top-level screens and `GesturesDisabled` on detail/editor screens.
`Hidden` (and with it `MainAppState.showDrawer`) has no route to apply to, and **`FabConfig` is
not in v1 at all** — the add-subscription FAB is a Phase 3 write. Both grow back with the screen
that needs them, along with the snackbar host: v1 screens keep their errors in UI state, so the
shell renders no `SnackbarHost`.

**`LocalIsOffline` is the shell's second composition local** (3.2), provided beside
`LocalTopBarConfig` from `!networkMonitor.isOnline`. `AuthenticatedMainScreen` injects the monitor
itself as a `koinInject()` default rather than taking `isOnline: Boolean` from above as Mealie
does: `WallosAppContent` renders login as well as the shell, and connectivity has no reader on
that side of the startup branch. It reads a `StateFlow`, so `collectAsState()` has its value in the
*first* composition and nothing is pushed below `rememberNavBackStack` (§5.5). The local errors
when unprovided rather than defaulting to `false` — an unprovided tree should fail, not silently
claim to be online — so `WallosMobilePreviewTheme` provides it too, and it is listed in
`.editorconfig`'s `compose_allowed_composition_locals`.

One detail from `AuthenticatedMainScreen.kt` worth keeping deliberately, because it is
bug-fix-shaped: the `NavigationBackHandler` that closes the drawer is placed **after**
`MainNavHost` in the composition — last-composed enabled handler wins, so composing it earlier
means back navigates the stack while leaving the drawer open. It also checks `isAnimationRunning`,
not just `isOpen`. (Mealie's other one, suppressing the FAB when offline, waits for the FAB.)

**Top app bar.** A single `WallosTopAppBar` in `uikit`, driven by a `TopBarController` provided
through `LocalTopBarConfig`. Screens declare their bar; the shell renders it:

```kotlin
// in the screen
val topBarController = LocalTopBarConfig.current
LaunchedEffect(Unit) {
    topBarController.update(
        TopBarConfig(
            title = NativeText.Resource(RString.subscriptions_title),
            navigationIcon = NavigationIconConfig.Menu,      // Menu | Back | Custom | None
            actions = persistentListOf(
                TopBarActionVectorButton(Icons.Default.FilterList, onClick = ::openFilters)
            )
        )
    )
}
```

`NavigationIconConfig.Menu` opens the drawer, `Back` falls through to `navigator.goBack()` unless
given an override, and `None` hides the bar entirely (the shell keys `isVisible` off it). Actions
come in icon / vector / text variants. `TopBarConfig` uses `NativeText` and `ImmutableList`, so it
composes with the existing state conventions.

`NativeText` itself is Mealie's type, trimmed: `utils:ui` holds `Empty` / `Simple` / `Resource`
plus a `@Composable asString()`, which is everything the bar and a screen title need. The rest of
Mealie's surface — `Plural`, `Arguments`, `Multi` and `asStringBlocking()` — gets added when a step
actually needs it.

`getErrorMessage(Throwable)` lives in the same file and is the **only** place an error becomes
something a user reads. It maps by *failure layer* (§1.1, API doc §5.1), because the layer is what
tells the user which field to fix:

| Error | Reads as | Points at |
|---|---|---|
| `Malformed`, `UnsupportedEndpoint` | nothing that looks like Wallos answered | the **URL** |
| anything that isn't a `WallosError` | couldn't reach that server | the **URL** |
| `Unauthenticated` | the instance didn't accept this key | the **key** |
| `Forbidden` / `NotFound` / `Validation` / `InUse` / `Server` | one message each | — |

The `WallosError` branch is an **exhaustive `when` over the sealed class**, not a `when` over
`Throwable` with an `else`: a new error type has to fail the build rather than quietly render as a
connection problem. Errors that belong to one feature — `ApiKeyNotFound`, say — are handled in
that feature's ViewModel, since `utils:ui` must not see a `feature:*:domain` module.

`ObserveAsEvents` is here too, as the collector side of the `Channel` + `receiveAsFlow()` one-off
event convention: it collects inside `repeatOnLifecycle(STARTED)`, so an event sent while the
screen is backgrounded is delivered when it returns rather than acted on off-screen.

Both pull `core:domain` and `strings` into `utils:ui` — the two dependencies its build file
declares beyond the Compose resources one.

This decouples every screen from the shell: a feature `ui` module depends on `uikit`, never on
`composeApp`.

### 5.5 Corrections to the MealieMobile doc

Four places where `kmp-nav3.md` has drifted from Mealie's actual code. Following the doc verbatim
will not compile:

1. **`NavBackStack` is generic.** The real signatures are `NavBackStack<NavKey>`, not the bare
   `NavBackStack` shown in the doc.
2. **`rememberViewModelStoreNavEntryDecorator<NavKey>()`** needs the explicit type argument.
3. **`SavedStateConfiguration.DEFAULT` is not sufficient.** The doc's "Known KMP limitations"
   section says to use it; the real `NavigationState.kt` builds a config carrying a
   `SerializersModule` with `polymorphic(NavKey::class) { subclass(EachRoute::class) }` for
   **every** route in the app. `rememberNavBackStack` `require`s this — passing the default
   configuration throws `IllegalArgumentException` on the **first composition**, so the shell
   cannot be stood up with a placeholder config and grow the module later.
   This is the one real cost of nav3 here — `composeApp` must enumerate every route class. An
   *unregistered* route is the silent case: composition succeeds and only back-stack restoration
   across process death fails. Mitigate with a test that asserts the registered set matches the
   set of routes reachable from the entry providers.
4. **Nav3 is no longer alpha.** The doc cites `1.1.0-alpha03`; the catalog is on `1.1.1`.
5. **`rememberNavBackStack` restores only in the *first* composition.** Nothing in the doc says
   so, and it constrains anything rendered *above* the shell. `WallosAppContent`'s startup branch
   (§7.1) reads a DataStore flow that has no value for frame 1; waiting for it composed
   `AuthenticatedMainScreen` a pass later, and the restored back stack was simply never consumed —
   the app came back alive, on the start destination, with no error anywhere. So the branch is
   seeded from `rememberSaveable` and the shell composes immediately (1.11).
   **This is invisible to the "Don't keep activities" developer option**, which recreates the
   activity inside a live process and passes either way. The check that catches it is
   `adb shell am kill <pkg>` on a backgrounded app, and that is what a process-death Verify line
   should mean from here on.

Also worth knowing: `io.insert-koin:koin-compose-navigation3` exists and is declared in Mealie's
catalog but is **not used** — `koinViewModel()` + `rememberViewModelStoreNavEntryDecorator()` is
enough. Don't add it without a reason.

### 5.6 What this changes elsewhere in this plan

- `core:navigation` is a real module with logic and tests, not the thin extension holder it is in
  TaigaMobileNova.
- Feature `ui` modules that **own a route** need `kotlinx.serialization` (for `@Serializable`
  routes) in addition to Compose. Not every one does: `feature:setup:ui` renders above the shell
  and never enters a back stack, so it carries no route and no serialization plugin (1.11).
- `Navigator` is unit-tested directly: `NavBackStack(vararg elements)` is a public constructor, so
  a test builds `NavigationState` by hand with no Compose runtime and no `rememberNavBackStack`,
  and `derivedStateOf` reads fine outside a composition. There is **no `NavigatorTest` in
  MealieMobile** to port — `core/navigation`'s was written for this repo (1.7).

---

## 6. Architecture patterns (inherited from TaigaMobileNova)

These carry over unchanged; they are listed so the port is deliberate rather than partial.

- **MVVM + Clean Architecture**, feature modules split `data` / `domain` / `dto` / `mapper` / `ui`.
- **State classes hold data *and* callbacks.** `MutableStateFlow` + `.update {}`, exposed as
  `StateFlow`. Strings from the ViewModel are `NativeText`, resolved in the UI.
- **One-off events via `Channel` + `receiveAsFlow()`**, observed with `ObserveAsEvents`. Never in
  UI state. Snackbars through `SnackbarDelegate`.
- **Use cases only when a screen needs multiple repository calls.** Single-call screens talk to
  the repository directly. (Wallos has real use-case candidates — see §8 Phase 4.)
- **Koin with `io.insert-koin.compiler.plugin`**, one `@Module @Configuration @ComponentScan` per
  module. Never KSP for DI.
- **Offline = disable, missing permission = hide.** Wallos has no permission model beyond
  "user id 1 is admin", so this reduces to `LocalOfflineState` disabling write actions.
- **Tests: hand-written fakes, never mocks** — see §6.1.

### 6.1 Testing — fakes, not mocks

**No mocking library. Not MockK, not Mockito, not anything — in any source set.** Test doubles are
hand-written classes in the shared `:testing` module, exactly as in TaigaMobileNova and
MealieMobile. Assertions are `kotlin.test`; flows use Turbine.

The reason is not purity: a fake is a compile-time-checked implementation of the interface, so
changing a repository signature breaks the fake and every affected test at once, loudly. A mock
records the *old* signature in a string-ish DSL and keeps passing until it fails at runtime for a
reason that doesn't mention the change.

**Fake shape** — set the result, record the calls, fail loudly if unconfigured:

```kotlin
class FakeSubscriptionsApi : SubscriptionsApi {
    var subscriptionsResult: List<SubscriptionDTO>? = null
    var currenciesCalls = 0

    override suspend fun getSubscriptions(): List<SubscriptionDTO> =
        subscriptionsResult ?: error("subscriptionsResult not set")
}
```

`error("… not set")` rather than a default is deliberate: a test that forgot to arrange its data
fails with a message naming the missing field, instead of silently asserting on an empty list.

**Layout of `:testing`** (mirrors Taiga's):

```
testing/src/commonMain/kotlin/.../testing/
  api/          FakeSubscriptionsApi, FakeWebLoginApi, …
  repo/         FakeSubscriptionsRepository, FakeSetupRepository, …
  storage/      FakeApiKeyStorage, FakeServerStorage, …
  models/       SubscriptionFakes.kt, CurrencyFakes.kt — fixture builders
  utils/        getRandomString(), getRandomInt(), nowLocalDate, testException
  MainDispatcherRule.kt
```

That layout is the *destination*, not a starting shape: a double moves here when a **second**
module needs it, and stays private to its one test file until then. The bar matters because
`configureTests()` puts `:testing` on every module's `commonTest` classpath — a fake parked here
early drags its whole `feature:*:domain` into modules that have no business seeing it. Through 2.3
only `MainDispatcherRule` has earned the move; the setup and subscriptions fakes are all still
private to their tests, and the checklist has twice asked for a move that the bar declined
(1.10, 2.3). Recorded response bodies live the same way — Kotlin constants in a `*Fixtures.kt`
beside the test, because `commonTest` has no portable way to read a resource.

One recurring trap in the repository tests: a `StandardTestDispatcher()` constructed outside
`runTest` carries its own scheduler, so the `withContext(dispatcher)` inside a repository fails
with "Detected use of different schedulers". `UnconfinedTestDispatcher()` doesn't, and is what
every repository test here injects.

`MainDispatcherRule` is not a JUnit `@Rule` — `commonTest` is `kotlin.test`, so the test class
calls `setup()`/`tearDown()` from `@BeforeTest`/`@AfterTest`. Every ViewModel test needs it:
`viewModelScope` dispatches on `Dispatchers.Main`, which a host test does not have, so without
`Dispatchers.setMain` the first `launch` throws.

**Fixture builders** are top-level functions returning randomized-but-valid models, with every
field overridable:

```kotlin
fun getSubscription(
    id: Int = getRandomInt(),
    name: String = getRandomString(),
    cycle: BillingCycle = BillingCycle.MONTHS,
): Subscription = Subscription(id = id, name = name, cycle = cycle, /* … */)
```

Randomized defaults mean a test that passes only because two unrelated fields happened to both be
`0` or `""` will not stay green. Only the fields a test actually cares about get named.

**The Koin graph gets its own test** (`composeApp`, 1.11). A missing definition is a launch-time
crash that no other gate can see — the compiler plugin resolves nothing, and both a
`@ComponentScan` that misses a class and an `AppModule` that forgets an `includes` line compile
cleanly. Use `koin-test`'s **`verify()`, not `checkModules()`**: `checkModules` instantiates
definitions, which here means a DataStore file and an HTTP engine, while `verify()` is pure
reflection over constructors.

Two things to know before reading a failure:

- `verify()` reads a definition through its **bound type's** constructor, so for a
  `@Single fun provideHttpClient(…)` it inspects `HttpClient(engine)` rather than the function's
  own parameters. `HttpClientEngine` therefore sits in `extraTypes` as a known false positive,
  alongside the types `:androidApp` genuinely supplies (`Context`, `AppInfoProvider`).
- `AppModule`'s `includes` list is load-bearing. The compiler plugin auto-gathers `@Configuration`
  modules only from the compilation that calls `startKoin`, which is why `androidApp`'s
  `AndroidModule` needs no entry and every cross-module one does.

And one thing the graph test **cannot** see (2.5): `verify()` whitelists `String`, `Int`, `Long`
and `Double` outright (`Verify.primitiveTypes`), so a primitive constructor parameter is reported
verified whether or not it is annotated. A route argument therefore needs `@InjectedParam` on the
ViewModel's constructor property for its own sake — without it the compiler plugin looks for an
`Int` definition in the graph and the screen crashes at first injection, with no gate in between.

**Not everything gets an interface.** The interfaces in this app are seams over a *platform* or
over *IO* — `SecretCipher`, `ApiKeyStorage`, `WebLoginApi` — because a host test cannot reach the
real thing. Pure logic gets none: mappers are classes (CLAUDE.md), and so are the formatters
(2.2). A `FakeMoneyFormatter` would only let a consumer's test assert against output the app never
produces, so consumers construct or inject the real one.

**What gets tested.** Pure logic earns real coverage — `WallosEnvelopeParser`, error mapping,
`FormParams`, mappers, the formatters, `Navigator`, the login response interpreter and key regex.
ViewModels are tested through their `StateFlow` with fakes injected. Composables are not unit
tested; `uikit` widgets, screens, DI modules and DTOs are excluded from Kover (§ the root build's
`excludes` block).

`:testing` is added to every module's `commonTest` automatically by the convention plugin, so no
module declares it by hand. That makes it the home for test-only *libraries* as well as doubles:
it re-exports `kotlinx-coroutines-test` (for `runTest`) and `ktor-client-mock` (for `MockEngine`)
as `api`, so neither is ever declared per module. `MockEngine` matters because `HttpClient { }`
uses engine autodiscovery and the real engine is `androidMain`-only — a host test that wants an
`HttpClient` has to build one itself.

**Instrumented tests exist, as of 3.3, and only where a host test is impossible.** `core:storage`
has an `androidDeviceTest` source set for the Room DAOs (§4.7) — not a preference for realism but
the only option: neither `Room`'s builders nor `BundledSQLiteDriver`'s native library can be
reached from `testAndroidHostTest`, and Robolectric remains ruled out for the reason above. Its
cost is real and should be weighed before the second one: it needs a running emulator, it is
outside `allTests`, and CI therefore doesn't run it (§3.5). `:testing` is *not* on its classpath —
`configureTests()` wires `commonTest` only — so a device test either declares what it needs or
does without.

### Domain modelling notes

- **`BillingCycle`** enum (`DAYS, WEEKS, MONTHS, YEARS, ONE_TIME`) + `frequency` multiplier. The
  editor must **not** offer `ONE_TIME` — the API rejects `cycle=5` on write even though the web UI
  and the database support it. Read paths must still handle it. `fromCode` returns **`null`** for
  a code this build doesn't know, and `Subscription.cycle` is nullable to match: an unrecognised
  cycle means a newer instance, and the screen drops the cycle text rather than defaulting to a
  wrong one (2.1).
- **Strings and dates off the wire need laundering, not just parsing** (API doc §3.1, verified on
  the live instance in 2.1): `name`/`notes`/`*_name` are **HTML-escaped** (`1&amp;1 Telekom`), and
  unset dates are **`""` rather than `null`**. Both are the mapper's job — every domain date is a
  nullable `LocalDate` so one unparseable value can't sink a list.
- **Domain models carry what the screens render, not what the row holds.** `SubscriptionDTO` is
  the full §3.1 row; domain `Subscription` is §7.1's list + detail fields and nothing else, and
  domain `Currency` is `id/name/symbol/code` without `rate` or `in_use`. Fields arrive with the
  screen that needs them (2.1).
- **Money.** `price` is a JSON number, `monthly_cost` is a preformatted string with thousands
  separators (`"1,234.56"`), and currency `rate` is a string. Parse each explicitly in
  `utils:formatter:decimal`; never map `monthly_cost` to `Double` directly. `MoneyFormatter`
  renders **`1,234.56` regardless of the device locale** (2.2): the instance formats its own
  totals `en_US` whatever the user's language (API doc §3.5), so a device-formatted price would
  disagree with every total the same server shows. Locale-aware money is possible later, but it
  needs `expect`/`actual` over the platform `NumberFormat` and stops being host-testable. The
  currency **symbol is a parameter, not a code** — Wallos stores an arbitrary string. Rounding is
  half-up (`floor(x + 0.5)`): `kotlin.math.round` breaks ties towards the *even* integer and
  renders an exact `0.125` as `0.12` where PHP's `number_format` says `0.13`.
- **Dates.** `utils:formatter:datetime` owns both shapes: `parseIsoDate`/`formatIsoDate` for the
  wire (2.2), and `formatDisplayDate` for the screen — `5 Mar 2026`, built from kotlinx-datetime's
  `MonthNames.ENGLISH_ABBREVIATED` (2.4). It lives here rather than in a composable because it
  needs no resource table and therefore stays a pure, host-tested function; it is hard-coded
  English on exactly the terms `MoneyFormatter` is hard-coded `en_US`, and the two move together
  the day the app is translated. `day()` pads to two digits — `day(Padding.NONE)` is what gives
  `5 Mar` rather than `05 Mar`. The **cycle text** ("every 6 months") is the one thing that cannot
  live here: it is a plural over `BillingCycle`, so it is resolved with `pluralStringResource` in
  the composable that renders it, and the UI item carries the enum + frequency rather than a
  string (2.2, 2.4).
- **Silent failures need UI affordances** (API doc §5.5): `convert_currency=true` with no exchange
  rates returns unconverted prices and an empty `notes` — detect by comparing `currency_id`
  against the user's `main_currency` and show a hint. A failed logo fetch reports success — re-read
  `logo` after a write to confirm.
- **Never combine `all-user-subscription=1` with filters** — the server builds
  `SELECT * FROM subscriptions AND …` and the query fails to prepare. The repository should make
  this combination impossible to express. In v1 it is unreachable by construction: `SubscriptionsApi`
  sends `api_key` and nothing else, so there is **no `SubscriptionQuery` type** (2.3) despite the
  fake sketch above naming one. Filtering and sorting are client-side, and the server's default sort
  is already `next_payment`. Whatever Phase 2b adds has to keep the two sides unable to meet.
- **Wire text needs unescaping, and the order matters.** `HtmlUnescaper` (`feature:subscriptions:mapper`,
  2.3) reverses PHP's `htmlspecialchars` over `name`, `notes`, the resolved `*_name` fields and
  currency `name`/`symbol`. It decodes `&amp;` **last**: doing it first turns `&amp;lt;` — the
  literal text `&lt;` — into a `<` the user never typed. Only the five entities
  `htmlspecialchars` emits, plus the unpadded `&#39;`; it is not a general HTML decoder.

---

## 7. Screens

### 7.1 v1 — the walking skeleton

The first goal is one honest vertical slice: **log in, see your subscriptions**. Three screens.

| # | Screen | Contents | Endpoints |
|---|---|---|---|
| 1 | **Login** | Server URL, username, password (with visibility toggle), Connect button. Loading + error states. Secondary "I have an API key" link revealing a key field. No drawer, no top bar (`NavigationIconConfig.None`). | `login.php`, `profile.php`, `api/status/version.php` |
| 2 | **Subscriptions list** | `LazyColumn` of cards: logo, name, price + currency symbol, next payment date, billing cycle ("every 6 months"), inactive badge. Loading / empty / error states, pull-to-refresh. Top bar: title + `Menu` icon. Drawer enabled. | `get_subscriptions.php`, `get_currencies.php` |
| 3 | **Subscription detail** | Read-only: logo, name, price, cycle + frequency, next payment, start date, category, payment method, payer, notes, URL, active state. Top bar: name + `Back`. Drawer gestures disabled. | `get_subscription.php` (+ `get_currencies.php` for the join) |

Plus a **startup branch** (not a screen, and not a route): stored key present → shell, absent →
login. It is `ApiKeyStorage.isConnected` collected in `WallosAppContent`, above the theme's
content — one source of truth, so login and disconnect both work by flipping it. Two things it
must get right (both learned in 1.11): it has to render the shell in the **first** composition
or the restored nav back stack is lost (§5.5), which is why the branch is seeded from
`rememberSaveable`; and because it is not a route, nothing about it belongs in
`NavKeySerializers`.

**Shell in v1:** the full drawer + top-bar port from §5.4, with the drawer holding *Subscriptions*
and *Settings* (Settings being a stub screen with Disconnect on it — one item is a lonely drawer,
and it gives logout an obvious home). No FAB until Phase 3 adds the editor.

**The Settings stub is `feature:settings:ui` and nothing else** (2.6). Disconnect is a single call
on a single seam, so `SettingsViewModel` takes `ApiKeyStorage` straight from `core:storage`: a
`domain` layer over one `clear()` would be an abstraction for one use, and the `data`/`domain`/`dto`
modules §2 lists arrive with Phase 5's server display settings. It also needs no navigation and no
success signal — `clear()` flips `isConnected` and the startup branch above swaps the tree, so the
screen is gone before it could render a spinner. A write that fails is logged and leaves the user
connected, which is the truthful outcome and the reason there is no error state.

Two consequences of `clear()` keeping the server URL (§4.7) that are easy to conflate: the URL
really does survive, but the login screen does **not** prefill it, so re-connecting still means
retyping the address. Seeding `LoginUiState.serverUrl` from `ServerUrlStorage` is the fix and it
belongs to `feature:setup:ui`; until it lands, don't write copy that promises a one-field re-login.

**Non-obvious dependency:** the list can't render a price without currency symbols. `price` comes
with a `currency_id`, and the symbol lives in `get_currencies.php`. So the list screen is two calls,
not one — fetch currencies once and map `currency_id → symbol`. (The alternative,
`convert_currency=true` plus the user's `main_currency`, silently returns unconverted prices when
the instance has never fetched exchange rates, so it's the worse default.)

The join lands in `SubscriptionsRepository` as a **`currencySymbol` field on domain
`Subscription`** (2.3), not as a map handed up to the screen: a consumer that has the model has
everything it needs to render a price, and nothing above the repository ever sees a currency list.
Blank for a `currency_id` the instance no longer lists — a deleted currency costs the sign, not the
screen. Two consequences to keep in view:

- **Every repository call is two round trips.** "No cache" (§7.2) means the currency list is
  re-read per call, so opening the list and then a detail is four requests. Phase 2b's Room cache
  is the fix; caching it in a ViewModel just moves the staleness somewhere untested.
- The subscriptions call goes **first**, so a failure on the resource the user actually asked for
  short-circuits before the second one.

**The second thing the model can't render on its own is the logo.** The wire carries a bare
filename and the full URL is `{base}/images/uploads/logos/{logo}`, so the *ViewModel* builds it —
`feature:subscriptions:ui` takes `core:api` for `BaseUrlProvider` alone, which is the one place the
instance root is normalized (2.4). Blank in, blank out: no logo, or no stored server, yields no URL
rather than a relative one Coil would fail on. Wallos serves that directory **unauthenticated**, so
a plain URL load needs no header plumbing — `coil.compose` + `coil.ktor`, no `ImageLoader` setup,
the ktor3 fetcher finding okhttp by autodiscovery.

**The detail screen re-reads its own row** (2.5) rather than being handed one by the list. The row
the list holds is a snapshot from whenever it last refreshed, and there is nothing that invalidates
it; the price of reading again is the two round trips above, paid a second time. Only the id
travels in `SubscriptionDetailRoute`. A field the instance has nothing for — `notes` and `url` are
`""` on every row of the local instance, `start_date` on many — has its whole row left out, since a
label over an empty value reads as a bug rather than as "unset".

Four things the two screens both need live in `feature:subscriptions:ui/widgets/` rather than in
either: `SubscriptionLogo` (parameterized by size), `InactiveBadge`, `cycleText` (which 2.4 had as
a private composable on the card), and — as `ui/LogoUrl.kt` — the `BaseUrlProvider.toLogoUrl(logo)`
extension below. That last one is a two-line helper over an injected seam, not a mapper, so
CLAUDE.md's mappers-are-classes rule doesn't reach it.

**A failed load clears the list** (2.4). With no cache there is nothing behind the error worth
keeping, and a stale list under "couldn't reach the server" is the shape that lies; the retry
button is the way back. Worth revisiting when Phase 2b's Room cache makes "stale but real" a state
the app can honestly describe.

### 7.2 Explicitly out of v1

Everything here is real work that the walking skeleton does not need. Deferring it is what keeps
v1 small:

- **TOTP** — if `login.php` redirects to `totp.php`, show "this account needs a one-time code;
  use the API key instead" and point at the key field. That's the whole handling.
- **Room / offline cache** — fetch on screen open. No `NetworkMonitor`, no `LocalOfflineState`.
- **Certificate trust prompt** — a plain HTTPS instance works. Self-signed certs fail with a clear
  error until this lands.
- **Extra drawer destinations** — the shell is fully wired (§5.4), but the drawer holds
  *Subscriptions* and *Settings* only. Dashboard and the *Manage* group arrive with their features.
- **`password_login_disabled` probing, login backoff, non-HTTPS warning** — Phase 2b hardening.
- Writes, dashboard, catalog CRUD, settings, profile, notifications.

`core:crud`, `core:serialization` and the `categories`/`currencies`/`paymentmethods`/`household`
feature modules don't exist yet in v1 — currencies is a single API call inside
`feature:subscriptions` until it earns its own module in Phase 3.

### 7.3 Full screen set (eventual)

| Section | Screens |
|---|---|
| Onboarding | Login, API-key entry, TOTP step |
| Dashboard | Home (monthly cost, period budget, upcoming payments) |
| Subscriptions | List, Detail, Editor (add/edit), Filter sheet |
| Manage | Categories, Currencies, Payment methods, Household — each list + editor |
| Settings | Display settings, App settings (theme), Profile + budget, Notifications (read-only), About |

---

## 8. Phases

Each phase ends with a build that runs and a feature that works.
**Phases 0 → 2 are the v1 walking skeleton (§7.1).**

### Phase 0 — Foundation
Restructure the wizard scaffold: delete `desktopApp`/`iosApp`/sample files, rename `shared` →
`composeApp`. Port `build-logic` with the six convention plugins and Android-only targets, with
nav3 wired into `configureKmpCompose()` (§5.1). Adopt Taiga's version catalog plus Mealie's nav3
entries. Stand up empty `core:*`, `utils:*`, `uikit`, `strings`, `testing`.
Wire detekt + ktlint + kover + compose-rules, and a GitHub Actions build/test workflow (§3.5).
*Done when:* `./gradlew build` and `./gradlew allTests` both pass on a stub app. (There is no
`jvmTest` — see §3.1; the per-module unit test task is `testAndroidHostTest`, and `allTests`
fans out to it.)

### Phase 1 — Login (screen 1)
`core:api`: `WallosApiClient`, `WallosEnvelopeParser`, `WallosError` mapping, `FormParams`.
`core:storage`: DataStore for base URL + API key (Android Keystore–backed) — **no Room, no
`NetworkMonitor`**. `feature:setup`: the `@WebSessionHttpClient` factory, `POST /login.php` with
redirects disabled, `profile.php` scraping, validation against `api/status/version.php`, plus the
"I have an API key" field. Tolerant URL normalization (trailing slash, subpath — many users run
Wallos under `/wallos`). TOTP redirect → message pointing at the key field, nothing more.
`core:navigation` + shell: `NavigationState`, `Navigator`, `DrawerDestination`, `DrawerItem`,
`MainAppState`, `RouteConfig`, the drawer widget and `WallosTopAppBar` in `uikit` — all ported
from MealieMobile per §5.4. `composeApp`: DI root, `MainNavHost`, `NavKeySerializers`,
login-vs-shell startup branch.
*Done when:* username/password onboarding works against the local instance in
`docs/local-info.txt`, the key persists, and the app survives process death on the right screen.
*Test focus:* the envelope parser, the API doc's §5.3 auth-title table, the login
response-interpretation table (302-to-`.` vs 302-to-`totp.php` vs 200), and the `id="apikey"`
regex — all pure functions over recorded fixtures.

### Phase 2 — Subscriptions list + detail (screens 2 and 3)
`feature:subscriptions` list and detail, fetched on screen open — no cache. Currencies fetched
alongside and mapped `currency_id → symbol` for price rendering (§7.1). Logos via Coil
(`{base}/images/uploads/logos/{logo}`, unauthenticated). Loading / empty / error states,
pull-to-refresh, and Disconnect on the Settings stub (§7.1) rather than in a top-bar overflow.
*Done when:* logging in lands on a list of real subscriptions and tapping one opens its detail.
**This completes v1.**

### Phase 2b — Hardening and offline
Everything §7.2 deferred, once the slice works end to end: Room cache + `NetworkMonitor` +
offline-first repository, certificate trust prompt for self-signed instances, TOTP second step,
`password_login_disabled` probing, login backoff, non-HTTPS warning, client-side filter (member,
category, payment method, active/inactive) and sort, currency-conversion hint when rates are
missing.
*Done when:* the list renders offline after one online fetch, and a self-signed instance connects.
*Decomposed as **M3** in `docs/CHECKLIST.md`* (12 steps), chosen over Phase 3 because three of
M2's steps deferred cache debt to it and because Phase 3's writes need its `NetworkMonitor`.
The Room step also brought **instrumented tests into the project**, earlier than §6.1 expected —
a DAO cannot be exercised from a host test at all (§4.7).

### Phase 3 — Subscriptions, write + reference data
Add / edit / delete, including the multipart logo upload and `logo_url` fetch. `feature:categories`,
`currencies`, `paymentmethods`, `household` data+domain layers on `core:crud`, surfaced first as
pickers inside the subscription editor. Enforce: `ONE_TIME` unavailable, strict date format,
`"1"`/`"0"` encoding, re-read after write to confirm the logo landed.

### Phase 4 — Dashboard
`get_monthly_cost` and `get_period_budget` with version gating, upcoming payments derived locally
from `next_payment` + cycle. This is where use cases earn their place — the home screen composes
three endpoints plus cached subscriptions into one state.

### Phase 5 — Management screens
Full CRUD UI for the four catalog resources (with the in-use delete guard surfaced properly),
`feature:settings` (server display settings + local theme), `feature:profile`
(`get_user`, `set_budget` — and note that sending `period_budget` alone silently resets the period
type and anchor, so always send all three together).

### Phase 6 — Extended
Read-only `feature:notifications`. iCal feed export/share (`text/calendar`, not JSON — check the
content type before parsing). `feature:admin` behind a user-id-1 check. Home-screen widget.
Re-enable the iOS and Desktop targets in `configureKmp()` and restore the entry-point modules.

---

## 9. Risks

| Risk | Mitigation |
|---|---|
| Self-hosted instances run different Wallos versions with different columns and endpoints. | `ignoreUnknownKeys`, version gating, `UnsupportedEndpoint` as a first-class error. |
| PHP diagnostics corrupt otherwise-valid responses. | Sanitizing parser in front of every decode; log the prefix so users can report it. |
| No API for notification writes, stats, clone, renew, or key regeneration — those are session-cookie endpoints only. | Stay on the API-key surface. Scraping the login form breaks under TOTP/OIDC. File upstream requests for the gaps. |
| The API key is a plaintext bearer credential. | Keystore-backed storage, HTTPS required, trust prompt instead of pinning. |
| **The login bridge scrapes HTML.** `id="apikey"` on `profile.php` is not an API contract; an upstream markup change breaks onboarding silently. | Validate the scraped key against `version.php` before storing, and keep Path B (manual entry) permanently in the UI as the recovery route. |
| **The login bridge handles the user's real password** — over cleartext on many self-hosted LAN instances. | Never persist it; exchange for the key and drop. Warn on non-HTTPS origins and steer to Path B there. |
| The server has **no login rate limiting or lockout**, so a retry loop is a brute-force tool aimed at the user's own instance. | Client-side backoff on failed login attempts. |
| Password login may be disabled (`OIDC_DISABLE_PASSWORD_LOGIN`), and OIDC can't be bridged at all. | Probe the login form during setup and degrade to Path B. |
| Whole-list fetches degrade with very large subscription sets. | Room cache + client-side paging; the payload is one row per subscription, so this is unlikely to bind in practice. |
| A route added without registering it in the nav3 polymorphic `SerializersModule` breaks back-stack restore — silently, and only on process death. | Test asserting the registered set covers every route reachable from the entry providers; "Don't keep activities" in the Phase 1 acceptance check. |

---

## 10. Open decisions

### Still open

1. **Money representation** — *needed at checklist step 2.2.* `Double` + careful formatting is
   enough if the client never does arithmetic beyond summation. If it does, KMP needs an external
   big-decimal library.
2. **Catalog module granularity** — *not needed until Phase 3.* §3.4 proposes four feature modules
   over a shared `core:crud`. The alternative is one `feature:catalog` module — less boilerplate,
   but it diverges from the Taiga structure being mirrored.

### Settled

- **DI mechanism** — `io.insert-koin.compiler.plugin`, never KSP for DI. Confirmed: both
  TaigaMobileNova and MealieMobile use it, with identical `KmpDiConventionPlugin`s. The
  `koin-ksp-compiler` entry in Mealie's catalog is unused. (KSP is still needed for Room.)
- **Android-only targets** — `configureKmp()` declares no targets at all; the Android one comes
  from the AGP KMP library plugin (§3.1). iOS and Desktop return in Phase 6.
- **nav3 placement** — `NavigationState`/`Navigator`/`toEntries()` live in `core:navigation`
  (§5.2), not in `composeApp` as MealieMobile has them, so `Navigator` stays unit-testable.
- **Shell** — `ModalNavigationDrawer`, not bottom navigation, matching both reference apps (§5.4).

### Superseded

- ~~Declaring `jvm()` immediately to keep `commonMain` honest~~ — replaced by the stricter rule
  that feature modules have no `androidMain` at all (§3.1), which catches the same leakage without
  the build cost.
