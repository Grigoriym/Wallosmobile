# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `11/11` · M2 `7/7` — **v1 done** · M3 `12/12` — **Phase 2b done** ·
M4 `1/5`
**Current step:** 4.2

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
- **Tick a step in place; move it out when its milestone closes.** A ticked step goes to
  `CHECKLIST-DONE.md` verbatim, in one move per milestone — not one per step, which would put a
  file rename in every commit. The Deviations log never moves.

## Ground rules (apply to every step)

**`CLAUDE.md` at the repo root holds the coding conventions** — KMP/DI/nav3 rules, Compose rules,
error handling, strings, and the think-before-coding / simplicity / surgical-changes guidelines.
It loads automatically; don't duplicate it here. Checklist-specific rules only:

- Do **exactly** the step. Don't pull work forward from a later step because it's convenient.
- **A step's prose is a sketch; `CLAUDE.md` is the spec.** Where they disagree, the convention
  wins and the step's `Note:` records the override — this has now happened four times (1.10 and
  2.3 on "fakes go in `:testing`", 3.1 on "seed from `ServerUrlStorage`", which would have been a
  `ui` → `core:storage` reach past this feature's own repository, and 3.3 on "entities mirror the
  domain model", which taken literally would have put a `feature:*:domain` dependency inside
  `core:storage`). The steps were written before
  the code existed; the rules were written from it.
- `./gradlew detekt ktlintCheck` must pass before a step is ticked.
- A step that adds logic adds its tests in the **same** step — hand-written fakes in `:testing`,
  no mocking library (plan §6.1).
- Read the reference projects rather than guessing:
  `/home/gregory/proj/grappim/TaigaMobileNova` (structure, build-logic, networking)
  `/home/gregory/proj/grappim/MealieMobile` (nav3, drawer, top bar, templates in its `CLAUDE.md`)

---

Completed steps live in [`CHECKLIST-DONE.md`](./CHECKLIST-DONE.md) — **all of M0, M1, M2 and M3**,
verbatim. This file carries what is still open, plus the Deviations log, which keeps growing.

Plan §8's **Phase 3** (subscription writes, reference-data pickers) is still next in the *phase*
order and still needs decomposing into steps the way Phase 2b became M3. M4 goes first deliberately:
its steps are defects in what already ships, and doing them first means the write screens get built
on a shell that renders correctly.

---

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

- [ ] **4.2 — core:storage: `ThemeMode` + `ThemeStorage`, honoured above the shell**
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

- [ ] **4.3 — feature:settings ui: the Interface screen**
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

- [ ] **4.4 — feature:settings ui: the About screen**
  Version and build info plus a link out to the project. `AppInfoProvider` (`core:appinfo-api`)
  currently exposes `isDebug()` alone, so it gains whatever the screen shows — the impl in
  `androidApp` is the only place `BuildConfig` exists (1.3's deviation is why the interface and its
  impl live apart). Link handling is `LocalUriHandler`, not an intent.
  *Verify:* `./gradlew :feature:settings:ui:testAndroidHostTest` with a fake `AppInfoProvider`, and
  on device: open About, check the version matches the installed build, tap the link.
  ·  *Ref:* `MealieMobile/feature/settings/ui/.../about/`
  Same route-registration rule as 4.3. Nothing here is user data, so there is nothing to redact and
  no reason for this screen to talk to the network.

- [ ] **4.5 — composeApp: an `ImageLoader` that trusts what the user trusted**
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

---

## Still open after v1

Written when M2 closed, and now the list that outlived M3 too — these are the pointers that would
otherwise have vanished with the steps that found them. **3.12 kept to that shape**: a verification
step files what it finds here rather than fixing it, and it found the first two entries below.

- **Logos never load on a self-signed instance** (3.12). The certificate the user accepts is pinned
  for the *HTTP client*; `AsyncImage` is Coil's own network stack and no `ImageLoader` is configured
  anywhere, so image loads keep failing after the trust prompt has been accepted. It is silent twice
  over: nothing reports the failure, and `SubscriptionLogo`'s initial-letter placeholder only fires
  on an **empty** filename, so a failed load draws a blank gap rather than the fallback that exists.
  The fix is a `SingletonImageLoader` built on the app's own `HttpClient` — which is `composeApp`'s
  DI root reaching into a `feature:*:ui` widget's business, so it is more than 3.7's title covered.
  Confirmed on the emulator against the nginx front, with `curl -k` proving the server serves the
  image.
