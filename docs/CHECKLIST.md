# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `11/11` · M2 `7/7` — **v1 done** · M3 `12/12` — **Phase 2b done** ·
M4 `5/5` — **Phase 2c done** · M5 `6/6` — **M5 done** · M6 `2/2` — **M6 done** · M7 `9/9` ·
M8 `4/4` — **M8 done** · M9 `9/9` — **M9 done** · M10 `9/9` — **M10 done** · M11 `1/1` —
**M11 done** · M12 `3/3` — **M12 done** · M13 `2/2` — **M13 done** · M14 `2/2` — **M14 done** ·
M15 `4/4` — **M15 done** · M16 `5/5` — **M16 done** · M17 `2/8`
**Current step:** M17, step 17.3. 17.2 (Cryptography) closed 2026-08-11: `KeystoreSecretCipher`'s
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
through M8, and now M10, M9, M11, M12, M13, M14, M15 and M16**, verbatim. M10 was archived once before too (2026-08-08, its first seven
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

---

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

- [ ] **17.3 — Network.** Run the three TOFU questions from `kmp-checks.md` against
  `CompositeTrustManager` as it stands today (don't assume Taiga's own review of the pre-port
  version still describes it); record the `usesCleartextTraffic="true"` deviation with its actual
  bound (is the API key ever sent in the clear — check `AuthHeaderPlugin`-equivalent call sites).
  *Verify:* register gains a Network section covering both.

- [ ] **17.4 — Authentication.** Verify the login-bridge shape from this preamble with file:line
  (`WebLoginApi.kt`/`ApiKeyScraper.kt`/`LoginThrottle.kt`) — confirm the password never persists or
  logs beyond the POST call, and that the scrape target is always the user's own configured server.
  Confirm MASVS-AUTH-2/3 (local auth, step-up) are N/A — no biometric anywhere
  (`grep -rln 'biometric\|Biometric\|BiometricPrompt'` already confirmed empty during this
  milestone's scoping; re-confirm rather than trust the preamble).
  *Verify:* register gains an Auth section.

- [ ] **17.5 — Platform.** Confirm the manifest's IPC surface (only `MainActivity` exported, plain
  `MAIN`/`LAUNCHER`, no deep link); check `LoginScreen.kt`'s password field for a reveal toggle and
  decide whether the `FLAG_SECURE` absence is a finding or an accepted product tradeoff (same
  question Taiga's maintainer answered for itself — don't assume the same answer applies here
  without asking).
  *Verify:* register gains a Platform section.

- [ ] **17.6 — Code quality.** Confirm `minSdk = 24`'s rationale (or lack of one, same as Taiga's
  finding) is stated plainly; confirm `renovate.json`'s `osvVulnerabilityAlerts` is still set and
  check `gh api repos/<owner>/<repo>/vulnerability-alerts` for GitHub's native alert status; confirm
  `WallosEnvelopeParser`/the app's JSON config tolerates unknown/null server fields; check both
  `LocalUriHandler.openUri()` call sites in `AboutScreen.kt`.
  *Verify:* register gains a Code section; the dependabot/renovate question has a stated answer,
  not a guess.

- [ ] **17.7 — Privacy.** Confirm both flavors' crash-reporting posture per M16 (Gplay real,
  fdroid no-op) is disclosed correctly in the register; confirm `ApiKeyStorage.clear()`'s
  cache-plus-key eviction actually satisfies MASVS-PRIVACY-4 by reading its three call sites
  (disconnect, both login paths); diff declared permissions (`INTERNET`,
  `ACCESS_NETWORK_STATE`) against actual call sites.
  *Verify:* register gains a Privacy section.

- [ ] **17.8 — Resilience (scope decision only).** Confirm the self-hosted-FOSS-client reasoning
  that makes MASVS-RESILIENCE out of scope actually holds for WallosMobile specifically — check for
  any embedded secret the way Taiga's task 7 did (`grep -rln 'client_secret\|CLIENT_SECRET'` and
  equivalents; WallosMobile has no OAuth flow at all, so this should be an even faster N/A than
  Taiga's). Write the exclusion into the register's header. No code review beyond that — this closes
  the milestone once done.
  *Verify:* register header states the Resilience exclusion with its reason.

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

