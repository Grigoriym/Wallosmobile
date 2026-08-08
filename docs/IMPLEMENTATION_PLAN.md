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
2. If `Location: totp.php`, prompt for the code and `POST /totp.php` with `one-time-code` on the
   same cookie jar (3.9 — see "The second factor" below).
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

  **`@Factory` is not per-*call*, and 3.9 depends on that.** Koin resolves a `@Factory` once per
  **injection point**, and the only injection point in this chain is `LoginViewModel`'s
  constructor — nothing else in the app asks for a `SetupRepository`. So one cookie jar spans one
  login screen, which survives a rotation and dies with the screen. That window is exactly what
  the second factor needs, and it is why 3.9 changed no lifetime at all: the danger the `@Factory`
  guards against is the *process*, not the attempt.

- **The second factor is a step on the same screen, not a second attempt** (3.9). `login.php`
  answering `Location: totp.php` is a success — the password was accepted and the session now
  carries `totp_user_id` — so `LoginOutcome.NeedsTotp` swaps the credential fields for a code
  field and `SetupRepository.submitTotpCode(code)` answers it. That method takes no `serverUrl`
  and clears no key: both were done by the `loginWithPassword` that raised the challenge, on the
  session this one runs against. Once `totp.php` redirects, the tail is the same
  scrape-validate-store as Path A's, shared as one private method.

  `totp.php` has **three** answers, not two (API doc §9.2), and telling the third apart is the
  whole reason it gets its own interpreter: `302` to `.` is verified, `200` is a rejected code —
  and `302` to `login.php` means the session no longer holds `totp_user_id`, which **no code can
  fix**. Reported as a bad code it would have the user typing fresh digits forever, so it clears
  the challenge and asks for the password again. The password itself is dropped from state the
  moment the challenge appears: the session carries the login from there.

  The code field is deliberately **not** numeric — Wallos accepts a backup code here, and a
  backup code is 20 hex characters (`endpoints/user/enable_totp.php`) that a number pad cannot
  type.

- **Clear the stored key before starting an attempt.** `WallosApiClient` injects the *stored* key
  over any `api_key` the caller put in the `FormParams` (§4.1), so a leftover key is what the
  validation call would actually validate — and a newly scraped key would then be stored on the
  strength of the old one. `clear()` keeps the server URL (§4.7), so this costs nothing.

- **Detect whether password login is even available.** `password_login_disabled` (admin setting or
  the `OIDC_DISABLE_PASSWORD_LOGIN` env var) strips the credential fields from the form, and
  OIDC-only instances can't be bridged at all. GET `login.php` during setup and check for the
  password input; if absent, show Path B only. **OIDC cannot be bridged** — it is a redirect dance
  against a third-party IdP whose registered `redirect_url` points at the Wallos web app.
  **Shipped in 3.10**, as `SetupRepository.probePasswordLogin(serverUrl)` →
  `PasswordLoginAvailability`, driven off the **URL field** with a 700 ms debounce rather than off
  Connect — being earlier than the password is the entire point, and Connect is not earlier. Three
  properties it needs, each of which is a way of getting it wrong:
  - **It is an affordance, so it fails open.** Only `Disabled` moves anything; a transport failure,
    a redirect, or a page that isn't the form are all `Unknown` and leave both paths on offer. It
    *hides* a path, so it may only fire on evidence — and the evidence is the form itself, which
    has to be recognised before its missing input means anything (API doc §9.5).
  - **It is silent.** It runs off a keystroke, so an unreachable host is not news there, and
    raising the trust prompt from a half-typed URL would be a modal nobody asked for. Connect
    still reports both.
  - **It must not run between the login POST and the TOTP POST.** It shares the session, and a GET
    of `login.php` clears `$_SESSION['totp_user_id']` (API doc §9.1) — so the probe is guarded on
    `isTotpRequired` and the next code would otherwise die as `TotpSessionExpired`.

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
  **Shipped in 3.10** as `LoginThrottle`, a counter *inside* `SetupRepositoryImpl` — not injected,
  because it has no dependencies and the lifetime it wants is already that object's (one login
  screen), and because it would otherwise be a seventh constructor parameter against detekt's
  `allowedConstructorParameters: 6`. Three decisions in it:
  - **The wait is spent before the next attempt, inside the call the spinner is already up for**,
    so there is nothing for the user to dismiss. Three attempts are free — the first
    failures are typos — then 1s, 2s, 4s, 8s and a cap. The cap is what keeps it a backoff rather
    than a lockout: eight seconds is ~7 attempts a minute, useless for a password list and still a
    wait a person will sit through.
    **3.10 read "nothing to dismiss" as "no new UI state", and 5.5 is that being wrong**: a login
    that silently got slower says nothing about why, so the wait is *announced* as it starts.
    The vehicle is an **`onThrottleWait: (Duration) -> Unit` parameter** on the two throttled
    `SetupRepository` methods, defaulted to `{}` — not a flow beside the repository, because the
    state exists only for the duration of one call and a flow would have needed a lifetime, an
    initial value and a clear that a parameter does not. It is the general shape for anything a
    repository learns *mid-call*: the call is already the scope. `LoginUiState` then carries
    `throttleWaitSeconds` (a number, for the `<plurals>`) with `isThrottled` derived from it, and
    `onConnectClick` clears it in one place after the `when` — the notice belongs to the call, not
    to any of the five outcomes.
  - **Only a credential the instance weighed and refused counts.** A transport failure is not a
    guess, and `TotpSessionExpired` weighed no code at all — throttling either would slow down the
    one case where retrying is right.
  - **`totp.php` shares the counter, and needs it more than the password does**: six digits
    against `verify($code, null, 15)` is 31 valid codes out of a million at any instant, on a
    server that counts nothing. `connectWithApiKey` is deliberately *not* throttled — nobody
    reaches a 32-character key by retrying, and the recovery route must not get slower.

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
  categories/     data domain dto mapper  \  Reference data. Identical CRUD shape — see §3.4 on
  paymentmethods/ data domain dto mapper   | core:crud. No feature:currencies module: that data
  household/      data domain dto mapper  /  stays inside subscriptions/ — see §3.4's note.
                                              No `ui` of their own: M7 (`docs/CHECKLIST.md`)
                                              surfaces all three as pickers inside
                                              `feature:subscriptions:ui`, confirmed building 7.2.
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
  `font` objects whether or not the module has any such resource, so `RDrawable` would have
  compiled even before `uikit` held any drawables. It now holds one: `wallosmobile_logo.png`
  (`uikit/src/commonMain/composeResources/drawable/`), copied verbatim from
  `art/wallosmobile_logo.png` — no resizing, matching MealieMobile's `ic_icon.png` precedent of
  shipping the source PNG as-is rather than pre-scaling it.

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
several obvious ones are absent (`Subscriptions`, `Payment`, `Visibility`/`VisibilityOff`,
`FilterList`); `ArrowBack`, `List` and `Send` are `Icons.AutoMirrored.Filled.*`. Several screens
wanted an icon that isn't there, so **assume it's missing and check** — a `TextButton` with a word
in it is often the cheaper answer (the login password toggle is Show/Hide text for exactly this
reason). A module that genuinely needs more declares
`jetbrains.compose.icons.extended` itself (the `feature/NAME/ui` block above), rather than
growing the convention plugin for one screen.
**`Icons.Filled.Add`, `.Edit` and `.Delete` are all in the set**, confirmed by `unzip`-and-`ls` on
the resolved `material-icons-core` jar's `androidx/compose/material/icons/filled/` directory
(7.6, 7.7) — the FAB and the detail screen's edit/delete actions needed no
`material-icons-extended` addition. Treat "obvious icons are usually missing" as a reason to
check, not as a verdict on any icon in particular.

### 3.4 `core:crud` — the one deliberate deviation

