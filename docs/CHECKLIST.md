# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `11/11` · M2 `7/7` — **v1 done** · M3 `12/12` — **Phase 2b done** ·
M4 `5/5` — **Phase 2c done** · M5 `6/6` — **M5 done** · M6 `2/2` — **M6 done** · M7 `9/9` ·
M8 `4/4` — **M8 done** · M9 `9/9` — **M9 done** · M10 `9/9` — **M10 done** · M11 `1/1` —
**M11 done** · M12 `1/3`
**Current step:** M12's 12.1 done (core/storage: `StartDestination` + `StartDestinationStorage`).
Next: 12.2.

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
through M8, and now M10, M9 and M11**, verbatim. M10 was archived once before too (2026-08-08, its first seven
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
Settings screen; see `archive/CHECKLIST-DONE.md`.

---

## M12 — User-configurable start destination (not in plan §8's phase order)

Goal: replace the hard-coded `START_DESTINATION` constant with a stored preference the user can
change from Settings, covering any of the 7 drawer sections — Dashboard, Subscriptions, Settings,
Categories, Household, Payment methods, Currencies — not just Dashboard-vs-Subscriptions, which
was the open scoping question this item carried in "To review". **Done when** picking a different
start screen and cold-starting the app (not just backgrounding it) opens there instead, with the
back stack still restoring correctly on process death.

Filed to "To review" 2026-08-08, decomposed 2026-08-09 — picked over the other backlog items by
the user. `feature:settings:ui`'s existing "Interface" sub-screen (9.9-era work:
`appearance/ThemeMode` + `ThemeStorage`) is the precedent to copy directly: same `core/storage`
interface+impl shape, same ui-only-feature ViewModel taking the storage straight (no repository),
same `RadioButton` `Column` picker. `core/storage` cannot depend on any route type —
`DrawerDestination` and the `NavKey` subclasses live in `composeApp` and feature `ui` modules,
layers above it — so the stored value is a new, self-contained enum in `core/storage` (seven cases
mirroring `DrawerDestination`'s names), and `composeApp` is the only place that maps it to an
actual `NavKey`.

- [x] **12.1 — core/storage: `StartDestination` + `StartDestinationStorage`**
  Mirrors `ThemeMode`/`ThemeStorage` exactly: a `StartDestination` enum persisted by a stable
  `value: String` (not ordinal) with seven cases — `Dashboard`, `Subscriptions`, `Settings`,
  `Categories`, `Household`, `PaymentMethods`, `Currencies` — `default()` returning `Dashboard`,
  and a `StartDestinationStorage` interface (`val startDestination: Flow<StartDestination>`,
  `suspend fun setStartDestination(value: StartDestination)`) + `@Single`-bound `internal` impl
  sharing the module's one DataStore file, its own `stringPreferencesKey` in a
  `private companion object`, falling back to `default()` on an unset or unrecognized value.
  *Verify:* `./gradlew :core:storage:testAndroidHostTest` — round-trips a stored value, and falls
  back to `Dashboard` for an unset or unrecognized key (mirrors `ThemeStorageImplTest`).
  ·  *Ref:* `core/storage/.../theme/ThemeMode.kt`, `ThemeStorage.kt`, `ThemeStorageImpl.kt`
  ·  *Note:* Implemented exactly as specced, package `core/storage/.../startdestination/`. Five
  tests in `StartDestinationStorageImplTest` (default, observed write, pre-stored value, unrecognised
  value, dedup on identical write), all green — `testAndroidHostTest` green, `detekt`/`ktlintCheck`
  green, and `:composeApp:compileGplayDebugKotlin --rerun-tasks` + `KoinGraphTest` both confirm the
  new `@Single` resolves with no further wiring: `StorageModule`'s existing `@ComponentScan` over
  `core.storage` picked it up automatically, same as `ThemeStorageImpl`.

- [ ] **12.2 — feature:settings:ui: a "Startup screen" picker**
  A new sub-screen off Settings, mirroring `appearance/InterfaceScreen`'s shape exactly (Route/
  Screen/UiState/ViewModel, `RadioButton` + `Column` picker — seven rows now, still fixed-item so
  still a `Column`, not a `LazyColumn`), reached the way Interface/Profile/About are: a new
  `SettingsRow` on `SettingsScreen` (pushing it to a fourth callback — still exempt under
  `compose:parameter-order`'s single-trailing-function rule, `viewModel` stays last), its `Route`
  registered in `NavKeySerializers.kt` and wired into `SettingsEntryProvider.kt`. This is a
  Settings sub-screen, not a drawer destination, so it needs no `DrawerDestination`/
  `DRAWER_NAV_ITEMS` entry — 9.9's `ProfileRoute` is the precedent for this whole shape, not
  9.1–9.7's drawer-destination one. `StartDestinationViewModel` takes `StartDestinationStorage`
  directly, no repository.
  *Verify:* `./gradlew :feature:settings:ui:testAndroidHostTest` (fake `StartDestinationStorage`,
  mirrors `InterfaceViewModelTest`), and on the emulator: Settings → the new row → pick each of the
  seven options, confirm the radio selection persists across leaving and re-entering the screen.
  ·  *Ref:* `feature/settings/ui/.../appearance/` (whole package), `.../SettingsScreen.kt`,
  `composeApp/.../nav/entries/SettingsEntryProvider.kt`, `composeApp/.../nav/NavKeySerializers.kt`

- [ ] **12.3 — composeApp: read the stored preference into the real start destination**
  Replaces the `START_DESTINATION` constant (`nav/DrawerDestination.kt`) with a small
  `StartDestination -> NavKey` mapper (a `when` over the seven cases, returning each
  `DrawerDestination` entry's own `.route`) and threads the stored value from `WallosAppContent`
  down to `rememberNavigationState`'s `startKey` **the same way `themeMode` already is** — the
  load-bearing precedent already in this file (`WallosAppContent.kt`'s own comment on
  `rememberNavBackStack` consuming `startKey` only on the first composition): read
  `startDestinationStorage.startDestination.collectAsState(initial = StartDestination.default())`
  at the `WallosAppContent` call site, pass the mapped `NavKey` into
  `rememberMainAppState(startDestination = ...)`, and thread it into `rememberNavigationState`'s
  `startKey` param. Never gate `AuthenticatedMainScreen`/`rememberMainAppState()`'s composition on
  the read finishing — `isConnected` is the one deliberate exception to that rule in this file,
  `themeMode` is not, and this follows `themeMode`.
  *Verify:* on the emulator against the live instance — set the picker to Subscriptions, force-stop
  and cold-start the app (`emulator-testing` skill's `am start -n` recipe, not `monkey`: 11.1 found
  `monkey` unreliable even on the first relaunch), confirm it opens on Subscriptions; then navigate
  a few levels deep into a different section, `am kill` + cold-start again, and confirm the back
  stack still restores correctly (the regression this design exists to avoid).
  ·  *Ref:* `WallosAppContent.kt`'s `themeMode` handling, `MainAppState.kt`, `NavigationState.kt`

**M12 is done** once all three steps are ticked — archive the same session, per the "one move per
milestone" rule.

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
over the other three real backlog candidates at the time. Resolved entries aren't repeated here.
Two of what's left are
standing decisions the user owns, kept here as the permanent answer rather than something to
re-open; the rest is real backlog. **Don't re-open the first two per step; they have both been
settled twice.**

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
- **The subscriptions list scrolls laggy.** Filed 2026-08-08 by the user, not yet investigated —
  no profiling done, so the cause (each `SubscriptionCard`'s Coil logo load, recomposition from
  `SubscriptionsViewModel`'s combined flow re-emitting more than the scroll needs, something in the
  `LazyColumn` item content itself) is a guess, not a finding. The `emulator-testing` skill's Step
  4b (`dumpsys gfxinfo`/Perfetto, written up 2026-08-07 for the FAB-open investigation in this same
  list) is the right technique to reach for first, not a fix guessed from the symptom alone.

