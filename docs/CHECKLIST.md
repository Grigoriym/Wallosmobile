# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `11/11` · M2 `7/7` — **v1 done** · M3 `12/12` — **Phase 2b done** ·
M4 `5/5` — **Phase 2c done** · M5 `6/6` — **M5 done** · M6 `2/2` — **M6 done** · M7 `9/9` ·
M8 `4/4` — **M8 done** · M9 `9/9` — **M9 done** · M10 `9/9` — **M10 done** · M11 `1/1` —
**M11 done** · M12 `3/3` — **M12 done** · M13 `2/2` — **M13 done** · M14 `2/2` — **M14 done** ·
M15 `4/4` — **M15 done**
**Current step:** M15 done. 15.4 added `release.yml`: tag-triggered (`v*`) + `workflow_dispatch`
(`tag` input), builds `assembleGplayRelease`/`assembleFdroidRelease`/`bundleGplayRelease` using the
two release keystores restored from `WALLOS_FILE_GPLAY`/`WALLOS_FILE_FDROID`, then publishes a
GitHub Release via `softprops/action-gh-release@v3` — no desktop packaging, no
`google-services.json` restore, matching 15.3's decision that neither exists for this repo. The
live dry run (`gh workflow run release.yml -f tag=v0.0.0-test`) was deferred at the user's request
— it would push a real tag and publish a visible Release on the now-public repo — so only the
YAML's syntax and its post-push `active` status in `gh workflow list` were checked; an end-to-end
dispatch is still unverified and is the user's to trigger whenever they choose. With all four
steps ticked, `dev` is the default branch behaving like `master` did before it, and `master` now
moves only through `release-prepare` → PR → `release-finalize` → tag → `release.yml`, with branch
protection still deliberately deferred (15.1) until the repo nears its first real release.
Next session should pick a new milestone — nothing in M15 carries forward as a dependency.

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
through M8, and now M10, M9, M11, M12, M13, M14 and M15**, verbatim. M10 was archived once before too (2026-08-08, its first seven
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
its four steps.

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

