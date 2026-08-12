# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `11/11` · M2 `7/7` — **v1 done** · M3 `12/12` — **Phase 2b done** ·
M4 `5/5` — **Phase 2c done** · M5 `6/6` — **M5 done** · M6 `2/2` — **M6 done** · M7 `9/9` ·
M8 `4/4` — **M8 done** · M9 `9/9` — **M9 done** · M10 `9/9` — **M10 done** · M11 `1/1` —
**M11 done** · M12 `3/3` — **M12 done** · M13 `2/2` — **M13 done** · M14 `2/2` — **M14 done** ·
M15 `4/4` — **M15 done** · M16 `5/5` — **M16 done** · M17 `8/8` — **M17 done** · M18 `2/2` —
**M18 done** · M19 `1/2`
**Current step:** M19, step 19.2. 19.1 closed 2026-08-12: `feature:subscriptions:ui`'s
`androidDeviceTest` source set is wired for Compose UI tests (same `withDeviceTestBuilder
{ sourceSetTreeName = null }` shape as 3.3's Room DAO suite), one test passing on-device against
`SubscriptionsContent` (now `internal`) rendering the empty state — see 19.1's own `Note:` for the
two artifact-resolution gotchas (the correct `ui-test-junit4`/`ui-test-manifest` coordinates, and a
transitively-pulled `espresso-core:3.5.0` crashing on this AVD's API 36 until forced to 3.7.0). Next:
19.2, the real matrix — all four derived states plus both banners. 18.2 closed 2026-08-11: the Settings →
Trusted certificates screen shipped, verified end-to-end on-device against a throwaway TLS front —
see 18.2's own `Note:` for the full on-device trace (accept a real TOFU prompt, revoke it from the
new screen, confirm the same host re-prompts on the next connection). `docs/revisit.md` #1 (filed
during 17.3) is now deleted — the gap it filed is closed. 18.1 closed 2026-08-11: `TrustedCertStorage`
now stores the full `PendingCertTrust` JSON-encoded (was a bare `host|fingerprint` string) and
gained `getAllFlow`/`untrust`, matching TaigaMobileNova's own shape — see 18.1's own `Note:` for the
dependency-edge fallout (`core:storage` → `core:domain`, `:testing` → `core:domain`). M18 decomposed
2026-08-11 straight from `docs/revisit.md` #1: `TrustedCertStorage` had no way to list or revoke an
accepted TOFU pin short of clearing all app data. Picked over the Compose UI test setup M17 left
queued in "To review" because the user asked for this one directly; that stays next. 17.8 (Resilience) closed 2026-08-11: confirmed N/A,
even faster than Taiga's own task 7 — no OAuth flow anywhere in the app and no
`client_secret`/`CLIENT_SECRET`/`client_id` in source, `build-logic`, or the version catalogue.
`docs/security/masvs.md`'s header rewritten to state all eight MASVS categories were addressed
(seven reviewed in full, RESILIENCE excluded with its reasoning inline — no vendor asset for the
device owner to be the adversary of, since the user self-hosts the server this client talks to). No
code changed. **M17 is done** — all eight MASVS v2 categories reviewed, register created and
complete, no Open findings anywhere in it; one real gap (TOFU pin revocation) filed as
`docs/revisit.md` #1 rather than fixed inline. 17.7 (Privacy) closed 2026-08-11: all four MASVS-PRIVACY
controls Accepted, no Open findings. Both declared permissions (`INTERNET`,
`ACCESS_NETWORK_STATE`) trace to real call sites, no analytics/ad-ID dependency anywhere in the
catalogue. Crash-reporting disclosure confirmed structurally real (fdroid's `CrashReporterImpl`
is a total no-op and the settings toggle is absent, not disabled, on that flavor; both
`PRIVACY_POLICY*.md` docs mirror the split). `ApiKeyStorage.clear()` deletes all three Room tables
the app has before removing the key, confirmed at all three call sites (disconnect, both login
paths) — no account's cache survives into the next login. No code changed. 17.6 (Code quality)
closed 2026-08-11: all four MASVS-CODE
controls Accepted, no Open findings. `minSdk = 24` ported from TaigaMobileNova's own catalogue at
this project's first commit, no independent rationale documented (same absence Taiga's own row
recorded). `renovate.json`'s `osvVulnerabilityAlerts` still set (since M14); GitHub's native
Dependabot alerts confirmed OFF (`vulnerability-alerts` → 404), an optional separate lever, not
required. `WallosEnvelopeParser` is the app's only JSON config, already tolerant
(`ignoreUnknownKeys`/`isLenient`). Both `LocalUriHandler.openUri()` call sites (`AboutScreen.kt`)
resolve to fixed `RString` resources, never user/server-supplied text — doesn't reproduce Taiga's
own scheme-allowlist finding since nothing here feeds untrusted text into `openUri` today. No code
changed. 17.5 (Platform) closed 2026-08-11: IPC surface is `MainActivity`
alone (only component in either manifest, plain `MAIN`/`LAUNCHER`, no deep link); password field
has a working Show/Hide toggle, hidden by default. `FLAG_SECURE` decided with the user directly
rather than assumed: they don't want it — same reasoning as Taiga's maintainer, since it would
block screenshots app-wide on a single-`Activity` app they use themselves, not just on login —
recorded Accepted. No code changed. 17.4 (Authentication) closed 2026-08-11: login bridge reviewed
clean — password lives only in an in-memory `MutableStateFlow`, never persisted or logged, sent
once as a form-body param over the same `RedactingLogger`-covered engine; scrape target confirmed
always the user's own configured host (`BaseUrlProviderImpl` reads the same `ServerUrlStorage`
value `SetupRepositoryImpl` saves before any web call); no `WebView` anywhere, so RFC 8252 doesn't
apply; AUTH-2/3 re-confirmed N/A (no biometric anywhere). One design tradeoff recorded Accepted:
`LoginThrottle`'s backoff is client-side only, since Wallos itself enforces no lockout. No code
changed. 17.3 (Network) closed 2026-08-11: `CompositeTrustManager`
reviewed against all three `kmp-checks.md` TOFU questions — falls through to the platform default,
requires a hostname match before ever offering trust, pins per-`(host, fingerprint)` not per-host,
still checks expiry on a pin hit, only activates from an explicit user Confirm tap — all backed by
existing `CompositeTrustManagerTest` cases, no gap. `usesCleartextTraffic="true"` recorded as
Accepted: the API key does cross an `http://` instance in the clear (form-body, never a URL param),
bounded by `RedactingLogger` and the existing login-screen cleartext warning. One real, non-security
gap found: no in-app way to revoke an accepted TOFU pin — filed `docs/revisit.md` #1 (new file), not
fixed (needs a new storage method + UI screen, not small/isolated). No code changed. 17.2
(Cryptography) closed 2026-08-11: `KeystoreSecretCipher`'s
`KeyGenParameterSpec` reviewed clean — AES/GCM, no padding, 128-bit tag, IV reuse not just
defaulted-away but platform-*enforced* against (no `setRandomizedEncryptionRequired(false)`, no
IV ever supplied by the code), key never exported, no key/secret literal anywhere in source/
build-logic/version catalogue. One real gap needing a device, not a source read — key size isn't
pinned via `.setKeySize()` — added to the Needs-a-device table. No code changed. 17.1 (Storage)
closed 2026-08-11: `docs/security/masvs.md`
created, both leads from the milestone preamble resolved as **Accepted deviations**, not Open
findings — the `allowBackup`/no-extraction-rules gap is bounded by the cipher already in place
(ciphertext-only file, Keystore key doesn't travel with a backup), and `ServerUrlStorageImpl` holds
only a bare URL. No code changed. M16 done; the "TaigaMobileNova recently did a security review"
"To review" entry (filed 2026-08-10) was investigated 2026-08-11 and decomposed into M17 — see the
milestone's own preamble below for what that investigation found (short version: Taiga's MASVS
register mechanics apply directly and WallosMobile starts from a better position on Storage/Crypto/
Network than Taiga did; Taiga's *testing* overhaul doesn't transfer as-is — no `jvm()` target here
for its `jvmTest`-based Compose UI test technique to attach to — so a testing-setup milestone is
next after M17, not folded into it).
Previous **Current step** note on 16.5 moved to `archive/CHECKLIST-DONE.md` with the rest of M16
(M16 shipped `gplay`-only crash reporting and a Play In-App Update prompt; see the archive for all
five steps' full detail).

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
  `archive/CHECKLIST-DONE.md` verbatim, in one move per milestone — not one per step, which would
  put a file rename in every commit. **The move rides in the same commit as the closing step
  itself** (confirmed from git history — M8's archival is inside 8.4's own commit, not a follow-up
  one), so the session that ticks the last box also does the move, in the same pass.

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

Completed steps live in [`archive/CHECKLIST-DONE.md`](./archive/CHECKLIST-DONE.md) — **all of M0
through M8, and now M10, M9, M11, M12, M13, M14, M15, M16, M17 and M18**, verbatim. M10 was archived once before too (2026-08-08, its first seven
steps) and pulled back out the same day once two more real gaps turned up (10.8/10.9, below); once
those two closed it, it was archived again for good, same day. On 2026-08-06 the per-step
Deviations log that used to sit at the bottom of this file moved to
[`archive/DEVIATIONS.md`](./archive/DEVIATIONS.md), **frozen** rather than carried forward: almost
every row already had a permanent home in `IMPLEMENTATION_PLAN.md` (`now in plan §X`), so the
practice of appending one was retired as pure duplication — the fold into the plan is what future
sessions actually read. A step's own `Note:` is still where a deviation gets written down, same as
always.

**M5 is done**: the list below filed six defects and all six are now closed, so what remains is
policy and deferred *features* rather than known-wrong behaviour. **M6 is done** too — the
launcher icon and the store flavours, the two things only visible from outside the code. **M7 is
done** too — plan §8's Phase 3 (subscription writes, reference-data pickers), decomposed the way
Phase 2b became M3; see `archive/CHECKLIST-DONE.md` for its nine steps. **M8 is done** too — plan
§8's Phase 4 (Dashboard: monthly cost, period budget, upcoming payments), the app's first use case
and its new landing screen; see `archive/CHECKLIST-DONE.md` for its four steps. **M10 is done**
too — inserted out of phase order the same way M5 was, to close a defect the app's own use
uncovered: comparing 8.4's dashboard against the real Wallos web UI found it didn't show what the
web shows, and later that the numbers it did show didn't match either; see
`archive/CHECKLIST-DONE.md` for its nine steps. **M9 is done** too — plan §8's Phase 5 (Management
screens: add/edit/delete for categories, household, payment methods and currencies, plus the
budget editor), deliberately parked until M10 had nothing left in it, decided with the user
2026-08-08 so dashboard work didn't hand off to M9 piecemeal one gap at a time; see
`archive/CHECKLIST-DONE.md` for its nine steps. **M11 is done** too — its one step, decomposed
straight from "To review" rather than from a plan phase, showing the connected server on the
Settings screen; see `archive/CHECKLIST-DONE.md`. **M12 is done** too — its three steps, also
decomposed straight from "To review", replacing the hard-coded start destination with a
user-configurable one; see `archive/CHECKLIST-DONE.md`. **M13 is done** too — its two steps
shipped the `:benchmark` module and extended its generator to all three cold-JIT'd journeys; see
`archive/CHECKLIST-DONE.md` for the honest measurement result (JIT lock contention eliminated,
aggregate frame-jank on this AVD not improved). **M14 is done** too — its two steps, filed
directly by the user rather than decomposed from "To review", wired Codacy, Renovate and Codecov
the way `TaigaMobileNova` already has them; see `archive/CHECKLIST-DONE.md`. **M15 is done** too —
its four steps, filed directly by the user the same session the repo went public, ported Taiga's
branch model and release automation: `dev` as the default branch, `release-prepare`/
`release-finalize` for the version-bump and tag mechanics, `signingConfigs` for release signing,
and `release.yml` to build and publish the signed artifacts; see `archive/CHECKLIST-DONE.md` for
its four steps. **M16 is done** too — its five steps ported Taiga's Firebase Crashlytics (with a
user-facing opt-out) and Play In-App Update to the `gplay` flavor only, structurally absent from
`fdroid`; see `archive/CHECKLIST-DONE.md` for its five steps, including 16.5's Compose snackbar
shell surface (`SnackbarHostController`, mirroring `TopBarController`) that Taiga itself never had.
**M17 is done** too — its eight steps ran the `masvs-review` skill once per MASVS v2 category,
creating `docs/security/masvs.md`; every category came back Accepted/N/A with no Open findings, one
real gap (no in-app way to revoke an accepted TOFU pin) filed as `docs/revisit.md` #1 rather than
fixed inline; see `archive/CHECKLIST-DONE.md` for its eight steps. **M18 is done** too — its two
steps closed that gap: `TrustedCertStorage` gained `untrust`/`getAllFlow` and a Settings → Trusted
certificates screen shipped, verified end-to-end on-device against a throwaway TLS front (accept a
real TOFU prompt, revoke it, confirm the next connection re-prompts); `docs/revisit.md` #1 is
deleted. See `archive/CHECKLIST-DONE.md` for both steps.

