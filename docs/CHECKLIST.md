# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `11/11` · M2 `7/7` — **v1 done** · M3 `12/12` — **Phase 2b done** ·
M4 `5/5` — **Phase 2c done** · M5 `6/6` — **M5 done** · M6 `2/2` — **M6 done** · M7 `9/9` ·
M8 `4/4` — **M8 done**
**Current step:** M8 done — Phase 4 complete. Next: decompose Phase 5 (Management screens).

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
  put a file rename in every commit.

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
through M8**, verbatim. On 2026-08-06 the per-step Deviations log that used to sit at the bottom of
this file moved to [`archive/DEVIATIONS.md`](./archive/DEVIATIONS.md), **frozen** rather than
carried forward: almost every row already had a permanent home in `IMPLEMENTATION_PLAN.md`
(`now in plan §X`), so the practice of appending one was retired as pure duplication — the fold
into the plan is what future sessions actually read. A step's own `Note:` is still where a
deviation gets written down, same as always.

**M5 is done**: the list below filed six defects and all six are now closed, so what remains is
policy and deferred *features* rather than known-wrong behaviour. **M6 is done** too — the
launcher icon and the store flavours, the two things only visible from outside the code. **M7 is
done** too — plan §8's Phase 3 (subscription writes, reference-data pickers), decomposed the way
Phase 2b became M3; see `archive/CHECKLIST-DONE.md` for its nine steps. **M8 is done** too — plan
§8's Phase 4 (Dashboard: monthly cost, period budget, upcoming payments), the app's first use case
and its new landing screen; see `archive/CHECKLIST-DONE.md` for its four steps. Phase 5
(Management screens) is not yet decomposed.

---

## To review

Written when M2 closed, as the place a verification step files a defect it finds rather than
fixing in place (**3.12** kept to that shape) — renamed from "Still open after v1" once it grew
past that: a park for anything that isn't today's work, whether an agent found it mid-step or the
user found it using the app, to come back to once there's room. Six entries left this list to
become M5 — see `archive/DEVIATIONS.md` and `archive/CHECKLIST-DONE.md` for how each closed;
resolved entries aren't repeated here. Two of what's left are standing decisions the user owns,
kept here as the permanent answer rather than something to re-open; the rest is real backlog.
**Don't re-open the first two per step; they have both been settled twice.**

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
  5.1 closed the copy half (a rotated certificate names itself in the stale banner and points at
  Disconnect) but not this one, which is a bigger change: it would put a pin write outside
  `SetupRepository`.