Categories, household members and payment methods are three separate feature modules (matching
Taiga's granularity, since each gets its own screen), but their API contract is byte-identical:
`get_*.php` returning `[{id, name, …, in_use}]`, and `set_*.php` with `action=add|edit|delete`,
differing only in field names and the ID parameter alias.

**Currencies do not get a fourth module here.** The original sketch below counted four, but by the
time Phase 3 was decomposed (§10) `feature:subscriptions` already had a full read path for
currencies — 2.3's `currencySymbol` join and 3.11's `observeCurrencies` cache — so a
`feature:currencies` module would duplicate it for no caller. Phase 3's currency picker reads that
existing flow; a standalone module with `add`/`edit` (rate maintenance) is Phase 5 management-screen
work, and it can sit on `core:crud` the same way the other three do when it lands.

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

Each feature supplies its endpoint path, ID parameter alias and DTO. This keeps three modules'
data layers at roughly 30 lines each instead of 150, and gives one place to encode the
"deleting an in-use item fails with `<Resource> in use`" rule.

**Built in 7.1, one type past the sketch above.** `getAll`/`add` can't be generic over `T` with
only `CrudResource` and `CrudApi` — the list's wrapper key (`categories`, `members`, …) and the id
alias (`categoryId`, `memberId`, …) differ per resource, and kotlinx.serialization has no way to
parameterize a `@Serializable` field name. `CrudEndpoint(getPath, setPath, listKey, idParam)`
carries both, and the one implementation, `WallosCrudApi<T>`, decodes every response to a raw
`JsonObject` and pulls the list or the created id out by name rather than through a per-resource
response DTO. A feature composes it by delegation — `CrudApi<CategoryDTO> by
WallosCrudApi(apiClient, ENDPOINT, CategoryDTO.serializer())` — rather than reimplementing the
four calls. The "in use" delete failure needed **no** code here at all: `WallosErrorMapper`'s
`title.endsWith(" in use")` branch (§4.2) already maps it to `WallosError.InUse` for every
endpoint, `core:crud`'s included, since it throws straight out of `WallosApiClient.post`.
`core:crud` itself is `kmp.library` + `kmp.serialization` only — no `kmp.di`, since nothing in it
is Koin-scanned (`WallosCrudApi` is constructed by each feature's data module, the same
not-injected reasoning §6.1 gives `LoginThrottle`), matching the plugin set `domain`/`dto` modules
use rather than `data`'s.

**7.2 fixed the rest of the shape, for the two catalog modules still to come; 7.3 confirmed it
travels.** A DTO satisfying
`CrudResource` (`CategoryDTO : CrudResource`) needs `api(projects.core.crud)` in the `dto`
module's `build.gradle.kts` — the interface is part of the DTO's own public supertype list, so
`implementation` would hide it from any consumer that works with the DTO as a `CrudResource`.
The repository itself has **no cache**: unlike `SubscriptionsRepository` (§3.x, 3.4), reference
data has no offline requirement in this milestone, so `CategoriesRepository` is a plain
`resultOf`-wrapped round trip per call, not `observe*`/`refresh*`.

**7.5 did not put `set_subscriptions.php` on `core:crud`, despite the identical `action=
add|edit|delete` shape.** `CrudApi`/`CrudEndpoint` model a resource with one name field and one id
alias that is *also* the response key on add; a subscription write has ~18 possible fields, and
its add response key (`subscriptionId`) is fixed while the edit/delete id param accepts three
aliases (`id`/`subscriptionId`/`subscription_id`) — an asymmetry `CrudEndpoint` has no field for.
`SubscriptionsApi` grew three hand-written methods instead, mirroring `WallosCrudApi`'s envelope
handling (decode to `JsonObject`, pull the field by name) rather than reusing it. This needed
`feature:subscriptions:data` to add `kmp.serialization` — the module had never decoded a raw
`JsonObject` before 7.5, only DTOs, so `kmp.network` alone (which it already had) wasn't enough.

**7.6 is where `FabConfig` stopped being parked.** 1.8 trimmed it from the shell with "no writes
before Phase 3"; it is now a `RouteConfig` field the same shape as MealieMobile's
(`None`/`Standard(icon, contentDescription, navigateTo)`), read by `AuthenticatedMainScreen`'s
`Scaffold(floatingActionButton = …)` and never offline-gated — navigating to the editor is not a
write, only its Save button is, so `LocalIsOffline` belongs there and not on the FAB. The
subscription editor ViewModel took exactly the five dependencies the checklist step counted (the
four reference-data repositories plus `SavedStateHandle`) by design: it is add-only in 7.6, with no
`subscriptionId` at all, so 7.7 is what decides how a sixth (an optional id, to make it the add/edit
form the title already promises) lands without tripping detekt's constructor-parameter ceiling.
Every picker (currency, category, payer, payment method, and the cycle enum) reuses one
`EditorPickerUiState` (selected id + options + callback) rather than three loose UI-state fields
each — the same "fold three parameters into one state object" move `SubscriptionsFilterUiState`
already made, and what keeps the shared `PickerField` composable at four parameters instead of
seven. `ExposedDropdownMenuBox`/`ExposedDropdownMenu` are new to the repo here — no
dropdown/menu component existed before 7.6 — and are the precedent 7.7's edit form and Phase 5's
catalog screens should reach for rather than a hand-rolled bottom sheet. Both are *members* of
`ExposedDropdownMenuBoxScope`, resolved by the implicit receiver inside `ExposedDropdownMenuBox {
}`'s content lambda — like `BoxScope.matchParentSize()`, there is no top-level symbol to import, and
trying to import one fails.

Reusing `HtmlUnescaper` (CLAUDE.md's "Wire text needs unescaping" note) makes
`feature:categories:mapper` the repo's **first cross-feature dependency**:
`implementation(projects.feature.subscriptions.mapper)`, since a fourth catalog `HtmlUnescaper`
copy is exactly the duplication CLAUDE.md rules out. Because that line is `implementation`, not
`api`, a downstream module that constructs a real `CategoryMapper` — `feature:categories:data`'s
own repository test — cannot see `HtmlUnescaper` through `categories:mapper` alone and needs the
same `implementation(projects.feature.subscriptions.mapper)` line itself. 7.3 and 7.4 hit this
identically.

**`AppModule`'s `includes` line is necessary but not sufficient** — `composeApp` also needs an
ordinary Gradle dependency on the new feature's `data` and `mapper` modules
(`composeApp/build.gradle.kts`), or `Koin.kt` fails to compile with `Unresolved reference` before
the graph is ever built. 7.2 added that line without remarking on it; 7.3 is where it got named,
since `:androidApp:compileGplayDebugKotlin --rerun-tasks` is what catches a forgotten one, same as
it catches a forgotten Koin re-scan.

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
workflows, and there is nothing here to share it with. No secrets either — the `gplay`/`fdroid`
flavors added in 6.2 carry no signing configs and no `google-services.json`, so a debug build
needs nothing restored. `assembleDebug` itself is unaffected: AGP still creates it as an aggregate
task depending on `assembleGplayDebug` + `assembleFdroidDebug`, so CI's own command didn't need to
change.

Two deliberate omissions: **Kover/Codecov is not in CI** (the upload wants a `CODECOV_TOKEN` this
repo doesn't have; `koverXmlReport` stays a local command), and `paths-ignore` skips `**.md` and
`docs/**`, so a **docs-only commit produces no run** — an absent run is not a failed one.

**There is still no coverage floor, and 3.12 measured why an aggregate one would be the wrong
instrument.** The project reads 48.8% line overall, which sounds like a floor worth setting until the
report is read per package: **388 of the 2012 measured lines are Room's generated `*_Impl` classes at
0%**, unreachable from a host test because Kover never sees the instrumented DAO suite above, while
every hand-written class in `core/storage/db` is at 100%. The remaining 0% is Composables (`uikit`,
and the list's widget packages). The layers a floor would be *for* — repositories, mappers,
ViewModels, `core:api`, the formatters — already sit at 82–100% without one. So the useful gate is
scoped to those modules rather than aggregated over the project, and setting it edits `kover { }`,
which is a guardrail tripwire (§3.6) and needs a `Gate-change:` line.

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

**`postMultipart` shipped in 7.9, as `MultipartFile` rather than the sketch's `FileUpload`** — a
plain class (`fieldName`/`fileName`/`mimeType`/`bytes`; not a `data class`, since a `ByteArray`
property gives one a reference-equality `equals`/`hashCode` pair that looks structural and isn't).
It builds on `httpClient.submitFormWithBinaryData` + a `formData { }` block: the caller's own
`FormParams` (with the stored key already injected, same as `post`) go in as plain string parts,
and the file is one more `append(fieldName, bytes, Headers.build { ContentType; ContentDisposition
= "filename=…" })`. `SubscriptionsApi.addSubscription`/`editSubscription` take an optional
`logo: MultipartFile? = null` and switch to `postMultipart` only when it's set — every pre-existing,
non-logo call site (and its tests) needed no change. Reused by nothing else yet;
`feature:paymentmethods`' `paymenticon` upload (7.4's deliberately out-of-scope half) is the next
caller whenever Phase 5 builds a picker for it.

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
rather than Mealie's autodiscovery) was v1's one deferred piece, and 3.7 landed it with
certificate trust — both clients now take an engine rather than letting Ktor find one (§4.5).

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

Similarly on the decoding side (no `core:serialization` module exists — 1.2 needed none): `notes`
is an array on every endpoint, `get_user.php` included — a claim this doc carried wrong until
checked live while scoping Phase 5 (§7.2 corrected the same error; `docs/WALLOS_API.md` §1 has the
`curl` evidence). Not modelling `notes` at all is enough; no lenient-serializer workaround was ever
needed for it. `Json { ignoreUnknownKeys = true; isLenient = true }` is still **mandatory**, not a
nicety, for the actual reason: responses are raw DB rows and self-hosted instances sit at different
migration levels.

### 4.5 Self-signed certificates

Port `CompositeTrustManager` + `TrustedCertStorage` + the trust-prompt flow from TaigaMobileNova
(`docs/features/private-cert-trust/` there) essentially unchanged. This matters *more* for Wallos
than for Taiga: nearly every instance is self-hosted behind a homelab certificate. Certificate
pinning is not an option; a trust prompt on first connect is.

What 3.7 kept from that port, and what it dropped:

- **The two properties the survey was written for stay.** A pin is `(host, fingerprint)`, so a
  certificate accepted for one server never authenticates another that presents the same bytes;
  and `checkValidity()` still runs **on a pin hit**, so accepting a certificate once is not
  accepting it after it expires. Both are tested.
- **The failure carries the certificate as a *cause*, not as a platform type.** JSSE lets a trust
  manager throw only `CertificateException` — but it says nothing about what that exception
  wraps, so `CompositeTrustManager` throws
  `CertificateException(UntrustedCertificateException(pendingCertTrust))` and
  `Throwable.findPendingCertTrust()` (`core:domain`, `commonMain`) digs it back out of however
  many layers the TLS stack added. That replaces Taiga's three moving parts — an `androidMain`
  `CertificateException` subtype, a portable twin of it, and an `expect`/`actual` mapper between
  them — with one `commonMain` class and one extension function, and it means **nothing catches
  the type**: a caller asks a throwable it already has for a `PendingCertTrust`.
- **A hostname the certificate doesn't cover is not offered for trust**, which was Taiga's Phase 7
  bug: hostname verification is a separate step downstream that no pin can satisfy, so accepting
  there would collect a decision that can never produce a working connection. It rethrows the
  original rejection rather than earning its own exception type — the only thing riding on the
  distinction here is whether the prompt appears.
- **Storage pins strings, not certificates.** `TrustedCertStorage` holds `"host|fingerprint"` in
  the module's shared DataStore. Taiga widened to a JSON list of `PendingCertTrust` for a
  settings screen that lists and revokes pins; nothing here plans one, and the full certificate
  is still what the *prompt* shows. It also means disconnect leaves pins alone —
  `ApiKeyStorage.clear()` removes its own key, and a pin is a statement about a server's
  certificate, not about the account.
- **The engine is now built, not autodiscovered.** `createPlatformHttpClientEngine` (`expect` in
  `core:api` `commonMain`, `actual` in its first `androidMain`) wraps the composite manager in an
  `SSLContext` and hands it to `OkHttp.create`. Both clients take it, the web-session one
  included: onboarding is the first thing to touch the server, so it is where an untrusted
  certificate surfaces. Hostname verification is left at OkHttp's default — this replaces *chain*
  trust and nothing else.
- **Coil did not go through it until 4.5.** The logo loader built its own client by autodiscovery
  (2.4), so on an HTTPS instance with an accepted-but-still-private certificate the data loaded and
  the logos did not — and it was **silent twice over**: nothing reported the failed load, and
  `SubscriptionLogo`'s initial-letter placeholder branched on an **empty** filename rather than on a
  load failure, so every row drew a blank gap where a fallback already existed. Both halves are 4.5:
  - `AppModule.provideImageLoader` builds a `@Single ImageLoader` whose components name
    `KtorNetworkFetcherFactory(client)` explicitly, over a **second, minimal** `HttpClient` on the
    same `createPlatformHttpClientEngine`. The engine is the only part shared: the `@Single
    HttpClient` carries `Logging(LogLevel.ALL)` in debug, which would dump every logo's bytes into
    logcat, and a retry predicate written for `get_*.php`.
  - `:androidApp`'s `WallosApp` implements `SingletonImageLoader.Factory` and hands that instance
    back. Coil asks the `Application` before it builds a default loader, so this cannot race the
    first `AsyncImage`; a `SingletonImageLoader.setSafe` from inside the composition could.
  - `SubscriptionLogo` is `SubcomposeAsyncImage` with a composable `error` slot — `AsyncImage`'s
    `error` takes a `Painter`, and the fallback is an initial to lay out. A failed load and a
    missing filename now draw the same placeholder.
  **The cost was one `api` dependency**, and it is a Koin compiler-plugin constraint rather than an
  architectural choice: `composeApp` exposes `core:storage` as `api` because the plugin re-checks
  `AppModule`'s own definitions at the `startKoin` call site in `:androidApp` — see §6.2.
- **4.5 fixed the certificate; 5.6 fixed the other reason a logo stays a placeholder.** Coil3's
  `AsyncImagePainter` decides whether a model "changed" through `AsyncImageModelEqualityDelegate
  .Default`, which compares an `ImageRequest`'s `data`, `memoryCacheKey`/`memoryCacheKeyExtras`,
  `diskCacheKey`, `sizeResolver`, `scale` and `precision` — never the model's own `equals`. A bare
  `logoUrl` string never differs between recompositions, so a server coming back left every row
  already in `Error` exactly where it was, no matter how many times the surrounding `ImageRequest`
  was rebuilt. The fix is a `logoRefreshToken: Int`, bumped only by a successful refresh and set as
  a `memoryCacheKeyExtra` on an explicit `ImageRequest` — it changes the request's identity without
  touching `logoUrl`, the disk cache key, or the memory cache key it decorates, so an
  already-successful row is a cheap memory-miss-then-disk-hit and only a genuinely failed one pays
  for a network round trip.
  **"Only a successful refresh" turned out too wide for the detail screen.** A fresh
  `SubscriptionDetailViewModel` is built on every open (this file's own nav3 section), so its
  construction-time refresh bumped the token every time even when nothing had failed — the
  cheap memory-miss-then-disk-hit round trip 5.6 accepted as the cost of a real recovery was
  instead paying on every open, and read as a flicker. `SubscriptionDetailViewModel` now bumps only
  when a `lastRefreshFailed` flag (a plain `var`, set in `onFailure` and read in `onRefreshed`,
  both only ever reached from `viewModelScope` launches on `Main.immediate`) is `true`, so a
  construction-time or already-fine refresh leaves the token alone. `SubscriptionsViewModel` (the
  list) is unaffected — it isn't rebuilt per visit, so its unconditional bump only fires at startup
  and on an explicit refresh or retry, not on every screen open.

The prompt itself is 3.8, and it lives on the **login screen only**:

- **The pin is written through `SetupRepository.trustCertificate(PendingCertTrust)`**, not through
  `TrustedCertStorage` from the ViewModel — 3.1's rule, since this feature has a `data` layer to
  route through. That makes `core:domain` an `api` dependency of `feature:setup:domain`, which is
  also how `PendingCertTrust` reaches the `ui` module's state.
- **`LoginUiState.pendingCertTrust` non-null *is* the dialog**, and the retry after accepting is
  `onConnectClick()` re-read from state rather than a captured lambda. Both paths are driven from
  that one state and the dialog is modal, so nothing can have changed underneath; the failed
  attempt stored nothing either. Declining sets its own message — `getErrorMessage` would send the
  user to Settings for a certificate this screen is already showing them.
- **Nothing else in the app can raise the prompt, so elsewhere the copy carries it** (5.1). A
  certificate that rotates after onboarding fails every refresh, and the list screen has only 3.5's
  stale banner — so `getErrorMessage` asks `findPendingCertTrust()` in its non-`WallosError` arm and
  names the certificate, pointing at Disconnect. `LoginViewModel.onFailure` asks the same question
  *first* and raises the dialog, which is why the branch is invisible there: the copy is for screens
  with no trust surface. A prompt on those screens would put a pin write outside `SetupRepository`
  and is still open.

### 4.6 Version gating

Originally planned as: store the `api/status/version.php` result at setup and after each
successful reconnect, and gate `get_period_budget`, `set_budget`'s period fields, `logo_variant`
and `square_icons` behind it, with `WallosError.UnsupportedEndpoint` only as the runtime backstop
for a stale stored version.

**`get_period_budget` (M8, plan §8 Phase 4) settled on the backstop alone, with no
`VersionStorage` built at all.** Neither `WALLOS_API.md` nor the live PHP
(`api/subscriptions/get_period_budget.php`) names a minimum version to compare against, so there
is nothing for a stored value to be compared *to* — `WallosEnvelopeParser` already turns any 404
into `WallosError.UnsupportedEndpoint` for every endpoint, and `DashboardRepository` lets that
surface untouched. The one place this shows up outside the network layer: `DashboardViewModel`
(`feature:dashboard:ui`) matches on that specific `WallosError` case directly — to decide "hide
the budget card" rather than "show an error" — which is why `core:domain` is a **main-source**
dependency of that module and not the test-only one every other `feature:*:ui` module has. This is
the sanctioned shape for a version-gated *card*, as opposed to `getErrorMessage`'s job of turning
any other failure into a message.

`set_budget`'s period fields, `logo_variant` and `square_icons` (Phase 5) are still unbuilt, and
still the candidates for a real `VersionStorage` — only if one of them turns out to need a
proactive check ahead of the call, which `get_period_budget` didn't.

### 4.7 `core:storage`

Grouped here because §4.1 defines the two interfaces `core:api` consumes and §4.5 adds a third.
One `PreferenceDataStoreFactory` store, file `wallos_storage`, shared by every storage class in
the module; each owns its keys, and **`ApiKeyStorage.clear()` removes its own key rather than
clearing the file**, so disconnect leaves the server URL the user typed.

**`clear()` also empties the Room cache** (3.4). The cached rows belong to the account whose
credential is being thrown away, and it has *three* callers, not the one the wording suggests:
disconnect, and **both login paths**, which clear the stale key before validating a new one (§1.1).
A cleaner called from `feature:settings` would have covered the first and silently missed the other
two — logging in as a second account would have shown the first account's subscriptions. So the
eviction sits at the single point a key is dropped, `ApiKeyStorageImpl` takes the DAOs (three since
3.11 added the conversion row, which explains the dropped account's prices and nobody else's), and
"no key ⇒ no cache" is an invariant rather than a convention. Nothing else invalidates the cache:
a schema change hits the destructive fallback below, and a whole-list refresh already drops rows
the server no longer sends.

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
- **`ThemeStorage` is the counter-example to `clear()`** (4.2). Everything else in this module is
  either account state (the key, the cache) or server state (the URL, the pins); the palette
  belongs to the *install*, so it survives disconnect — sharing the file costs it nothing, since
  `clear()` removes only its own key. Stored as `ThemeMode.value` (`"system"`/`"light"`/`"dark"`)
  rather than the ordinal, and an unrecognised value reads as the default instead of throwing.
- **`ServerUrlStorage.serverUrl` is not `suspend`** (§4.1): the implementation blocks on the first
  read via `runBlocking`, then serves an in-memory cache that `saveServerUrl` keeps current.
  `runBlocking` is reachable from `commonMain` here only because Android is the sole target and
  the metadata compilation therefore resolves the JVM variant of coroutines.

#### The Room cache

`WallosDB` (`db/`) holds `SubscriptionEntity`, `CurrencyEntity` and — since 3.11 — the one-row
`PriceConversionEntity`, with a DAO each. A snapshot of the server, never a source of truth, so
there is no dirty-write state to reconcile. `version = 3` (3.11 added the third table; 7.7 added
six columns to `SubscriptionEntity` for the editor's pre-fill, no new table),
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
  the *next* refresh's resolution — §7.2's second round trip per call — not for this read. Since
  3.4 that is what makes a detail refresh one call instead of two.
- **Both of `SubscriptionDao`'s reads are `Flow`s** — 3.3's one-shot `getById` became `observeById`
  in 3.4, because the row the detail screen holds is the row a list refresh rewrites underneath it
  and nothing else would tell the screen. The entity↔domain mappers live in
  `feature:subscriptions:mapper` (`SubscriptionEntityMapper`, `CurrencyEntityMapper`), which is
  what keeps this module free of a `feature:*:domain` dependency.
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

That rules out `SavedStateHandle` as a *route argument* vehicle, not as a vehicle. 5.2 injects one
into `SubscriptionsViewModel` for state that belongs to no route at all — the filter and sort, which
the back stack was carefully serializing around while they reset on every process death. It takes
the same `@InjectedParam` (Koin builds it from the `CreationExtras`, not from the graph), and there
`KoinGraphTest` *does* enforce it, since `SavedStateHandle` is not one of `verify()`'s whitelisted
primitives.

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
                // Sketch only — `FilterList` is not in `material-icons-core`, so the real
                // subscriptions bar uses a `TopBarActionTextButton` reading "Filter" (3.6).
                TopBarActionVectorButton(Icons.Default.Refresh, onClick = ::openFilters)
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
| a throwable carrying an `UntrustedCertificateException` | the certificate isn't the one you trusted | **Disconnect** |
| anything else that isn't a `WallosError` | couldn't reach that server | the **URL** |
| `Unauthenticated` | the instance didn't accept this key | the **key** |
| `Forbidden` / `NotFound` / `Validation` / `InUse` / `Server` | one message each | — |

The `WallosError` branch is an **exhaustive `when` over the sealed class**, not a `when` over
`Throwable` with an `else`: a new error type has to fail the build rather than quietly render as a
connection problem. The certificate row (5.1) is the one thing the `else` arm inspects before
falling through, because it is the only transport failure the user can act on and the fallback copy
argues *against* the action — see §4.5. Errors that belong to one feature — `ApiKeyNotFound`, say — are handled in
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
catalog but is **not used** — `koinViewModel()` alone is enough. Don't add it without a reason.

6. **This repo wires no `rememberViewModelStoreNavEntryDecorator()` either**, contrary to what the
   line above used to claim: `MainNavHost` passes no `entryDecorators`, so `NavDisplay` runs on its
   default of `rememberSaveableStateHolderNavEntryDecorator()` alone. 5.2 went looking for the
   defect that implies — an activity-scoped ViewModel store would hand a second detail route the
   first route's ViewModel — and there isn't one: measured on device, the list ViewModel survives a
   detail round trip untouched while each detail route builds its own (`Refreshing subscription 4`,
   then `26`, in logcat). So the decorator list is not the authority on ViewModel lifetime here;
   the log line is, and it costs one `adb logcat`.

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
  **Shape, settled by 8.3's `DashboardHomeUseCase`** (the app's first): interface + `Impl`,
  `@Factory(binds = [...])` — unlike TaigaMobileNova's use cases (`EpicDetailsDataUseCase` et al.,
  which wrap every composed call in one outer `resultOf`), a use case whose sources fail
  *independently* returns a plain data class of per-source `Result`s instead, so one endpoint
  being down (`WallosError.UnsupportedEndpoint`) can't blank out the sources that succeeded. A
  helper the use case composes over but that has **no dependencies of its own** (here,
  `UpcomingPaymentsCalculator`, and 10.4/10.5's `SubscriptionStatsCalculator` the same way) is
  constructed directly inside the `Impl` rather than injected — the same "stop injecting it" case
  as the cache-repository bullet below, just reached from a use case instead of a repository. A
  *derived* field built from two independently-failing sources (10.5's `monthlyBudget`/
  `subscriptionStats`, each needing `monthlyCost`'s unwrapped amount) is nullable and left `null`
  when either source failed rather than a zeroed-out instance — a fabricated 0 there would read as
  "no cost" rather than "unknown," so the `Impl` gates the derivation on `Result.getOrNull()`
  rather than defaulting the missing operand. A domain module gaining its first real use case is also gaining
  its first Koin content: every domain module up to 8.3 scanned to zero `@Single`/`@Factory`
  definitions (8.1's own note said so), so the module needs `alias(libs.plugins.wallosmobile.kmp.di)`
  added to its `build.gradle.kts` and a new `<Feature>DomainModule` (`@Module @Configuration
  @ComponentScan`) — that module didn't need one before and won't need it again if it stays at
  zero.
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
- **The plugin also re-*checks*, at that same call site, every definition whose declaring
  `@Configuration` class it can read** (4.5). From `:androidApp` that is `AndroidModule` and
  `AppModule` — a direct dependency — and *not* the `includes`, which arrive through `composeApp`'s
  `implementation` dependencies. So a definition declared in `AppModule` itself must have both its
  parameter types **and the definitions binding them** resolvable from `:androidApp`'s classpath,
  or it fails to compile with `[KOIN-D001] Missing dependency`. That is the whole reason
  `composeApp` exposes `core:storage` as `api`: `provideImageLoader` takes a `TrustedCertStorage`.
  A definition inside an `includes` module is never re-checked there, which is why
  `NetworkModule.provideHttpClient` takes the same type freely — and moving the definition from a
  scanned class to a module function does *not* help, since the check is on the definition, not on
  its shape. Anything new in `AppModule` that reaches into another module pays this again; a
  definition that belongs to a module already in `includes` does not.

And one thing the graph test **cannot** see (2.5): `verify()` whitelists `String`, `Int`, `Long`
and `Double` outright (`Verify.primitiveTypes`), so a primitive constructor parameter is reported
verified whether or not it is annotated. A route argument therefore needs `@InjectedParam` on the
ViewModel's constructor property for its own sake — without it the compiler plugin looks for an
`Int` definition in the graph and the screen crashes at first injection, with no gate in between.
The whitelist is what makes this blind spot narrow, though: `SavedStateHandle` (5.2) needs the same
annotation for the same reason and is *not* whitelisted, so there the graph test does fail.

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
as `api`, so neither is ever declared per module. `MockEngine` matters because the real engine is
`androidMain`-only — whether it is autodiscovered or, since 3.7, built by
`createPlatformHttpClientEngine` (§4.5), a host test that wants an `HttpClient` has to supply an
engine itself.

**There is a third source set, and it is cheap** (3.7). `commonTest` cannot see an `androidMain`
class, so code that only exists there — `CompositeTrustManager` — is tested from
`src/androidHostTest/`, which the same `testAndroidHostTest` task runs and which inherits
`commonTest`'s dependencies (`kotlin.test`, Turbine, `:testing`) through the test source-set tree.
The one wrinkle is lint: detekt's per-rule `excludes` list `commonTest` and friends by name and
not `androidHostTest`, so `FunctionNaming` applies there — its test names are camelCase, the same
concession 3.3's device tests make, rather than widening the detekt config for a source set name.

**Instrumented tests exist, as of 3.3, and only where a host test is impossible.** `core:storage`
has an `androidDeviceTest` source set for the Room DAOs (§4.7) — not a preference for realism but
the only option: neither `Room`'s builders nor `BundledSQLiteDriver`'s native library can be
reached from `testAndroidHostTest`, and Robolectric remains ruled out for the reason above. Its
cost is real and should be weighed before the second one: it needs a running emulator, it is
outside `allTests`, and CI therefore doesn't run it (§3.5). `:testing` is *not* on its classpath —
`configureTests()` wires `commonTest` only — so a device test either declares what it needs or
does without.

**The second one is named, as of 3.12: the subscriptions list screen.** Every Composable in the
project is at 0% coverage, so the four derived states 3.5/3.6 built (stale, failed, empty, no-match)
plus 3.11's conversion banner and 3.8's trust dialog are verified only by manual emulator runs like
3.12's — which is precisely the "logic outgrows its ViewModel test" bar 2.7 set for reaching here.
What still has to be decided when it lands is whether it earns an emulator job in CI, since a second
device-only suite doubles the surface no other session's commit can feel.

**A `@Dao` is faked by hand like anything else** — it is an interface, so a `commonTest` fake needs
no Room runtime, and `replaceAll` is a `@Transaction` method *with a body*, so only the abstract
members have to be implemented. Back the fake with a `MutableStateFlow` if the code under test
observes it: the real `@Query` `Flow` re-emits on every write, and a fake returning `flowOf(rows)`
will pass a test that the app then fails.

**`MainDispatcherRule` is unconfined, so a state that only exists *while* a call is in flight is
invisible** — the ViewModel's `launch` runs to completion before the constructor returns, and
`uiState.value` is already the final one. To assert a loading state, give the fake a
`CompletableDeferred` the suspend function awaits, read the state, then `complete()` it (3.4's "an
empty cache keeps the spinner up until the refresh answers").

**Asserting on a captured `MultiPartFormDataContent`'s parts needs `@OptIn(io.ktor.utils.io.
InternalAPI::class)`** (7.9) — its `parts: List<PartData>` property carries that annotation, even
though building the request via `formData { }` needs no opt-in at all. And `PartData.headers[key]`
(the `[]` accessor) only ever returns the *first* value under a header name; `formData { append(key,
bytes, Headers.build { append(ContentDisposition, "filename=…") }) }` lands a **second**
`Content-Disposition` value beside the `name=` one `formData` itself adds, so reading it back needs
`headers.getAll(ContentDisposition)`, not `headers[ContentDisposition]`.

**`:testing` is excluded from linting** (`lintingExclusions` in `build-logic/.../Quality.kt`, plus
`.editorconfig`), so there is no `:testing:ktlintFormat`/`:testing:detekt` task at all — asking for
one fails with "task not found", which is the config working, not a broken build.

**A test fixture factory grows by `.copy()`, never by parameters.** detekt's
`allowedFunctionParameters: 5` applies to `commonTest` too, and `ignoreDataClasses` exempts the data
class, not the `private fun subscription(...)` that builds one — so a sixth parameter fails the
build and the reflex fix, `@Suppress`, is a guardrail tripwire costing a `Gate-change:` line for a
test helper. Build one canonical fixture and `.copy()` off it.

**`UnusedParameter` applies to `commonTest` too**, so a test that ignores a callback passes `{ }` at
the call site — a named `private fun ignore(wait: Duration) = Unit` fails detekt.

**`LongParameterList`'s `allowedConstructorParameters: 6` does not, in practice, gate a fake's
constructor** — `FakeSubscriptionsApi` sat at 9 named/defaulted params pre-7.5 and reached 13 after
it, both real `./gradlew detekt` runs, both clean. Don't pre-split a fake's constructor to dodge it.

**A fake's settable field must not be named after the method it feeds.** `var baseUrl` beside
`override fun getBaseUrl()` is a "platform declaration clash" — the property's getter compiles to
`getBaseUrl()` too. Name the field for what it holds (`var url`), not for the method.

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
  screen that needs them (2.1). **7.7 is that arrival**: the editor's pickers need
  `categoryId`/`paymentMethodId`/`payerUserId` (the list/detail screens only ever needed the
  *resolved* `categoryName`/`paymentMethodName`/`payerName`) and its switches need
  `autoRenew`/`notify`/`notifyDaysBefore`, so all six joined `Subscription` — cached on
  `SubscriptionEntity` too, since the point was pre-filling the editor from the cache with no
  extra round trip. The rule cuts both ways: it justified 2.1 leaving them out, and 7.7 adding
  them back once a screen actually renders them.
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
  rates returns unconverted prices and an empty `notes`. ~~detect by comparing `currency_id`
  against the user's `main_currency`~~ — **that detection does not work**, and 3.11 replaced it;
  see "Currency conversion" below. A failed logo fetch reports success — re-read `logo` after a
  write to confirm.
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
modules §2 lists arrive with Phase 5's server display settings. Disconnect itself needs no navigation
and no success signal — `clear()` flips `isConnected` and the startup branch above swaps the tree, so
the screen is gone before it could render a spinner. A write that fails is logged and leaves the user
connected, which is the truthful outcome and the reason there is no error state.

The module stayed `ui`-only through 4.3, which added the **Interface** sub-screen: `ThemeStorage` is
the same one-seam case `ApiKeyStorage` is, so `InterfaceViewModel` takes it directly. What the root
screen did gain is *navigation* — an `onInterfaceClick` plain parameter (a pure navigation callback,
so not on the UI state) wired by `settingsEntry`, which therefore now takes the `Navigator` it was
originally written without. 4.4's **About** sub-screen is the third such reach (`AppInfoProvider`),
and it is still `ui`-only: three one-call seams are not a `domain` layer, because there is nothing
between the call and the screen for an interface to hide. The count that matters is *repositories*,
not reaches.

**A platform seam hands back facts, not rendered text** (4.4). `AppInfoProvider` gained
`versionName()` and `versionCode()` rather than MealieMobile's `getAppInfo(): String`, because the
`androidApp` implementation is the one class in the graph a host test cannot construct and the one
place `:strings` resources cannot be resolved — `about_version_value` (`%1$s (%2$d)`) and the
Debug/Release word are both `stringResource` calls in the composable. A seam that pre-renders moves
presentation into exactly the class that can neither be tested nor localised.

Two consequences of `clear()` keeping the server URL (§4.7) that are easy to conflate: the URL
really does survive, but the login screen does **not** prefill it, so re-connecting still means
retyping the address. Seeding `LoginUiState.serverUrl` from `ServerUrlStorage` is the fix and it
belongs to `feature:setup:ui`; until it lands, don't write copy that promises a one-field re-login.

**Non-obvious dependency:** the list can't render a price without currency symbols. `price` comes
with a `currency_id`, and the symbol lives in `get_currencies.php`. So the list screen is two calls,
not one — fetch currencies once and map `currency_id → symbol`. (The alternative,
`convert_currency=true` plus the user's `main_currency`, silently returns unconverted prices when
the instance has never fetched exchange rates, so it's the worse default. It is *also* not an
alternative — see "Currency conversion" below: conversion never rewrites `currency_id`, so the
symbol join is needed either way.)

The join lands in `SubscriptionsRepository` as a **`currencySymbol` field on domain
`Subscription`** (2.3), not as a map handed up to the screen: a consumer that has the model has
everything it needs to render a price, and nothing above the repository ever sees a currency list.
Blank for a `currency_id` the instance no longer lists — a deleted currency costs the sign, not the
screen. Two consequences to keep in view:

- ~~**Every repository call is two round trips.**~~ True until 3.4: the currency list was re-read
  per call, so opening the list and then a detail was four requests. The Room cache is the fix, as
  predicted — a **list refresh** is still the two calls, because it is the only thing that fills
  the currency table, but a **detail refresh** is one, resolving its symbol out of that table.
- The subscriptions call goes **first**, so a failure on the resource the user actually asked for
  short-circuits before the second one. Since 3.4 neither table is written until both have
  answered: a half-written cache is worse than the stale one it would replace.

**The second thing the model can't render on its own is the logo.** The wire carries a bare
filename and the full URL is `{base}/images/uploads/logos/{logo}`, so the *ViewModel* builds it —
`feature:subscriptions:ui` takes `core:api` for `BaseUrlProvider` alone, which is the one place the
instance root is normalized (2.4). Blank in, blank out: no logo, or no stored server, yields no URL
rather than a relative one Coil would fail on. Wallos serves that directory **unauthenticated**, so
a plain URL load needs no header plumbing — the module takes `coil.compose` alone. It took
`coil.ktor` too until 4.5, for the autodiscovery that step replaced: the `ImageLoader` is now
configured, in `composeApp`, so the certificate the user accepted covers the logos (§4.5).

**The detail screen re-reads its own row** (2.5) rather than being handed one by the list, and
since 3.4 it reads it *from the cache* and refreshes behind it — one round trip, not two, because
the symbol comes from the cached currency table. Only the id travels in `SubscriptionDetailRoute`.
A field the instance has nothing for — `notes` and `url` are
`""` on every row of the local instance, `start_date` on many — has its whole row left out, since a
label over an empty value reads as a bug rather than as "unset".

Five things the two screens both need live in `feature:subscriptions:ui/widgets/` rather than in
either: `SubscriptionLogo` (parameterized by size), `InactiveBadge`, `cycleText` (which 2.4 had as
a private composable on the card), `StaleBanner` (3.5), and — as `ui/LogoUrl.kt` — the
`BaseUrlProvider.toLogoUrl(logo)` extension below. That last one is a two-line helper over an
injected seam, not a mapper, so CLAUDE.md's mappers-are-classes rule doesn't reach it.

~~**A failed load clears the list** (2.4).~~ **Reversed by 3.4**, exactly as this paragraph
anticipated: with a cache behind the error there is something true to keep, and clearing it would
throw away the only data the app has. A failed refresh now changes nothing but the error.

#### Offline-first, from 3.4

`SubscriptionsRepository` is **`observe*` + `refresh*`**, not `get*`. Reads come off the Room cache
and cannot fail; the network only ever writes to it. Three things follow, and they are the shape
every later feature's repository should copy:

- **The cache is the single source of truth.** A screen renders what the DAO emits, so a refresh
  reaches it through the database rather than around it, and two screens looking at the same row
  cannot disagree. `refreshSubscriptions` is a **snapshot** (`replaceAll`); `refreshSubscription`
  is an upsert of one row that leaves the rest alone — and leaves the row alone on failure, since
  `Unauthorized or Not Found` is an ownership answer as much as a deletion (API doc §3.3).
- **The repository is two classes.** `SubscriptionsCache` holds the DAOs and the entity mappers and
  speaks domain models only; `SubscriptionsRepositoryImpl` holds the API, the wire mappers and the
  order things happen in. detekt's `allowedConstructorParameters: 6` is what forced the split, and
  the line it drew is the right one — everything about the cache being *rows* stops at the first
  class. It is also why 3.11's third DAO and mapper cost the repository nothing: they went behind
  the seam, where they belong, and `SubscriptionsCache` is at the limit of 6 exactly.
- **The ViewModels are cache-first.** The spinner belongs to the *empty* cache alone: cached rows
  dismiss it the moment they arrive, and a refresh runs behind whatever is already on screen.
- **An error means two different screens, and the UI state derives which** (3.5). `isStale` is
  `error` *with* data behind it and renders as a `StaleBanner` above rows that stay put; `isFailed`
  is `error` with *nothing* behind it and still owns the screen. Both are computed properties on
  the existing fields — a ViewModel that *set* a `isStale` boolean would be storing a second copy
  of a fact `error` and `items` already carry, free to disagree with them. 3.5 changed no ViewModel
  at all. The banner takes its reason line from **`LocalIsOffline`** rather than from the error: a
  refresh that never left the device fails as "check the URL and your connection", which points at
  a server that is not the problem. This is the shape every later cached screen should copy.

#### Filter and sort, from 3.6

Both are **client-side over the cache**, which is what 2.3 asked Phase 2b to keep: `SubscriptionsApi`
still sends `api_key` and nothing else, so §3.2's "no `WHERE` clause" SQL bug stays unreachable by
construction, and the sheet works with no network at all. There is no catalog endpoint behind it —
every option is a distinct value of the rows already cached, which is only possible because 2.1 kept
the server-resolved `category_name` / `payer_user_name` / `payment_method_name`.

- **`items` is no longer "what the cache holds"**, and that is the part that propagates. Every
  derived state 3.5 built on `items.isEmpty()` now asks `hasCachedRows` instead — the one new
  *field* on the UI state, and not a second copy of anything, because the filtered list genuinely
  cannot express it. Get it wrong and a filter matching nothing reads as an empty instance; offline
  it turns the stale banner into a full-screen error over rows that are right there. `isNoMatch`
  joins the derived three. **Any later screen that narrows what it draws inherits this rule.**
- **The criteria are `MutableStateFlow`s beside the state**, `combine`d with the DAO flow rather
  than copied into the UI state and read back out of it. So "the filter changed" and "a refresh
  arrived" render through one path, and re-sorting provably costs no refetch.
  **Since 5.2 they also outlive the process**, through a `SavedStateHandle` — the shape above is
  untouched, and the handle is written from a `combine(filter, sort)` rather than from each of the
  three setters, so the saved copy is driven off the flows and cannot disagree with them. The stored
  form is one JSON string under one key, not an encoded `SavedState`: on Android
  `androidx.savedstate`'s `SavedState` **is** `Bundle`, so `encodeToSavedState`, the `saved { }`
  property delegate and `savedState { }` all need an Android runtime — unreachable from a host
  test, and Robolectric is out. `SavedStateHandle()` itself, `get`, `set` and
  `getMutableStateFlow` are pure Kotlin, so a `String` value is testable in both directions while
  anything Bundle-shaped is testable in neither. `SubscriptionFilter`'s `ImmutableSet`s have no
  kotlinx serializer either, so what gets written is a small `@Serializable` companion type
  (`SavedCriteria`) with plain `Set`s — the same DTO-beside-the-model instinct the wire layer uses.
  Decoding it catches `IllegalArgumentException`: pre-v1 stored state is disposable, but
  *discarding* it has to be a default screen rather than a crash in the ViewModel's constructor.
  The UI state's `filters` is *seeded* from the restored values as well as set by `onCached`,
  because the first frame after a restore is drawn before the
  DAO flow has emitted anything.
- **Sorting is a pure class** (`SubscriptionSorter`), because §3.2 fixes the direction *per field*
  — `price` and `id` descend, everything else ascends — and a table like that is worth a test
  rather than a `sortedWith` in a ViewModel. Three of its fields are ids this app never receives,
  so `payer` / `category` / `payment_method` sort by the resolved name instead; `alphanumeric` is
  skipped as §3.2's own alias for `name`. The default is the server's `next_payment`, which is also
  the first time the list's order stopped being the DAO's `ORDER BY id`.
- **An empty selection means every value**, exactly as an omitted parameter does server-side, so
  unpicking the last chip widens back out and no "All" chip is needed. Status is the exception —
  §3.2's `state` is a tri-state, and *All* there is a real third choice.

#### Currency conversion, from 3.11

**The response cannot be read back for this, and that is the whole design constraint.** Both
subscription endpoints overwrite `price` when they convert and leave `currency_id` naming the
currency they converted *from*, with `notes` empty either way (API doc §5.5 has the measured table).
So sending `convert_currency=true` without deciding the symbol yourself is a **regression**, not a
feature: it renders `$29.35` on an amount in euros. What the step protects is not the conversion, it
is that.

- **The instance's own `convert_currency` setting decides whether to send the flag** (`get_settings.php`,
  §3.11), rather than the app deciding for the user. Someone who turned conversion off is asking to
  see real currencies. The read is a **preference, not data**: it runs first because the flag shapes
  the subscriptions request, but a failure degrades to "off" and logs, because a 404 there on an
  older instance must not cost the user their list. "Subscriptions first" still describes the
  failure behaviour.
- **`PriceConversion` is the domain answer**: `isEnabled` + `mainCurrencyId` + `hasRates`, with
  `converts(currencyId)` the question the repository asks per row. Every default is the reading that
  cannot mislabel a price, so "never refreshed" and "nothing converted" are the same state.
- **`hasRates` stands in for a flag no endpoint exposes.** The server gates on `last_exchange_update`;
  the rate table is the observable proxy, because a fresh install seeds every rate at exactly `1`
  and only an exchange update writes both. Where proxy and truth could disagree, `price / 1` is the
  same amount in either currency, so the app's conclusion stays true.
- **It is cached in a one-row Room table**, and both halves of that matter. Cached, because a cold
  offline start has no response to re-derive it from and the prices on screen still need explaining
  — and because the detail refresh must send the *same* flag the list was fetched with or one row
  disagrees with the list it sits in, which would otherwise cost a second round trip per open. One
  row, because this is a property of the **fetch**, not of a currency or a subscription. It goes
  into `ApiKeyStorage.clear()` with the other two tables (§4.7): how one account's prices were
  denominated must not explain the next account's list.
- **The banner is the visible half, and it only fires on the genuine silent failure** — conversion
  asked for, no rates, *and* the drawn rows spanning more than one currency. Not on a user who
  turned conversion off: their prices are correct and saying so would be nagging. Unlike 3.6's
  `hasCachedRows` it is asked of the **filtered** rows, and for the opposite reason — narrowing to
  one currency removes the comparison the banner exists to warn about. It carries no retry, since
  nothing the app can send fixes it; the fix is a Fixer API key on the server, so the copy names it.
- **"Which currency is this price in" has no field, and 5.3 is where that bites twice.** The answer
  is the repository's `symbolFor` decision — `converts(currencyId)` ? the main currency : the row's
  own — and **neither** thing on the row reproduces it. `currencySymbol` is not a currency: the
  instance ships four dollars and three kroner, so equal symbols do not mean comparable amounts.
  `currencyId` is not the denomination: a working conversion puts every row in one unit while each
  keeps the id it was converted *from*. So anything asking "are these prices comparable?" asks
  `PriceConversion` **and** the ids — `!isActive && distinctBy(currencyId).size > 1` — and the
  banner's condition above turns out to be a strict subset of it, since conversion asked for without
  rates is one way of not being active. One computation feeds both.
- **A comparison the data doesn't support is withdrawn *and* explained** (5.3). The Price sort is
  disabled while the drawn rows span denominations, and the sheet says why underneath rather than
  leaving a greyed chip to be interpreted. Both halves are needed because the flag is about the rows
  on screen: a sort chosen while they were comparable stays selected when a widened filter brings a
  second currency back, and in that state the chip is inert and the order is already wrong — the
  disable cannot reach it, so the sentence has to.
- **A converted row names the currency it came from, and only that** (5.4). The original amount is
  unrecoverable — the server overwrites `price` — so the detail screen carries a *label* under the
  price, never a second number, and the list row carries nothing (no space, and the banner already
  covers the instance-wide case). It is the same `converts(currencyId)` the repository asked when it
  chose the symbol, so the label cannot contradict the amount above it: a row already in the main
  currency, an instance not converting and one that cannot convert all say nothing.
  Naming it needs the **currency table**, because `currency_id` is the only thing the row carries —
  hence `SubscriptionsRepository.observeCurrencies()`, the cache read that turns that id into a
  code. The **code** (`USD`), not the symbol, for the same reason the comparison above is not asked
  of `currencySymbol`; blank for an id the cached table no longer holds, on the same reasoning as
  the blank symbol it would have had.
- **What this leaves undone**: nothing about currency — 5.3 closed the price sort and 5.4 the
  missing source currency.

#### Editor UI patterns, from 7.6/7.7

- **A field that opens something other than the keyboard** (a dropdown, a `DatePickerDialog`) is a
  `readOnly = true` `OutlinedTextField` with a transparent `Box(Modifier.matchParentSize()
  .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { … })`
  layered over it in a parent `Box`. `readOnly` alone still routes taps into text-cursor placement
  rather than a click callback, so nothing opens without the overlay. `next_payment`/`start_date`
  use `DatePicker`/`DatePickerDialog`/`rememberDatePickerState` this way — both stable Material3,
  needing only the file's own `ExperimentalMaterial3Api` opt-in.
  `initialSelectedDateMillis`/`selectedDateMillis` are UTC epoch millis, so a `LocalDate` round
  trips through `kotlin.time.Instant` (`kotlinx.datetime.Instant` is the deprecated one in 0.8.0):
  `date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()` in,
  `Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date` out.
- **Read a `SavedStateHandle` key before anything starts writing it, not after.** A form that
  persists itself via `_uiState.onEach { savedStateHandle[KEY] = … }.launchIn(viewModelScope)` in
  `init` writes its *first* entry the moment that line runs — `viewModelScope`'s dispatcher is
  `Main.immediate`, so the launch's first collection is not deferred to a later loop turn.
  `SubscriptionEditorViewModel`'s `hadStoredForm` (does this key already have a value, to decide
  whether to pre-fill from the cache or leave a restored/mid-edit form alone) is captured as a
  property initializer *ahead* of `persistForm()` in `init`, or it always sees the value that same
  line just wrote and never pre-fills anything.
- **An offline preview provides the local itself**: `WallosMobilePreviewTheme` supplies
  `LocalIsOffline = false`, so a preview of an offline branch wraps its content in
  `CompositionLocalProvider(LocalIsOffline provides true) { … }` inside the theme rather than
  faking the effect any other way.

### 7.2 Explicitly out of v1

Everything here is real work that the walking skeleton does not need. Deferring it is what keeps
v1 small:

- **TOTP** — if `login.php` redirects to `totp.php`, show "this account needs a one-time code;
  use the API key instead" and point at the key field. That's the whole handling. (Landed: 3.9
  drives `totp.php` on the same session — see §1.1. The key field stays as the fallback it always
  was, not as the answer to a challenge.)
- **Room / offline cache** — fetch on screen open. No `NetworkMonitor`, no `LocalOfflineState`.
  (All three have landed since: `NetworkMonitor` + `LocalIsOffline` in 3.2, the database in 3.3,
  the offline-first repository in 3.4 — see the subsection above.)
- **Certificate trust prompt** — a plain HTTPS instance works. Self-signed certs fail with a clear
  error until this lands. (Landed: the trust manager and storage in 3.7, the prompt in 3.8 — see
  §4.5. Still true of a certificate that changes *after* onboarding, which no screen can accept.)
- **Extra drawer destinations** — the shell is fully wired (§5.4), but the drawer holds
  *Subscriptions* and *Settings* only. Dashboard and the *Manage* group arrive with their features.
  (Landed: Dashboard in 8.4 — it took the top drawer slot and `START_DESTINATION`, per §5.4's
  sketch. The *Manage* group is still Phase 5.)
- **`password_login_disabled` probing, login backoff, non-HTTPS warning** — Phase 2b hardening.
  (Landed: the warning in 3.1, the probe and `LoginThrottle` in 3.10 — see §1.1.)
- Writes, dashboard, catalog CRUD, settings, profile, notifications.

None of this existed in v1: `core:crud` and the `categories`/`paymentmethods`/`household` feature
modules landed in M7 (§3.4) — full CRUD, not just reads, ready for Phase 5's UI. (`core:serialization`
never became a real module at all — see §4.4.) Currencies stayed a single read inside
`feature:subscriptions` through M7, but that was never meant to be permanent: §3.4's own note says
a standalone `feature:currencies` module "can sit on `core:crud` the same way the other three do
when it lands," and Phase 5 is where it lands — `feature:subscriptions` keeps its trimmed
read-only `Currency` for the picker/list join, and the new module gets its own DTO/domain with
`rate`/`inUse` restored, a deliberate small duplication over a cross-feature reach.

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
Wallos under `/wallos`). TOTP redirect → message pointing at the key field, nothing more (3.9
replaced that message with the real second step).
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
*Done when:* the list renders offline after one online fetch, and a self-signed instance connects —
both verified end to end in 3.12 (cold `force-stop` start, fresh install against an nginx TLS
front). **Phase closed.** (The Kover-floor and instrumented-Compose-test questions 3.12 settled,
and the Coil/certificate defect it found, are covered in §3.5, §6.1 and §4.5 respectively —
not repeated here.)
*Decomposed as **M3** in `docs/CHECKLIST.md`* (12 steps, all ticked), chosen over Phase 3 because
three of M2's steps deferred cache debt to it and because Phase 3's writes need its `NetworkMonitor`.

### Phase 2c — Appearance and the deferred fixes
Not in the original phase list, and inserted here rather than appended because its steps are defects
in screens that already ship: the login screen rendered unthemed in dark mode (fixed in 4.1), there
was no theme preference (4.2 stores one; 4.3 is the screen that sets it), Settings has a single row,
and §4.5's Coil gap meant an accepted certificate didn't reach the logos (4.5). **Done when** a
dark-mode device shows no light-mode screen, the choice survives a restart, and a self-signed
instance renders logos — **all three met; the phase is closed.**
*Decomposed as **M4** in `docs/CHECKLIST.md`* (5 steps, all ticked). It runs before Phase 3 so the write screens
are built on a shell that draws correctly; nothing in it sends data, so it needs none of Phase 3's
groundwork.

The one structural fact worth stating here rather than in a step, settled by M4.1: **the `Surface`
belongs to `WallosMobileTheme`, not to `WallosMobilePreviewTheme`.** It used to be the other way
round, which is why the login screen — the only screen with no `Scaffold` above it, since everything
else sits under `AuthenticatedMainScreen` — showed the *window* background, which the manifest pinned
to a light theme; and why no `@PreviewWallosDarkLight` in the repo could catch a background or
content-colour bug, previews having rendered a themed surface the app never drew. The preview theme
now adds composition locals and nothing else, so a preview is evidence about colour.

Two things follow, and they are the reason this is in the plan rather than only in the step:

- **`androidApp` owns a `values/` + `values-night/` `themes.xml`** whose `windowBackground` tracks
  `SurfaceLight`/`SurfaceDark`. It is what the window paints before Compose does. Deliberately no
  `com.google.android.material` dependency: `Theme.Material3.DayNight.NoActionBar` would need one,
  `Theme.DeviceDefault.DayNight` is API 29 against a minSdk of 24, and a colour per configuration
  needs neither.
- **A colour role left out of `lightColorScheme`/`darkColorScheme` is not derived from `surface`** —
  it falls back to Material's baseline lavender. So the surface-container ladder, `outlineVariant`
  and the inverse roles are all set explicitly in `Color.kt`/`Theme.kt`. Adding a component means
  checking which token it reads (`Card` → `surfaceContainerHighest`, `AlertDialog` →
  `surfaceContainerHigh`, `ModalBottomSheet`/`ModalDrawerSheet` → `surfaceContainerLow`); only the
  `*Fixed` family is still on the baseline, since nothing here draws it.
- **The window is themed by *configuration*, the app by preference, and 4.2 lets them disagree.**
  `ThemeStorage` is collected in `WallosAppContent` and reduced to the `darkTheme` boolean
  `WallosMobileTheme` takes — one flow, no `MainViewModel` (Mealie's shape), seeded with
  `ThemeMode.default()` so it never gates the first composition (§5.5).
- **The system bars are the platform's, and 4.3 is what makes them follow the app.** Bare
  `enableEdgeToEdge()` picks its icon tint from the resource *configuration*, so a stored Light on a
  night-mode device leaves the status bar icons invisible. The fix is `SystemBarStyle.auto(…) {
  darkTheme }` for both bars, re-applied whenever the boolean changes — `enableEdgeToEdge` is
  designed to be called again. The boolean reaches `androidApp` as `WallosAppContent`'s
  `onDarkThemeChange` callback rather than being recomputed there: a second `ThemeStorage`
  collection above the shell would be §5.5's first-composition trap a second time, and this keeps
  one reader and one composable entry point.

### Phase 2d — The filed defects
Not in the original list: six defects a *verification* step saw on a device and filed under
`docs/CHECKLIST.md`'s "To review" rather than fixing in place — an error message that blames the
connection for an expired certificate (§4.5), filter and sort that don't survive a process death
while the back stack does (§5.5), a price sort that compares across currencies and a converted price
that doesn't say so (§7.1's currency-conversion notes), an invisible login backoff (§1.1), and a
failed logo that stays a letter after the server comes back (§4.5).
**Done when** what remains under "To review" is policy and unstarted phases rather than
known-wrong behaviour.
*Decomposed as **M5** in `docs/CHECKLIST.md`* (6 steps, all ticked).

### Phase 3 — Subscriptions, write + reference data
Add / edit / delete, including the multipart logo upload and `logo_url` fetch. `feature:categories`,
`paymentmethods`, `household` data+domain layers on `core:crud`, surfaced first as pickers inside
the subscription editor — currencies reuse `feature:subscriptions`'s existing read path rather than
a fourth module (§3.4, §10). Enforce: `ONE_TIME` unavailable, strict date format, `"1"`/`"0"`
encoding, re-read after write to confirm the logo landed.
*Decomposed as **M7** in `docs/CHECKLIST.md`* (9 steps, all ticked — **Phase 3 is done**); the
`core:crud` shape itself, and the two type-level corrections it needed past the original sketch,
are covered in §3.4 rather than repeated here.

### Phase 4 — Dashboard
`get_monthly_cost` and `get_period_budget` with version gating, upcoming payments derived locally
from `next_payment` + cycle. This is where use cases earn their place — the home screen composes
three endpoints plus cached subscriptions into one state.
*Decomposed as **M8** in `docs/CHECKLIST.md`* (4 steps, all ticked — **Phase 4 is done**) —
version gating turned out to be reactive (`WallosError.UnsupportedEndpoint` on a 404), not a
stored `version.php` value, since no minimum version is documented anywhere for
`get_period_budget`; see the milestone's own preamble, and §4.6 for where that leaves the plan's
original proactive-storage sketch. `UpcomingPaymentsCalculator` (8.2) rolls a stale `next_payment`
forward by mirroring the server's own `endpoints/cronjobs/updatenextpayment.php` cron rather than
just cycle + frequency in isolation: that cron only ever advances a row where `auto_renew = 1 AND
inactive = 0`, so a past-due row with auto-renew off (or `ONE_TIME`, which has no periodicity to
roll by) is excluded from the list rather than given a fabricated future date the server itself
never computes. 8.4 built the screen itself and made it the app's landing screen, ahead of
Subscriptions in the drawer (§5.4).

### Phase 4b — Dashboard: web parity

Not in the original list: comparing the shipped dashboard (8.4) against the actual Wallos web UI
(`/home/gregory/proj/other/Wallos`) rather than `WALLOS_API.md` alone found it doesn't show what
the web shows — no limit on upcoming payments where the web caps at 3, no "Overdue Renewals"
section, "Budget" collapsing two web widgets (Monthly Budget, which contains Monthly Cost, and a
Period Budget the web hides whenever the period equals the calendar month) into one always-visible
card, and no equivalent of the web's "Your Subscriptions"/"Your Savings" sections. The user does
not consider 8.4 done as shipped and put this ahead of Phase 5. **Done when** the dashboard's
sections match the web dashboard's, verified card-by-card against the live instance.
*Decomposed as **M10** in `docs/CHECKLIST.md`* (7 steps) — pulls a minimal `feature:profile`
(`getUser()` only) forward from Phase 5's M9, since nothing on the dashboard could reach
`user.budget` before this; M9's own `feature:profile` step later adds `setBudget()` to the same
module rather than building a second reader. See the milestone's own preamble for the full list of
what was checked against the live PHP source, including a real gap found in `UpcomingPaymentsCalculator`
(a future one-time subscription isn't excluded, unlike the web's own query).

### Phase 5 — Management screens
Full CRUD UI for the four catalog resources (with the in-use delete guard surfaced properly),
`feature:settings` (server display settings + local theme), `feature:profile`
(`get_user`, `set_budget` — and note that sending `period_budget` alone silently resets the period
type and anchor, so always send all three together).
*Decomposed as **M9** in `docs/CHECKLIST.md`* (9 steps) — scoped down from the paragraph above in
one real way: server-side display settings (`get_settings.php`/`set_settings.php`) turned out to
be an open ~12-field map governing mostly the Wallos *web* UI's own rendering, with nothing this
app has a concrete reason to edit yet, so M9 skips it and ships the four catalog resources plus
the budget editor only — see the milestone's own preamble for the rest of what got settled
(currencies as a new `feature:currencies` module, the payment-icon multipart upload, the budget
editor as a Settings sub-screen) and a `get_user.php` doc bug (`notes`) found and fixed while
scoping it.

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
| No API for notification writes, stats, clone, renew, or key regeneration — those are session-cookie endpoints only. | Stay on the API-key surface. Scraping the login form breaks under OIDC (TOTP is driven since 3.9). File upstream requests for the gaps. |
| The API key is a plaintext bearer credential. | Keystore-backed storage, HTTPS required, trust prompt instead of pinning. |
| **The login bridge scrapes HTML.** `id="apikey"` on `profile.php` is not an API contract; an upstream markup change breaks onboarding silently. | Validate the scraped key against `version.php` before storing, and keep Path B (manual entry) permanently in the UI as the recovery route. |
| **The login bridge handles the user's real password** — over cleartext on many self-hosted LAN instances. | Never persist it; exchange for the key and drop. Warn on non-HTTPS origins and steer to Path B there. |
| The server has **no login rate limiting or lockout**, so a retry loop is a brute-force tool aimed at the user's own instance. | Client-side backoff on failed login attempts — `LoginThrottle`, 3.10, announcing its wait since 5.5; see §1.1. |
| Password login may be disabled (`OIDC_DISABLE_PASSWORD_LOGIN`), and OIDC can't be bridged at all. | Probe the login form during setup and degrade to Path B — 3.10; see §1.1. |
| Whole-list fetches degrade with very large subscription sets. | Room cache + client-side paging; the payload is one row per subscription, so this is unlikely to bind in practice. |
| A route added without registering it in the nav3 polymorphic `SerializersModule` breaks back-stack restore — silently, and only on process death. | Test asserting the registered set covers every route reachable from the entry providers; "Don't keep activities" in the Phase 1 acceptance check. |

---

## 10. Open decisions

### Still open

Nothing at present — the two entries this section held are both settled below.

### Settled

- **DI mechanism** — `io.insert-koin.compiler.plugin`, never KSP for DI. Confirmed: both
  TaigaMobileNova and MealieMobile use it, with identical `KmpDiConventionPlugin`s. The
  `koin-ksp-compiler` entry in Mealie's catalog is unused. (KSP is still needed for Room.)
- **Android-only targets** — `configureKmp()` declares no targets at all; the Android one comes
  from the AGP KMP library plugin (§3.1). iOS and Desktop return in Phase 6.
- **nav3 placement** — `NavigationState`/`Navigator`/`toEntries()` live in `core:navigation`
  (§5.2), not in `composeApp` as MealieMobile has them, so `Navigator` stays unit-testable.
- **Shell** — `ModalNavigationDrawer`, not bottom navigation, matching both reference apps (§5.4).
- **Money representation** — `Double` + careful formatting, no external big-decimal library.
  Settled by 2.2: the client never does arithmetic beyond summation, and money formatting is fixed
  `1,234.56` with hand-rolled half-up rounding rather than device-locale — see the Domain modelling
  notes under §6.1.
- **Catalog module granularity** — three feature modules over `core:crud`
  (`categories`/`household`/`paymentmethods`), not the four §3.4 originally sketched and not a
  single `feature:catalog`. Settled when Phase 3 was decomposed into `docs/CHECKLIST.md`'s M7:
  currencies stays inside `feature:subscriptions`, which already built a full read path for it in
  2.3 and 3.11 — a fourth module would duplicate that for no caller. See §3.4's note.

### Superseded

- ~~Declaring `jvm()` immediately to keep `commonMain` honest~~ — replaced by the stricter rule
  that feature modules have no `androidMain` at all (§3.1), which catches the same leakage without
  the build cost.