---

## M19 — Instrumented Compose UI tests, starting with the subscriptions list screen (not in plan §8's phase order)

Decomposed 2026-08-12 from the "To review" backlog entry 2.7 first parked and 3.12 dated: *"A Kover
floor and a Compose UI test setup"*. The Kover-floor half was decided 2026-08-04 (leave the
aggregate alone — stays settled, not reopened here). This milestone decomposes only the Compose
half, which 3.12 already gave a shape: the first instrumented Compose test should cover the
subscriptions list screen's four derived states (`isStale`, `isFailed`, `isEmpty`, `isNoMatch`)
plus its two banners (`StaleBanner`, `ConversionBanner`, gated on `uiState.isStale` and
`uiState.isConversionUnavailable` respectively) — every Composable in the project sits at 0%
coverage, and this is the screen 2.7 predicted would first outgrow a ViewModel test.

Android is the only target here, so TaigaMobileNova's own Compose UI test technique doesn't
transfer as-is: Taiga runs `runComposeUiTest` on a `jvm()` desktop target
(`TaigaMobileNova/docs/testing/survey.md`, `docs/testing/improvement-plan.md` tasks 10–16), and
WallosMobile declares no `jvm()` target at all (`CLAUDE.md`'s Non-negotiables — platform targets
are `configureKmp()`'s call only). The mechanism here has to be a real Android instrumented test —
`androidDeviceTest`, the same source set 3.3 already paid the setup cost for on `core:storage`'s
Room DAOs, wired the identical way (`withDeviceTestBuilder { sourceSetTreeName = null }` in the
module's own `build.gradle.kts`, not `build-logic` — this is the second module that needs one, not
every `ui` module by default). Two steps, wiring then coverage, the same split M18 used for storage
then screen.

**CI stays out of scope, following 3.3's own precedent.** `core:storage:connectedAndroidDeviceTest`
already isn't in `allTests` and CI has no emulator (`CLAUDE.md`'s build-commands section) — a
second device-only suite gets the same treatment rather than being the one that finally earns an
emulator job, which both 2.7 and 3.12 left open. Revisit only if the maintainer actually wants CI
device coverage; nothing here forecloses it.

