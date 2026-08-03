# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `11/11` · M2 `7/7` — **v1 done** · M3 `11/12`
**Current step:** 3.12

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

Completed steps live in [`CHECKLIST-DONE.md`](./CHECKLIST-DONE.md) — M0, M1, M2 and 3.1–3.7,
verbatim. This file carries what is still open, plus the Deviations log, which keeps growing.

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

- [ ] **3.12 — Phase 2b acceptance**
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

---

## Still open after v1

The tick above closes M2, so these are the pointers that would otherwise vanish with it.

- **The pre-v1 no-backcompat bullet in `CLAUDE.md` expires at the first outside install** — see
  2.7's first deferred item. Nothing has changed yet: nobody but us has installed the app.
  **M3 raises the stakes**: once 3.3 lands there is a Room schema in the picture too, and a
  released app needs a real migration for it rather than a destructive fallback.
- **A Kover floor and a Compose UI test setup**, on the terms in 2.7's second deferred item —
  instrumented, not Robolectric, and grown one screen at a time. 3.12 revisits both.
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
| 2.7 | Agent guardrails are their own workflow, and the two rule documents are gated by *structure*, not by any edit | `ci.yml`'s `paths-ignore` means a docs-only commit gets no run, so the check can't live there; and gating any edit to `CLAUDE.md`/`CHECKLIST.md` fires on every reflow — counting Non-negotiables and steps instead was clean over all 35 commits of history — *now in plan §3.6* |
