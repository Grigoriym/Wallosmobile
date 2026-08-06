# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `11/11` · M2 `7/7` — **v1 done** · M3 `12/12` — **Phase 2b done** ·
M4 `5/5` — **Phase 2c done** · M5 `6/6` — **M5 done** · M6 `2/2` — **M6 done**
**Current step:** none — M6 closed; Phase 3 still needs decomposing into steps

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
- **A step that says it already checked an API still gets checked.** 4.5's text carried "the API
  confirmed against the artifact on disk … no need to read Coil's sources", and both API claims in
  it were wrong — the factory's Kotlin name (`KtorNetworkFetcher.factory` is a `@JvmName`) and
  `AsyncImage`'s `error` slot, which takes a `Painter` and cannot hold the fallback the step wanted.
  A step's confirmation is the previous session's reading, exactly like a `Ref:` into
  `WALLOS_API.md`; the `unzip`-the-sources-jar read that settles it takes seconds.
- `./gradlew detekt ktlintCheck` must pass before a step is ticked.
- A step that adds logic adds its tests in the **same** step — hand-written fakes in `:testing`,
  no mocking library (plan §6.1).
- Read the reference projects rather than guessing:
  `/home/gregory/proj/grappim/TaigaMobileNova` (structure, build-logic, networking)
  `/home/gregory/proj/grappim/MealieMobile` (nav3, drawer, top bar, templates in its `CLAUDE.md`)

---

Completed steps live in [`CHECKLIST-DONE.md`](./CHECKLIST-DONE.md) — **all of M0 through M5**,
verbatim. This file carries what is still open, plus the Deviations log, which keeps growing.

Plan §8's **Phase 3** (subscription writes, reference-data pickers) is still next in the *phase*
order and still needs decomposing into steps the way Phase 2b became M3. **M5 is done**: the list
"Still open after v1" filed six defects and all six are now closed, so what remains there is policy
and deferred *features* rather than known-wrong behaviour. **M6 is two housekeeping steps** — the
launcher icon and the store flavours — that belong to no phase and are not urgent, but which every
feature step will keep displacing until they are written down as steps.

---

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

---

## Still open after v1

Written when M2 closed, and now the list that outlived M3 and M4 — these are the pointers that would
otherwise have vanished with the steps that found them. **3.12 kept to that shape**: a verification
step files what it finds here rather than fixing it.

**Six entries left this list to become M5, and that is the loop working**: an entry is filed here
by the step that trips over it, and leaves when a step owns it. 4.5 closed the first of them (3.12's
logo defect) and filed one of its own on the way out. What is left below is deliberately *not* M5
material — two standing decisions the user owns, and one piece of surface that belongs to a phase
nobody has started. **Don't re-open the first two per step; they have both been settled twice.**

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
- ~~The trust prompt only exists on the login screen~~ (3.8) — the copy half is **done in 5.1**:
  a rotated certificate now names itself in the stale banner and points at Disconnect.
  A prompt *wherever* a refresh can fail is still unowned, and is a bigger change than naming the
  cause: it would put a pin write outside `SetupRepository`.
- ~~The backoff is invisible~~ (3.10) — **done in 5.5**: a held-back attempt names its wait under
  the spinner, announced by the repository as the wait starts rather than inferred by the screen.
- ~~A converted price loses its original currency~~ (3.11) — **done in 5.4**: the detail screen
  names the currency the amount was converted from. The amount itself stays unrecoverable, which is
  the server's doing, not a deferral.