- [x] **19.1 — feature:subscriptions:ui: wire `androidDeviceTest` for Compose, spike one render**
  Add the Compose UI test artifacts to `gradle/libs.versions.toml` (check
  `TaigaMobileNova/uikit/build.gradle.kts` — task 10 in its `docs/testing/improvement-plan.md` —
  for the `compose.dependencies.uiTest`/`ui-test-manifest` wiring shape, then confirm which
  artifact actually resolves for an `androidTarget` `androidDeviceTest` compilation rather than
  assuming the desktop one carries over unchanged: Taiga attaches this to a `jvmTest`, this module
  attaches it to a real device instead). Wire `feature:subscriptions:ui`'s own `androidDeviceTest`
  source set, same shape as `core/storage/build.gradle.kts`'s
  `withDeviceTestBuilder { sourceSetTreeName = null }`.
  Write one test that renders something real from this module — `SubscriptionsContent` is
  `private` in `SubscriptionsScreen.kt` today, so the first thing this step has to settle is
  whether that becomes `internal` (a device-test compilation is a friend of the `androidMain`/
  `commonMain` it tests, same as any AGP `androidTest`, so `internal` should cross that boundary —
  confirm rather than assume) or whether the test goes through the public `SubscriptionsScreen`
  entry point instead. Assert on one visible node (e.g. the empty state's text) — this step proves
  the wiring, not the matrix.
  *Verify:* `./gradlew :feature:subscriptions:ui:connectedAndroidDeviceTest` with an emulator up,
  one passing test.
  ·  *Ref:* `core/storage/build.gradle.kts`; `TaigaMobileNova/docs/testing/improvement-plan.md`
  task 10 (`uikit/build.gradle.kts`'s one-time Compose UI test dependency addition) — read for the
  *shape* of the wiring, not the exact artifact, since Taiga's is desktop and this one is Android.
  **Note:** Artifacts confirmed by resolving `androidCompileClasspath`/the KMP module metadata rather
  than guessing: `org.jetbrains.compose.ui:ui-test-junit4` (matches Taiga's `uiTest`/`uiTestJUnit4`
  shape) is enough alone — its module metadata forces `androidx.compose.ui:ui-test-junit4:1.11.2`
  on the android target (the whole `androidx.compose.ui` group resolves to 1.11.2 here, confirmed the
  same way) and pulls `ui-test` transitively, so no separate `ui-test` catalog entry was needed.
  `androidx.compose.ui:ui-test-manifest` has no JetBrains multiplatform wrapper at all (it's
  Android-instrumentation-only) and needed its own pinned version (`androidxComposeUiTest = 1.11.2`)
  to match. Both landed in `gradle/libs.versions.toml`, not inline, per this project's own convention.
  A second, unplanned gotcha: the transitively-pulled `androidx.test.espresso:espresso-core:3.5.0`
  reflects for `android.hardware.input.InputManager.getInstance()`, removed on API 34+, so
  `connectedAndroidDeviceTest` crashed with `NoSuchMethodException` on this AVD's API 36 the first
  run — fixed by forcing `espresso-core:3.7.0` (new `androidxEspresso` catalog entry, added as a
  direct `androidDeviceTest` dependency). `SubscriptionsContent` went `internal`, not through the
  public `SubscriptionsScreen`: the private function already took `SubscriptionsUiState` and a plain
  callback, no ViewModel/Koin needed, matching this project's own no-op-default UI-state shape.
  `SubscriptionsScreenTest` renders the default (empty) `SubscriptionsUiState` and asserts
  `RString.subscriptions_empty`'s resolved text exists — one test, passing on-device
  (`Medium_Phone_API_36.1`). `createComposeRule()` prints a v1→v2 deprecation warning
  (`StandardTestDispatcher` vs `UnconfinedTestDispatcher`); left as-is, matching
  `TaigaMobileNova/docs/testing/compose-ui-test-spike.md`'s own "known follow-up, not chased here."

- [ ] **19.2 — feature:subscriptions:ui: cover the list screen's four states and two banners**
  The real matrix 3.12 dated: `SubscriptionsContent` rendered once per derived state (`isStale`,
  `isFailed`, `isEmpty`, `isNoMatch`, plus the ordinary loaded case) built from a
  `SubscriptionsUiState` fixture, asserting the right content is on screen each time — the loading
  spinner, the empty-state text, the no-match Clear button, the error state's Try again, and real
  rows. Separately, `StaleBanner` and `ConversionBanner` each get a render assertion for their
  visible text/actions. `SubscriptionsUiState`'s no-op-default callback shape (`CLAUDE.md`'s UI
  state rules) means each fixture needs no real ViewModel — the same reason a `commonTest`
  ViewModel test doesn't need one either.
  *Verify:* `./gradlew :feature:subscriptions:ui:connectedAndroidDeviceTest` with an emulator up,
  covering all four derived states and both banners.
  ·  *Ref:* `feature/subscriptions/ui/.../list/SubscriptionsScreen.kt`,
  `feature/subscriptions/ui/.../widgets/StaleBanner.kt`,
  `feature/subscriptions/ui/.../widgets/ConversionBanner.kt`; 3.5's and 3.11's own `Note:`s for
  what each banner is supposed to show and when.

---

## To review

Written when M2 closed, as the place a verification step files a defect it finds rather than
fixing in place (**3.12** kept to that shape) — renamed from "Still open after v1" once it grew
past that: a park for anything that isn't today's work, whether an agent found it mid-step or the
user found it using the app, to come back to once there's room. Six entries left this list to
become M5, and one — the dashboard-vs-web comparison filed 2026-08-08 — left it to become **M10**;
see `archive/DEVIATIONS.md` for how the first six closed. Two more, filed the same day the user
compared 10.6/10.7's numbers against the real logged-in web dashboard, became M10's own **10.8**
and **10.9** instead of a fresh milestone — M10's own preamble (now `archive/CHECKLIST-DONE.md`,
M10 having closed) has the root cause for both; see it there, not here, since it stays with the
steps rather than duplicated in this list. One more — "Show the connected server in Settings" —
left it to become **M11** (now closed; `archive/CHECKLIST-DONE.md`), 2026-08-09. One more — "A
user-configurable start destination" — left it to become **M12**, 2026-08-09, picked by the user
over the other three real backlog candidates at the time. One more — "Why does a real account have
no API key yet, when the web frontend logs in fine?" — was answered and closed with no app change,
2026-08-09: Wallos backfills `api_key` for every existing user in `migrations/000029.php`, but that
migration (like all of them) only runs from `startup.sh` at **container boot**, not on login or
page load — confirmed against the local `wallos` container's own `startup.sh` and its `migrations`
table. A long-uptime container that hasn't restarted since before that migration shipped
(2024-10-04) can go on authenticating fine via session cookie indefinitely while never generating a
key; a restart, or clicking regenerate on Profile, fixes it immediately. Two more — the
FAB-slow-open and list-scroll-laggy entries below — were investigated together 2026-08-09
(`docs/issues/2026-08-09-fab-open-and-list-scroll-jank.md`): a scoped Coil concurrency-cap fix
landed the same day (`a0cf54d`, outside the checklist step process since it was framed as a bug fix
rather than a milestone step) and closed the Coil half of both; the JIT-compilation floor the doc
found underneath both left it to become **M13**, 2026-08-10, closed the same day — the JIT
mechanism itself is confirmed gone (zero `Lock contention on Jit code cache for mutator` slices,
was 119), but the aggregate frame-jank numbers the doc used as the user-visible proxy did **not**
improve on this AVD (`archive/CHECKLIST-DONE.md`'s 13.2 has the honest measurement). A same-day
addendum measuring the FAB item's own complaint directly (tap-to-screen-visible latency, not the
scroll-jank proxy) found no measurable difference from list→detail any more — see the FAB entry
below. The scroll item stays unresolved by M13 alone; the FAB item's JIT half now reads as fixed,
though its separate network-wait half still doesn't. Both entries below are updated in place rather
than removed, since the FAB item still carries an unresolved network-wait half that isn't part of
M13 either. Resolved entries aren't repeated here.
Three of what's left are
standing decisions the user owns, kept here as the permanent answer rather than something to
re-open; the rest is real backlog. **Don't re-open the first three per step** — the pre-v1 and
Kover-floor ones have each been settled twice, the certificate-trust one once (2026-08-09).