- **The filter and sort don't survive process death, though the back stack does** (3.12). After
  `am kill` the app restores the detail screen it was on, and the list behind it has forgotten its
  filter — 3.6 put both in `MutableStateFlow`s, which outlive a rotation and not a process. The
  inconsistency is what makes it worth a line: the nav state is carefully saved and the criteria
  beside it are not. A `SavedStateHandle` would close it, and that is a change to 3.6's surface.
- **The pre-v1 no-backcompat bullet in `CLAUDE.md` expires at the first outside install** — see
  2.7's first deferred item. Nothing has changed yet: nobody but us has installed the app.
  **The user reaffirmed this on 2026-08-04 and owns the trigger**: ignore backward compatibility
  entirely until they say otherwise, and don't re-open the question per step. Destructive Room
  fallbacks and renamed DataStore keys are free until that word comes.
  **M3 raised the stakes and 3.11 raised them again**: there is now a Room schema in the picture, and
  it is already at version 2, so a released app needs real migrations rather than the destructive
  fallback the pre-v1 rule permits.
- **A Kover floor and a Compose UI test setup**, on the terms in 2.7's second deferred item —
  instrumented, not Robolectric, and grown one screen at a time. **3.12 revisited both and measured
  them**, which changed the question from *when* to *what*: the aggregate is 48.8% line, but 388 of
  the 2012 measured lines are Room's generated `*_Impl` classes at 0% (Kover cannot see the
  instrumented DAO suite) and the rest of the 0% is Composables, while the logic layers already sit
  at 82–100%. So a **whole-project floor is the wrong instrument** — it would gate on generated code;
  a floor scoped to the logic modules is the real one, and setting it edits `kover { }` and costs a
  `Gate-change:`. The Compose half is now dated rather than open-ended: M3's whole rendering surface
  is at 0%, so the first instrumented Compose test should cover the list screen's four derived states
  plus the two banners, and 3.3 has already paid the `androidDeviceTest` setup cost.
  **Decided on 2026-08-04: leave the coverage floor alone and stop revisiting it.** The user's reason
  is that the number will keep moving while features are still landing, and the tests that matter are
  written per step anyway — which is what the 82–100% on the logic layers already shows. This is not
  a "later" item any more; it needs a reason to come *back*, such as coverage on a logic module
  visibly falling.
- ~~The login screen doesn't prefill the server URL~~ — **done in 3.1**.
- ~~Plan §9's non-HTTPS warning~~ — **done in 3.1** (warn and steer to Path B, never disable).
  ~~1.9's `password_login_disabled` probe~~ — **done in 3.10**, off the URL field rather than off
  Connect.
- **The trust prompt only exists on the login screen** (3.8). A certificate that rotates *after*
  the app is connected leaves every screen on 3.5's stale banner with no way to accept the new one
  — Disconnect and log in again is the only route, and nothing says so. Confirmed on the emulator,
  not inferred. The fix is either a prompt wherever a refresh can fail, or copy on the banner that
  names the real cause; both are more than 3.8's title covers.
- **The backoff is invisible** (3.10). Past three refused attempts the next one waits 1–8 seconds
  under the spinner that was already there, so the screen says nothing about why the login got
  slow. Telling the user would mean a state that only exists *while* a call is in flight — either
  a wait the repository announces before it starts, or a countdown — which is more than the step's
  title covers. Nothing is wrong; it is just unexplained.
- **A converted price loses its original currency** (3.11). When the instance converts, the row is
  stored with the main currency's symbol and nothing on either screen says what it was before —
  Wallos' own web UI has a `show_original_price` setting for exactly this, which this app doesn't
  read. Cheap to add on the detail screen, and not what a step called "conversion hint" covers.
- **Sort by price still compares across currencies** (3.6, unchanged by 3.11). Whenever conversion
  is off or unavailable, `SubscriptionSort.PRICE` orders raw numbers, so €5 sorts below $10 as if
  they were the same unit. 3.11's banner *tells* the user their prices don't compare but the sort
  still offers the comparison; the honest fix is to disable or annotate that option when the drawn
  rows span more than one currency, which is a change to 3.6's surface rather than to 3.11's.