- ~~Sort by price still compares across currencies~~ (3.6) — **done in 5.3**: the option is taken
  off while the drawn rows are in more than one denomination, and says why.
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
| 4.2 | The status-bar icon tint does not follow the app theme, and the fix is 4.3's | `enableEdgeToEdge()`'s `SystemBarStyle.auto` reads the resource configuration, not the Compose theme, so it only misbehaves once the two can diverge — and nothing can diverge them until the Interface screen exists |
| 4.3 | The `darkTheme` boolean leaves `composeApp` as an `onDarkThemeChange` callback, not as a value `MainActivity` computes | Computing it in the activity means a second `ThemeStorage` collection above the shell, which is 1.11's first-composition trap twice; the callback keeps one reader and one `WallosAppContent` entry point |
| 4.4 | `AppInfoProvider` gained `versionName()`/`versionCode()` — raw fields, not Mealie's rendered `getAppInfo(): String` | The version line and the Debug/Release word are string resources, which the `androidApp` impl cannot reach; raw fields also keep the only unconstructable class free of presentation, so a fake drives the whole screen — *now in plan §7.1* |
| 4.4 | `SettingsScreen` takes `viewModel` **last**, unlike 4.1–4.3's screens | `compose:parameter-order` exempts a single trailing function from following the defaulted params, so 4.3's `viewModel`-first shape only passed while there was one callback; a second one fails detekt and the fix is the order the subscriptions screens already use |
| 4.5 | `composeApp` exposes `core:storage` as **`api`**, the only `api` project dependency it has | The Koin compiler plugin re-checks `AppModule`'s own definitions at the `startKoin` call site in `:androidApp`, which reaches `core:storage` only through an `implementation` dependency — so `provideImageLoader`'s `TrustedCertStorage` and the definition binding it both have to be visible from there. The `includes` are never re-checked, which is why `NetworkModule` takes the same type freely |
| 4.5 | The loader is a `@Single fun` in `AppModule`, not the `@Factory class ImageLoaderProvider` TaigaMobileNova uses | Same plugin check: a scanned class in `composeApp` fails identically, so the class bought nothing. `NetworkModule` builds its clients in the module class too |
| 4.5 | `SubcomposeAsyncImage`, not `AsyncImage` with an `error` slot as the step said | `AsyncImage`'s `error` is a `Painter`; the fallback is an initial-letter `Surface` that has to be laid out, so the composable slot is the only one that takes it |
| 4.5 | A new `coil-singleton` version-catalog alias (`io.coil-kt.coil3:coil`) | `SingletonImageLoader` is not in `coil-core`; `:androidApp` implements its `Factory` and reached the type only transitively through `coil-compose` — *Gate-change on the commit* |
| 5.2 | `SavedStateHandle` is injected as an `@InjectedParam`, the repo's second use of that annotation | Koin resolves it from the `CreationExtras` through `AndroidParametersHolder`, not from the graph; unlike 2.5's `Int`, `verify()` **does** catch a missing annotation here, since `SavedStateHandle` isn't one of its whitelisted primitives |
| 5.2 | The criteria are stored as one JSON string, not through `androidx.savedstate`'s `encodeToSavedState` | On Android `SavedState` *is* `Bundle`, so the idiomatic encoder can't run in a host test and `SubscriptionFilter`'s `ImmutableSet`s have no serializer either — a String key is testable in both directions |
| 5.3 | "Spans more than one denomination" is asked of `PriceConversion` plus `currencyId`, not of the `currencySymbol` the step named | A symbol is not a currency (four dollars share `$`) and a working conversion leaves every row in one unit with its source `currency_id` intact — so each field alone gets one of the two cases wrong; reproducing `symbolFor`'s choice makes 3.11's banner a strict subset of the same computation — *now in plan §7.1* |
| 5.3 | The Price option is disabled **and** annotated, where the step offered either | Disabling alone is silent about the order the user is already in: a sort chosen while the rows were comparable survives a widened filter, and the chip is then selected, greyed and still wrong — *now in plan §7.1* |
| 5.4 | The label is derived in the ViewModel from a third cached flow (`observeCurrencies`), not stored on the row | The entity holds SQLite primitives and a "converted from" column would be derived data at schema version 3; the conversion state and the currency table are cached by the same refresh, so combining them keeps one render path — *now in plan §7.1* |
| 5.4 | The source currency is named by its **code**, not its symbol or name | Same objection 5.3 raised to `currencySymbol`: five currencies here share `$`. `code` falls back to `name` only because the DTO defaults it — *now in plan §7.1* |
| 5.2 | `NavDisplay`'s default `entryDecorators` is `rememberSaveableStateHolderNavEntryDecorator()` alone — no ViewModel-store decorator — and that says **nothing** about ViewModel lifetime | It reads as "every ViewModel is activity-scoped, so a second detail route reuses the first's", which is a phantom defect: measured on device, the list ViewModel survives a detail round trip while each detail route builds its own (`Refreshing subscription 4`, then `26`). Measure this, never infer it from the decorator list |
| 5.5 | The wait reaches the caller as an `onThrottleWait: (Duration) -> Unit` parameter on the two throttled repository methods, defaulted to `{}` | The state exists only for the duration of one call, so a flow beside the repository would have needed a lifetime, an initial value and a clear that a parameter doesn't; the default keeps every existing call site and the unthrottled surface untouched — *now in plan §1.1* |
| 5.6 | The fix is a `logoRefreshToken: Int` set as a Coil `memoryCacheKeyExtra`, not a change to `logoUrl` itself | `AsyncImageModelEqualityDelegate.Default` compares an `ImageRequest`'s `data` and cache keys, never the model's own `equals`; changing `data` would also change the actual fetch URL, where an extra changes only the request's identity — *now in plan §4.5* |
| 6.1 | `drawable/ic_launcher_background.xml` is a single fully-transparent `<path>`, not Taiga's/Mealie's opaque `ic_launcher_background_tint` colour | The user's artwork is a self-contained "sticker" — the foreground already bakes in its own navy disc on a transparent canvas — unlike those two references' bare glyphs, which need a background fill for contrast. Leaving the scaffold's opaque green grid behind it showed as a dark ring in the adaptive mask's safe-zone margin; a fully-transparent background removed it. **Android Studio's Image Asset Studio reimport reverts this file to the scaffold on every run** — it happened mid-step — so re-check it after any future logo regeneration, including 6.2's per-flavour debug icon if that step touches the same wizard |