- **The pre-v1 no-backcompat bullet in `CLAUDE.md` expires at the first outside install** — see
  2.7's first deferred item. Nothing has changed yet: nobody but us has installed the app.
  **The user reaffirmed this on 2026-08-04 and owns the trigger**: ignore backward compatibility
  entirely until they say otherwise, and don't re-open the question per step. Destructive Room
  fallbacks and renamed DataStore keys are free until that word comes.
  **M3 raised the stakes and 3.11 raised them again, and 7.7 a third time**: there is now a Room
  schema in the picture, and it is already at version 3, so a released app needs real migrations
  rather than the destructive fallback the pre-v1 rule permits.
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
  **The Compose-UI-test half left this list to become M19, 2026-08-12** — see that milestone's own
  preamble for why Taiga's `jvm()`-based technique doesn't transfer and what shape the first test
  takes. The Kover-floor half above stays parked on its own terms.
- **A certificate-trust prompt anywhere a refresh can fail, not only on the login screen** (3.8) —
  **decided against, 2026-08-09.** 5.1 already closed the copy half (a rotated certificate names
  itself in the stale banner/error message and points at Disconnect); this would have added the
  actual dialog-and-retry to the other ~13 call sites across the app (every list, detail, editor
  and dashboard card that calls `getErrorMessage`), which is a lot of surface for an event that's
  rare in practice (a homelab cert rotating after onboarding) and already has a working, if
  clunkier, recovery path. Checked TaigaMobileNova's own `docs/features/private-cert-trust/plan.md`
  first, since this feature was ported from there: it hit the identical question and made the same
  call under "Out of Scope" — the dialog stays login-only there too, everywhere else falls back to
  the generic message. The user confirmed the same tradeoff applies here. **Don't re-open this per
  step; it needs a reason to come back**, such as a user actually hitting a mid-session cert
  rotation and finding the fallback message insufficient.