- **Version gating (plan §4.6) is still unowned.** It gates `get_period_budget`, `set_budget`'s
  period fields, `logo_variant` and `square_icons` — all Phase 4 and 5 surface, so M3 leaves it
  alone deliberately rather than by oversight.

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
| 1.8 | No snackbar host and nothing offline-aware in the shell (Mealie has both) | There is no `NetworkMonitor` (1.4) and no `LocalIsOffline`; errors go to UI state — *now in plan §5.4*; **the offline half is closed by 3.2**, the snackbar half still stands |
| 1.9 | `WebLoginApiImpl` **and** `SetupRepositoryImpl` are `@Factory`, not `@Single` | A `@Single` above the `@Factory` web client resolves it once and keeps the session for the life of the process — *now in plan §1.1* |
| 1.9 | The bridge clears the stored key before it starts | `withApiKey` overwrites a caller's `api_key` with the stored one, so a re-login would validate the stale key — *now in plan §1.1, §4.1* |
| 1.9 | `VersionDTO` created in `feature:setup:dto`, a module outside this step's title | `WallosApiClient.post<T>` needs a response type for the validation call; `version` is nullable so an older instance isn't `Malformed` |
| 1.10 | `getErrorMessage` and `ObserveAsEvents` created in `utils:ui`, which gains `core:domain` + `strings` | 1.6 parked `getErrorMessage` here, and the screen's one-off success event needs the collector; neither has a module of its own — *now in plan §5.4* |
| 1.10 | `SetupRepository.connectWithApiKey` returns `Result<Unit>`, not a `LoginOutcome` | Path B drives no web session, so there is no credential verdict to report — *now in plan §1.1* |
| 1.10 | `FakeSetupRepository` stayed private to its test instead of moving to `:testing` (1.9's note asked for the move) | One consumer, and `:testing` is on every module's test classpath — a `feature:setup:domain` dep there leaks the feature everywhere — *now in plan §6.1* |
| 1.10 | Password visibility is a Show/Hide `TextButton`, not an icon | `material-icons-core` has neither `Visibility` nor `VisibilityOff`, and one glyph doesn't justify `material-icons-extended` in every `ui` module — *now in plan §3.3* |
| 1.10 | `MainDispatcherRule` added to `:testing` | `viewModelScope` needs `Dispatchers.setMain`, and every ViewModel test from here on needs it — the genuine cross-module double — *now in plan §6.1* |
| 1.11 | Login is not a route: `LoginRoute` deleted, startup branch is `isConnected` collected above the shell | Plan §7.1 calls it "not a screen"; one source of truth means Disconnect (2.6) needs no navigation — *now in plan §5.3, §7.1* |
| 1.11 | The branch is seeded from `rememberSaveable`, not just from the flow | `rememberNavBackStack` only consumes restored state in the *first* composition, so a late-composed shell loses the back stack on process death — *now in plan §5.5* |
| 1.11 | `LoginViewModel.connectedEvent` / `onConnectSuccess` removed (1.10 added them) | The stored key already is the signal; a second owner of the same fact can only disagree with it — *now in plan §1.1* |
| 1.11 | Graph test uses `verify()` with `HttpClientEngine` in `extraTypes`, not `checkModules()` | `checkModules` instantiates (needs a DataStore file); `verify` reads `@Single fun` definitions through the bound type's constructor — *now in plan §6.1* |
| 1.11 | Manifest gains `usesCleartextTraffic="true"` | Plain-HTTP self-hosted instances are the normal case and are otherwise unreachable; §9's non-HTTPS warning is still unowned — *now in plan §9* |
| 1.11 | Verify lines point at the local instance, not `demo.wallosapp.com` | The public demo's `profile.php` throws a PHP fatal, so the login bridge finds no key to scrape — *now in `WALLOS_API.md` §8* |
| 2.1 | Subscription names arrive HTML-escaped; unset dates are `""` not `null` | Neither is in the schema or the PHP docblocks — read off the live instance — *now in `WALLOS_API.md` §3.1* |
| 2.1 | `BillingCycle.fromCode` is nullable, so `Subscription.cycle` is too | An unknown code means an instance newer than this build; a default would render a wrong cycle — *now in plan §6.1 "Domain modelling notes"* |
| 2.1 | Domain `Subscription`/`Currency` model only what §7.1's two screens render | The DTO keeps the full row; the domain model grows with the screen that needs a field |
| 2.2 | Cycle + frequency → "every 6 months" moved out of `utils:formatter:datetime` and into 2.4 | It is a function of `BillingCycle`, which lives in `feature:subscriptions:domain` — keeping it here would point `utils/` at `feature/` — *now in plan §2, §3.3* |
| 2.2 | Formatters are plain `@Single` classes, not interface + impl | The repo's interfaces are platform/IO seams; a fake formatter would only let a consumer's test assert wrong output — *now in plan §6.1* |
| 2.2 | Money formatting is fixed to `1,234.56`, not device-locale, and rounds half-up by hand | The instance itself renders `en_US` (API doc §3.5); `kotlin.math.round` ties to even — *now in plan §6, Domain modelling notes* |
| 2.3 | The currency join is a `currencySymbol` field on domain `Subscription`, not a map handed to the screen | 2.1 left the shape open; a field keeps the currency list out of every consumer, at the price of two round trips per repository call — *now in plan §7.1* |
| 2.3 | No `SubscriptionQuery` type, though plan §6.1's fake sketch has one | v1 sends `api_key` alone — filters are Phase 2b, and with neither filters nor `all-user-subscription` the §3.2 SQL bug is unreachable by construction — *now in plan §6.1* |
| 2.3 | Response envelope DTOs have no defaults, unlike the row DTOs | An empty user gets `"subscriptions":[]`, so an absent key is a broken response and `Malformed` beats a false empty state — *now in plan §4.2* |
| 2.3 | `HtmlUnescaper` is its own `@Single` class, and decodes `&amp;` last | The ordering is the whole trap: decode it first and `&amp;lt;` becomes a `<` the user never typed — *now in plan §6.1* |
| 2.3 | Fakes stayed private to their tests again, against the step's own "in `:testing`" wording | Same objection as 1.10 — `:testing` reaches every module's `commonTest`, so a feature-domain dependency there leaks the feature everywhere — *now in plan §6.1* |
| 1.5 | No dynamic colour, so no `expect`/`actual` `colorScheme()` and no `androidMain` in `uikit` | Mealie's only reason for the `expect` is `dynamicDarkColorScheme(LocalContext)`; a static palette seeded from the logo navy keeps the brand and the module common — *now in plan §3.3* |
| 2.4 | The display date format lives in `utils:formatter:datetime` after all, not in the composable | kotlinx-datetime's `MonthNames.ENGLISH_ABBREVIATED` needs no resource table, so it stays a pure host-tested function — *now in plan §6, Domain modelling notes* |
| 2.4 | `feature:subscriptions:ui` depends on `core:api` for `BaseUrlProvider` | A logo is a bare filename; the ViewModel is the first place that has both it and the normalized instance root — *now in plan §7.1* |
| 2.4 | A failed load clears the list rather than leaving a stale one under the error | With no cache there is nothing behind the error worth keeping, and a stale list under "couldn't reach the server" lies — revisit with Phase 2b's Room cache |
| 2.4 | `:strings` gained a `RPlurals` typealias beside `RString` | The cycle text is a plural, and `Res.plurals` had no alias — *now in plan §3.3* |
| 2.4 | `Routes.kt` renamed to `SettingsRoute.kt` when `SubscriptionsRoute` left it | detekt's `MatchingDeclarationName` fires on a one-declaration file; 2.6 deletes the file — *now in plan §5.2, §5.3* |
| 2.5 | A route parameter needs `@InjectedParam`, and `KoinGraphTest` cannot catch a missing one | `verify()` whitelists `String`/`Int`/`Long`/`Double` outright, so a primitive constructor parameter always passes — *now in plan §6.1* |
| 2.5 | The detail screen re-reads its row instead of taking one from the list | No cache means the alternative is a snapshot of unknown age; the price is two more round trips per open — *now in plan §7.1* |
| 2.5 | Logo, inactive badge and cycle text moved to a shared `ui/widgets/` package; the logo URL to `ui/LogoUrl.kt` | Second consumer, same module — the alternative was writing all four twice — *now in plan §7.1* |
| 2.6 | `feature:settings` is created as `ui` alone, and its ViewModel takes `core:storage` directly | Disconnect is one call on one seam; a `domain` layer over it would be an abstraction for a single use — *now in plan §2, §7.1* |
| 2.6 | 1.4's "re-login is one field" is not true: the login screen never prefills the kept server URL | `clear()` does keep the URL, but `LoginUiState` starts blank — the fix is in `feature:setup:ui`, so the Disconnect copy was reworded instead — *fixed in 3.1* |
| 3.1 | The prefill reads through `SetupRepository`, not `ServerUrlStorage` from the ViewModel | It would have been the third `ui` → `core` reach CLAUDE.md flags, and unlike `feature:settings` this feature already has a `data` layer to route through — *now in plan §1.1, §7.1* |
| 3.1 | `getStoredServerUrl()` returns `Result<String>` for a read that "cannot fail" | A DataStore read can throw and `viewModelScope` would crash on it; a bare `String` would also pull `core:domain` into `feature:setup:ui`'s `commonMain` for `resultOf` alone |
| 3.1 | Plan §9's cleartext warning is advisory and disappears on Path B | Every on-device `Verify:` line uses a plain-HTTP instance, so blocking Path A would make M3 untestable; and once the user is on Path B the warning has nothing left to steer — *now in plan §9* |
| 3.2 | `LocalIsOffline` needed a `.editorconfig` line, so a `NetworkMonitor` step carries a `Gate-change:` | `compose:compositionlocal-allowlist` fails the build on any new composition local; the allowlist is the rule's intended escape hatch, and this is the last entry M3 needs — *now in plan §5.4* |
| 3.2 | The shell injects `NetworkMonitor` itself instead of taking `isOnline: Boolean` from `WallosAppContent` as Mealie does | `WallosAppContent` also renders login, and connectivity has no reader there; keeping the collection inside the shell also keeps it below the startup branch, where §5.5's first-composition rule applies — *now in plan §5.4* |
| 3.2 | The step adds no host test | Both halves are unreachable from one — `ConnectivityManager` is the reason the seam exists, and the shell wiring needs an instrumented Compose test. The emulator flip is the whole verification; 3.4 is the first consumer that can assert anything |
| 3.3 | The DAO tests are **instrumented**, not host tests, and `core:storage` gained an `androidDeviceTest` compilation | On the Android target `Room`'s only builders take a `Context` and `BundledSQLiteDriver`'s native library ships in the aar's `jni/` — neither is reachable from `testAndroidHostTest`, and Robolectric is out — *now in plan §4.7, §8* |
| 3.3 | `allTests` and CI don't run the DAO suite | Device tests are not in the KMP `allTests` aggregate and CI has no emulator; the first suite in the project that isn't a CI gate — *now in plan §3.5, §8* |
| 3.3 | Entities are SQLite primitives with no `TypeConverter`, and `core:storage` depends on no feature module | Taiga's `core:storage` depends on a feature's `domain` for its entity types; here that would invert `feature/` → `core/`, so `cycleCode` stays an `Int?` and the entity↔domain mapper is 3.4's — *now in plan §4.7* |
| 3.4 | Cache eviction lives inside `ApiKeyStorage.clear()`, not in a cleaner the caller invokes | `clear()` has three callers — disconnect and both login paths (1.9) — and a second account must not inherit the first's rows; putting it anywhere else covers one of the three — *now in plan §4.7* |
| 3.4 | `SubscriptionsRepository` is `observe*`/`refresh*`, and `SubscriptionDao.getById` became `observeById` | Reads come off the cache and can't fail; the detail row is rewritten by a list refresh underneath the screen, which a one-shot read can't report — *now in plan §7.1, §4.7* |
| 3.4 | The repository split off a `SubscriptionsCache` | Two DAOs plus two entity mappers plus the API and the wire mappers is eight constructor parameters, over detekt's limit of 6 — and the DB half is a real seam — *now in plan §7.1* |
| 3.4 | The ViewModels are cache-first but the screens still draw the error *over* the data | 3.5 owns the rendering; 3.4 stops at the state being right, which is what its host-test verify can see |
| 3.6 | `SubscriptionsUiState` gained a `hasCachedRows` field, and 3.5's derived states ask *it* instead of `items` | `items` is now the filtered view, so `items.isEmpty()` no longer means an empty cache — offline, a filter matching nothing would turn 3.5's banner into a full-screen error over rows that exist — *now in plan §7.1* |
| 3.6 | Sorting by payer / category / payment method uses the resolved **name**, not §3.2's id | The ids never reach this app — 2.1 kept only the server-resolved names, which is also what the filter chips are built from — *now in plan §7.1* |
| 3.6 | Filter and sort are `MutableStateFlow`s combined with the DAO flow, not fields the ViewModel copies into state | One render path for "the filter changed" and "a refresh arrived", and re-sorting provably costs no refetch — *now in plan §7.1* |
| 3.6 | The filter action is a text button, not plan §5.4's `Icons.Default.FilterList` | The icon isn't in `material-icons-core` (1.10 and 2.5 hit the same wall); one word is cheaper than `material-icons-extended` in every `ui` module — *now in plan §3.3* |
| 3.7 | One portable exception carried as a `CertificateException`'s cause, instead of Taiga's platform subtype + portable twin + `expect`/`actual` unwrapper | JSSE constrains the *type* thrown, not the payload, so `findPendingCertTrust()` walking the cause chain replaces all three — and keeps `core:domain` free of `androidMain` — *now in plan §4.5* |
| 3.7 | `TrustedCertStorage` pins `(host, fingerprint)` strings in the shared DataStore, not Taiga's JSON `PendingCertTrust` list | That widening was for a revoke screen this app doesn't plan; the full certificate is still what the *prompt* shows, it just isn't what gets persisted — *now in plan §4.5* |
| 3.7 | A hostname that the certificate doesn't cover rethrows the original failure instead of getting its own exception type | Taiga needed a distinct type to pick a distinct message; here the only thing riding on it is whether TOFU is offered, and `error_unreachable` already covers the rest — *now in plan §4.5* |
| 3.7 | `core:api` gained `androidMain` **and** `androidHostTest`, the repo's first of either | The trust manager is `javax.net.ssl`, and `commonTest` cannot see an `androidMain` class; detekt's test exclusions don't cover `androidHostTest`, so its test names are camelCase like 3.3's — *now in plan §4.5, §6.1* |
| 3.8 | The retry is `onConnectClick()` re-read from state, not Taiga's captured `pendingRetry` lambda | Both paths are driven from one state and the dialog is modal, so nothing can have changed — a stored lambda would be a second copy of what the state already says |
| 3.8 | `pendingCertTrust != null` *is* the dialog; no `isCertTrustDialogVisible` beside it (Taiga has both) | Same rule 3.5 wrote down: a stored boolean is free to drift from the field that already carries the fact |
| 3.8 | Declining sets an error (`login_error_cert_not_trusted`); Taiga leaves the screen silent | Connect did nothing and the user is owed a reason — and `getErrorMessage` would say "check the URL and your connection", which is the one thing that was right |
| 3.8 | Trust is stored through `SetupRepository.trustCertificate`, not `TrustedCertStorage` from the ViewModel | 3.1's precedent exactly: this feature has a `data` layer, so a `ui` → `core:storage` reach would be going past it — *now in plan §4.5* |
| 3.9 | The `@Factory` chain was left exactly as it was; only its KDoc changed | A `@Factory` resolves once per *injection point*, and `LoginViewModel` is the chain's only one — the session already spanned the screen, which is the window a typed code needs — *now in plan §1.1* |
| 3.9 | `totp.php` has three outcomes, so `WebTotpOutcome` is its own enum rather than a reuse of `WebLoginOutcome` | A `302` to `login.php` is a lost session that no code can answer; folded into "bad code" it would loop the user forever — *now in `WALLOS_API.md` §9.2* |
| 3.9 | `submitTotpCode` takes no `serverUrl` and clears no key, unlike every other entry point | Both were done by the `loginWithPassword` that raised the challenge, on the session this runs against — a second `clear()` would drop the key the same call is about to store — *now in plan §1.1* |
| 3.9 | The code field is a plain text field, not `KeyboardType.Number` | Wallos accepts a backup code here and those are 20 hex characters — a number pad cannot type one — *now in `WALLOS_API.md` §9.2* |
| 3.10 | The probe runs off the URL field on a debounce, not from Connect | Connect is not earlier than the password, and being earlier than the password is the whole ask — *now in plan §1.1* |
| 3.10 | `PasswordLoginAvailability` has an `Unknown`, and the interpreter recognises the form before reading a missing input | It hides a path, so it may only fire on evidence: any 200 that isn't `login.php` also has no password input — *now in plan §1.1, `WALLOS_API.md` §9.5* |
| 3.10 | The probe is guarded on `isTotpRequired` | A GET of `login.php` clears `$_SESSION['totp_user_id']`, so probing mid-challenge kills it — the API doc had no row for this — *now in `WALLOS_API.md` §9.1* |
| 3.10 | `LoginThrottle` is constructed inside `SetupRepositoryImpl`, not injected | No dependencies, and the lifetime it wants is already that object's; injecting it would also be a seventh constructor parameter against detekt's limit — *now in plan §1.1* |
| 3.10 | The backoff is spent under the existing spinner and says nothing | A visible wait needs state that only exists while a call is in flight; parked in "Still open after v1" rather than widened into this step |
| 3.10 | `SetupRepositoryImplTest.repository()` became a `TestScope` extension taking `UnconfinedTestDispatcher(testScheduler)` | A dispatcher built with its own scheduler leaves a `delay` inside `withContext` invisible to `currentTime` — *now in `CLAUDE.md`* |
| 3.9 | Verified against a throwaway container, not the live instance the step assumed | 2FA on the user's own account is a live-data mutation with a lockout tail; a scratch `bellamy/wallos` on :8283 with a known secret costs one `docker run` and risks nothing |
| 3.11 | Conversion is detected from `get_settings.php` + `main_currency` + the rate table, not from `currency_id` vs `main_currency` as `WALLOS_API.md` §5.5 said | A converted row keeps the source `currency_id`, so that comparison says conversion was *attempted*, never that it happened — measured on a scratch instance — *now in `WALLOS_API.md` §5.5, plan §7.1* |
| 3.11 | The flag is sent only when the instance's own `convert_currency` setting asks for it, which needed a fourth endpoint | Overriding it shows a user who asked for real currencies something else; the read degrades to "off" on failure so a missing endpoint costs the conversion, not the list — *now in plan §7.1* |
| 3.11 | `PriceConversion` is cached in a **one-row Room table**, so `WallosDB` is version 2 | A response can't be read back for it and a cold offline start has no response at all; it also keeps the detail refresh at one round trip, since that must send the same flag as the list — *now in plan §7.1, §4.7* |
| 3.11 | `hasRates` is read off the rate table, not off `last_exchange_update` | No endpoint exposes the row the server actually gates on; every rate is exactly `1` until the first update writes both, and where the proxy is wrong `price / 1` makes it harmless — *now in plan §7.1* |
| 3.11 | `isConversionUnavailable` is a stored field asked of the **filtered** rows, the opposite of 3.6's `hasCachedRows` | Narrowing to one currency removes the comparison the banner warns about; `items` carries `price` as formatted text, so nothing derivable can count currencies — *now in plan §7.1* |
| 3.11 | The banner fires only on the silent failure, not on conversion being switched off | Prices in their own currencies with their own symbols are correct; a banner over a deliberate setting is nagging — the sort-by-price half is parked in "Still open after v1" |
| 3.12 | §4.5 already predicted Coil bypasses the pin; what was wrong is its "nothing in M3 needs it" | 3.8 and 3.12 both verify over the TLS front, so the gap is *in* the milestone that dismissed it — and a failed load draws a blank gap, since the placeholder branches on an empty filename — *now in plan §4.5* |
| 3.12 | No Kover floor was set, though the step reconsidered one | 19% of the measured lines are Room's generated `*_Impl` classes at 0% and unreachable from a host test, so an aggregate floor would gate on codegen; the logic modules are 82–100% already — *now in plan §3.5* |
| 2.7 | Agent guardrails are their own workflow, and the two rule documents are gated by *structure*, not by any edit | `ci.yml`'s `paths-ignore` means a docs-only commit gets no run, so the check can't live there; and gating any edit to `CLAUDE.md`/`CHECKLIST.md` fires on every reflow — counting Non-negotiables and steps instead was clean over all 35 commits of history — *now in plan §3.6* |
| 4.1 | The `Surface` went into `WallosMobileTheme` and *out* of `WallosMobilePreviewTheme`, which the step didn't ask for | Leaving both would double-`Surface` every preview and keep previews rendering something the app doesn't; moving it is what makes a preview evidence about colour |
| 4.1 | `Card` reads `surfaceContainerHighest`, not `surfaceContainerLow` as the step said | `FilledCardTokens.ContainerColor` is `SurfaceContainerHighest`; `surfaceContainerLow` is the drawer's and the bottom sheet's. The whole ladder is filled in, so the step's conclusion held |
| 4.1 | The `*Fixed` colour roles are left on Material's baseline | Only expressive components read them and none are used here; every role anything in this app draws is now a Wallos colour |
