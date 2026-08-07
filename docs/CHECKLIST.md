# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `11/11` · M2 `7/7` — **v1 done** · M3 `12/12` — **Phase 2b done** ·
M4 `5/5` — **Phase 2c done** · M5 `6/6` — **M5 done** · M6 `2/2` — **M6 done** · M7 `9/9`
**Current step:** M7 done — Phase 3 complete. Next: decompose Phase 4 (Dashboard) into M8.

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
through M7**, verbatim. On 2026-08-06 the per-step Deviations log that used to sit at the bottom of
this file moved to [`archive/DEVIATIONS.md`](./archive/DEVIATIONS.md), **frozen** rather than
carried forward: almost every row already had a permanent home in `IMPLEMENTATION_PLAN.md`
(`now in plan §X`), so the practice of appending one was retired as pure duplication — the fold
into the plan is what future sessions actually read. A step's own `Note:` is still where a
deviation gets written down, same as always.

**M5 is done**: the list below filed six defects and all six are now closed, so what remains is
policy and deferred *features* rather than known-wrong behaviour. **M6 is done** too — the
launcher icon and the store flavours, the two things only visible from outside the code. **M7 is
done** too — plan §8's Phase 3 (subscription writes, reference-data pickers), decomposed the way
Phase 2b became M3; see `archive/CHECKLIST-DONE.md` for its nine steps. **Phase 4 (Dashboard) has
not been decomposed into a milestone yet** — there is no M8 section below until that happens.

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
- **Version gating (plan §4.6) is still unowned.** It gates `get_period_budget`, `set_budget`'s
  period fields, `logo_variant` and `square_icons` — all Phase 4 and 5 surface, so M3 leaves it
  alone deliberately rather than by oversight.
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
- **The detail screen's logo flickers on every open.** Root cause found and confirmed 2026-08-07,
  fix deferred at the user's choice. `SubscriptionDetailViewModel.load()` refreshes on every entry
  to the screen; a successful refresh bumps `refreshGeneration` (5.6), which `SubscriptionLogo`
  feeds into Coil's `memoryCacheKeyExtra` so a row that previously errored gets a real retry once
  the server is back (Coil won't retry a request already marked `Error`). The same bump fires on
  *every* successful refresh, not only a recovery from a prior error, so a logo that already loaded
  fine drops out of memory cache and does a visible memory-miss-then-disk-hit round trip for no
  reason — the "flicker". Fix is narrow: only bump the token when the row is recovering from an
  error, not unconditionally. Touches `SubscriptionDetailViewModel`'s `onCached`/`onRefreshed` and
  the ViewModel test file that already covers `logoRefreshToken` (5.6, "a successful refresh bumps
  the logo refresh token" / "a failed refresh leaves the logo refresh token alone") — those two
  tests would need a third state (recovering vs. already-fine) to actually prove the fix.
- **The FAB → add-subscription screen feels slower to open than list → detail.** Filed 2026-08-07,
  not yet measured — this is the likely cause read off the code, not a timed comparison. The detail
  screen is cache-first (3.4): the cached row renders instantly and a single `refreshSubscription`
  round trip happens invisibly behind it. `SubscriptionEditorViewModel`'s add path has no cache to
  be first with — `loadCategories`/`loadPayers`/`loadPaymentMethods` are three independent
  `viewModelScope.launch`es (so they run concurrently, not serially) each hitting
  `CategoriesRepository`/`HouseholdRepository`/`PaymentMethodsRepository`, and 7.2's own Note
  already says why: those three have "no cache behind them... every call is a round trip", a
  deliberate scope cut for M7 rather than an oversight. So opening the add screen always costs at
  least one full round trip with nothing to show meanwhile (empty pickers, no fallback), where the
  detail screen never blocks on the network for anything on screen. Worth confirming with the
  logcat `REQUEST:`/`RESPONSE:` timestamps (per `CLAUDE.md`'s own technique) before touching
  anything — if this is right, giving these three a cache is Phase 5 management-screen scope, not a
  small fix.