- **The FAB → add-subscription screen is still slower to open than list → detail, after 4.4's fix.**
  4.4 shipped a real, tested, on-device-confirmed improvement — each no-cache picker
  (`EditorPickerUiState.isLoading`, category/payer/paymentMethod) now shows a spinner instead of
  sitting silently empty while `loadCategories`/`loadPayers`/`loadPaymentMethods` are in flight — but
  the user still sees the screen itself take a while to open, which that fix never addressed. Two
  separate, real costs, only one still unscoped:
  1. **The network wait — still open, unscoped.** 2 of the 3 picker calls land together ~500–700ms
     after the request (the third, `get_household`, is fast — under 15ms) against the local
     instance, with no retries or exceptions logged. Confirmed server-side, not client:
     `LoginThrottle` only gates `login.php`/`totp.php`, `NetworkModule.kt` sets no connection-pool
     limit, and a bare `curl` to the same three endpoints from the host resolved in ~7ms each — so
     whatever serializes two of the three only shows up through the app's own request pattern
     (PHP-FPM worker count or session-file locking are the live guesses, still unconfirmed). Fixing
     this for real means giving these three repositories a cache the way `SubscriptionsRepository`
     already has one — Phase 5 management-screen scope, not a small change. Filed 2026-08-07; the
     next session picking this up should read this entry before re-deriving the measurement.
  2. **The JIT warm-up tax on cold navigation — addressed by M13, but the "fixed" verdict below
     rested on an unapplied profile. Now corrected: real improvement, not the original
     "indistinguishable" claim.** Re-investigated 2026-08-09 alongside the scroll-laggy item
     below (`docs/issues/2026-08-09-fab-open-and-list-scroll-jank.md`), which confirmed the same
     mechanism (ART JIT-compiling this process's cold code paths under a lock the main thread's
     rendering work contends on) recurs on both screens, and decomposed into **M13** (an Android
     Baseline Profile), 2026-08-10, closed the same day. 13.2's own scroll-based measurement
     (`archive/CHECKLIST-DONE.md`) left this "not confirmed" since it never directly timed FAB-open
     against a baseline — a same-day addendum did, with `dumpsys gfxinfo`/`framestats`
     (tap-to-settled-frame, cold process, `am force-stop` between runs): FAB→editor 245ms/250ms
     across two runs, row-tap→detail 242ms/250ms — indistinguishable, read as fixed at the time.
     **A second same-day investigation found that verdict was measuring an unapplied profile**
     (`docs/issues/2026-08-10-editor-open-stall-and-unapplied-profile.md`): `dumpsys package`
     showed `[status=verify] [reason=install]` on the exact build variant the addendum measured —
     `adb install` never triggered `speed-profile` dexopt, and the project had no
     `androidx.profileinstaller` dependency to do it automatically outside Play. **Fixed**: added
     `androidx.profileinstaller` (`gradle/libs.versions.toml`, `androidApp/build.gradle.kts`),
     verified end-to-end on-device (fresh install → one launch → `ProfileInstaller` auto-fires →
     `[status=speed-profile] [reason=bg-dexopt]`, no manual force needed). Re-measured:
     editor-open worst frame **150ms → 117ms reproducibly**, a real ~22% improvement, not the
     "indistinguishable from list→detail" the addendum originally claimed — a further ~600-class
     first-touch load of Compose's text-field internals accounts for most of what remains (that
     doc's Findings 3–4), unscoped, not concentrated in any one composable (pickers: only 14% of
     the effect, tested by bisection). **The scroll-jank verdict, re-checked against the same
     correctly-compiled build, did not change** (94.8% jank, 107.53ms worst frame — statistically
     the same as 13.2's own 93%/101.9ms and 88%/107.3ms) — that "did not improve" result stands
     on its own, unaffected by the profile-application gap. See the doc's "What landed" section
     for full numbers.
- **The subscriptions list scrolls laggy.** Filed 2026-08-08 by the user; investigated 2026-08-09
  (`docs/issues/2026-08-09-fab-open-and-list-scroll-jank.md`), together with the FAB item above on
  the hunch they shared a cause — confirmed true. A static code trace ruled out all three original
  guesses (missing `key`, unstable item type, ViewModel flow re-emission during scroll — none
  survive a read of `SubscriptionsScreen.kt`/`SubscriptionCard.kt`/`SubscriptionsViewModel.kt`).
  Two real causes turned up by trace instead:
  - **Coil loading ~20+ previously-unfetched logos at once on a fast fling, contending on a lock
    inside Coil's own disk-cache writer — fixed and verified, `a0cf54d`.**
    `AppModule.provideImageLoader`'s fetcher concurrency is now capped at 4
    (`fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(4))`); on-device contention dropped
    from 18 events/50.6ms to 0 across two follow-up cold-scroll runs.
  - **The same JIT-compilation floor as the FAB item above — addressed by M13, still open as a
    user-visible complaint.** The Coil fix alone didn't move it: overall frame-jank numbers stayed
    flat even with Coil contention at zero, confirming Coil was never the dominant cause of the
    *aggregate* jank this AVD measures. Folded into **M13** (an Android Baseline Profile) alongside
    the FAB item's JIT half, 2026-08-10, closed the same day: the profile eliminates JIT-code-cache
    lock contention on the list-scroll path too (confirmed, reproduced across two runs), but the
    doc's own frame-jank/worst-frame numbers — the metric closest to "does it feel laggy" — did not
    improve and read worse in both post-profile runs on this AVD (`archive/CHECKLIST-DONE.md`'s
    13.2 has the full numbers and the caveats around them). **The user's original complaint is not
    confirmed fixed** — real hardware, not this software-rendered AVD, is the only way to settle
    whether the profile actually helps a real user's felt experience.
- **The Subscriptions list flashed its empty-state text on every login — fixed and verified,
  2026-08-10, outside the checklist step process, same shape as `a0cf54d`.**
  Two compounding causes in `SubscriptionsViewModel`: (1) `_uiState`'s initial value defaulted
  `isLoading = false`, so the screen's very first frame — before `init`'s `load()` had a chance to
  run — read as "checked, found nothing" rather than "about to load"; (2) `onRefreshed()` cleared
  `isLoading` the moment the network call returned, which only proves the write reached Room, not
  that `observeCache()`'s own long-lived collector had re-run against it — a separate, genuinely
  slower, cross-thread hop. Fixed by seeding `isLoading = true` and by having `onRefreshed()` await
  a fresh `observeSubscriptions().first()` (which, unlike the long-lived collector, always re-runs
  its query against current state) before declaring the refresh done. Regression test needed a fake
  repository upgrade (`queryDelay`, modelling that a cold `Flow` collection genuinely re-queries,
  unlike a plain `StateFlow`) to actually reproduce the race rather than the atomic write the old
  fake collapsed it into.
- **The Login screen doesn't scroll — filed 2026-08-10 by the user, fixed and verified the same
  day, outside the checklist step process.** Investigated per
  `docs/issues/2026-08-10-login-screen-doesnt-scroll.md`: the checklist's own prior lead
  (`Arrangement.spacedBy(..., Alignment.CenterVertically)` on a `verticalScroll` `Column`,
  `LoginScreen.kt:107-114`) did not reproduce — on `Medium_Phone_API_36.1` (API 36), forcing real
  overflow via landscape still scrolled correctly by hand, keyboard open or not. The real repro
  needed the user's own physical device (`SM-A920F`, Android 10/API 29): there, opening the
  keyboard left the *entire* content area blank, not just the tail end unreachable. Root cause,
  found by the user directly: `androidApp/src/main/AndroidManifest.xml`'s `MainActivity` declared
  no `android:windowSoftInputMode`, so `imePadding()` (used here and in
  `AuthenticatedMainScreen.kt`) had no correct IME insets to push content against on that API
  level — API 36's own insets dispatch masks the gap, which is why the emulator never reproduced
  it. Fixed with a single `android:windowSoftInputMode="adjustResize"` on the activity.
- **TaigaMobileNova recently did a security review and a testing overhaul — investigated
  2026-08-11, left this list to become M17 on the security half.** Filed 2026-08-10 by the user.
  Read `TaigaMobileNova/docs/security/` (`masvs.md`, `masvs-review-plan.md`) and `docs/testing/`
  (`improvement-plan.md`, `survey.md`, `compose-ui-test-spike.md`) in full, then checked both
  against WallosMobile's actual source rather than assuming the parallel holds. **Security: real
  gap, decomposed into M17** (see its preamble above for the full comparison — short version:
  WallosMobile already has a Keystore-backed cipher over the API key and a ported
  `CompositeTrustManager`, both further along than Taiga's own starting point, but Network/Auth/
  Platform/Code/Privacy/Resilience have never been reviewed at all). **Testing: next milestone
  after M17, not folded into it.** Taiga's Compose UI test sweep runs via a `jvmTest` source set
  (Compose Desktop test artifacts), and WallosMobile declares no `jvm()` target, so that exact
  mechanism doesn't transfer as-is — but that's a setup gap for the next milestone to close, not a
  reason to drop the idea: once M17 closes, scope whether to add a `jvm()` target (so
  `runComposeUiTest` can run in `jvmTest` the same way Taiga's does) or build out the
  `androidDeviceTest` route instead (3.3 already paid part of that setup cost). The settled
  no-Kover-floor decision is unaffected either way — Taiga's survey/heuristics work didn't surface
  anything that reopens it.