- **Version gating (plan §4.6) is partly owned now.** M8 (8.1/8.4) gates `get_period_budget`
  reactively — off `WallosError.UnsupportedEndpoint`, not a stored version, since no minimum
  version is documented anywhere to compare against (M8's own preamble has the detail). That
  leaves `set_budget`'s period fields, `logo_variant` and `square_icons` still unowned, all Phase 5
  surface — a real `VersionStorage` gets built there if one of those three turns out to need an
  upfront check rather than the same reactive pattern.
- **Why does a real account (`gregorz` on the user's own `sbscrpt.gregstuff.click` instance) have
  no API key yet, when the web frontend logs in fine?** Not a bug in this app — confirmed against
  `WALLOS_API.md` §2 that this is Wallos's own design: `api/*.php` ignores the session cookie
  entirely and resolves the caller by `api_key` alone, so a working frontend session proves nothing
  about whether one has ever been generated. Filed 2026-08-07 as a "this looks strange" from the
  user, worth a closer look rather than acted on: is `api_key` seeded at registration and this
  account predates that, was it cleared by something, or does Wallos genuinely never generate one
  until the user visits Profile and clicks generate? `docker exec wallos cat api/... ` /
  `registration.php` source would settle it. No app change implied either way — the fix for the
  user's own account is generating the key once in the web UI — this is purely a "why" to close out
  of curiosity, not a defect to design around.
- **The FAB → add-subscription screen is still slower to open than list → detail, after 4.4's fix.**
  4.4 shipped a real, tested, on-device-confirmed improvement — each no-cache picker
  (`EditorPickerUiState.isLoading`, category/payer/paymentMethod) now shows a spinner instead of
  sitting silently empty while `loadCategories`/`loadPayers`/`loadPaymentMethods` are in flight — but
  the user still sees the screen itself take a while to open, which that fix never addressed. Two
  separate, real costs, neither with a small fix:
  1. **The network wait**: 2 of the 3 picker calls land together ~500–700ms after the request (the
     third, `get_household`, is fast — under 15ms) against the local instance, with no retries or
     exceptions logged. Confirmed server-side, not client: `LoginThrottle` only gates
     `login.php`/`totp.php`, `NetworkModule.kt` sets no connection-pool limit, and a bare `curl` to
     the same three endpoints from the host resolved in ~7ms each — so whatever serializes two of
     the three only shows up through the app's own request pattern (PHP-FPM worker count or
     session-file locking are the live guesses, still unconfirmed). Fixing this for real means giving
     these three repositories a cache the way `SubscriptionsRepository` already has one — Phase 5
     management-screen scope, not a small change.
  2. **A one-time JIT warm-up tax on cold navigation**, found by breaking down a captured Perfetto
     trace frame-by-frame (technique: `emulator-testing` skill's Step 4b, written up 2026-08-07): the
     very first `Choreographer#doFrame` after the FAB tap took 121.9ms by itself, and slice-level
     breakdown showed that time dominated by repeated `Lock contention on Jit code cache for mutator`
     entries — ART's JIT compiling this screen's heavier components (date pickers, dropdown menus;
     more than list/detail exercise) for the first time in the process, while the main thread waits on
     the same lock. Confirmed real by measuring the same screen's *second* visit in the same process:
     90th-percentile frame time roughly halved (450–500ms cold → 200–250ms warm) with nothing else
     changed. This is what Android Baseline Profiles exist to remove; investigating one is unscoped,
     separate work — plan §8/Phase 5 territory at best, not investigated further here.
  Filed 2026-08-07. Not a regression to chase further in a single session — the next session picking
  this up should treat (1) and (2) as two independent tickets with two independent fixes, and read
  this entry plus the Step 4b recipe before re-deriving either measurement.
- **A tentative idea, not a decision: log on tap during emulator regression passes**, so a click's
  effect shows up in `logcat` immediately instead of needing a screenshot read every time. Filed
  2026-08-07, with the user's own caveat attached — not expected to replace screenshots, since the
  UI still has to be checked visually, so at most a supplement for "did the tap even register"
  questions a log line answers faster than a screencap round trip. Touching every clickable in
  every screen (and its preview) for this is not obviously worth it yet; wants a concrete case this
  would have shortened before it becomes a step rather than a hunch.
- **A user-configurable start destination.** 8.4 made `START_DESTINATION` a hard-coded constant
  (`DashboardRoute`), on the reasoning that a drawer ordering with Dashboard first only makes sense
  if it's also where the app opens — but the user wants the choice back: some sessions want
  Subscriptions as the landing screen instead. Filed 2026-08-08. Should be possible without
  disturbing `NavigationState`/`Navigator` — `START_DESTINATION` would read a stored preference
  (`feature:settings`'s local-theme storage is the existing precedent for a device-only setting
  with no server round trip) instead of the constant, with a picker on the Settings screen next to
  Interface. Worth deciding whether the choice is just Dashboard-vs-Subscriptions or any drawer
  destination before this becomes a step. Not tackled here — separate work.
- **The subscriptions list scrolls laggy.** Filed 2026-08-08 by the user, not yet investigated —
  no profiling done, so the cause (each `SubscriptionCard`'s Coil logo load, recomposition from
  `SubscriptionsViewModel`'s combined flow re-emitting more than the scroll needs, something in the
  `LazyColumn` item content itself) is a guess, not a finding. The `emulator-testing` skill's Step
  4b (`dumpsys gfxinfo`/Perfetto, written up 2026-08-07 for the FAB-open investigation in this same
  list) is the right technique to reach for first, not a fix guessed from the symptom alone.
- **Show the connected server in Settings.** Filed 2026-08-08 by the user. `SettingsScreen`
  currently shows Interface/About rows and Disconnect, but never the URL the app is actually
  talking to — `BaseUrlProvider.getBaseUrl()` (`core:api`, already a dependency of
  `feature:subscriptions:ui` for logo URLs) is the existing read path, so this looks like a small
  addition: one more row or a line above Disconnect, no new storage. Not investigated further here.

