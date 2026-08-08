# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `11/11` · M2 `7/7` — **v1 done** · M3 `12/12` — **Phase 2b done** ·
M4 `5/5` — **Phase 2c done** · M5 `6/6` — **M5 done** · M6 `2/2` — **M6 done** · M7 `9/9` ·
M8 `3/4`
**Current step:** 8.4

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
Phase 2b became M3; see `archive/CHECKLIST-DONE.md` for its nine steps. **Phase 4 (Dashboard) is
decomposed below as M8.**

---

## M8 — Dashboard (plan §8, Phase 4)

Goal: a home screen showing this month's cost, the period budget, and the subscriptions coming due
soonest — the three things plan §8 names for Phase 4, composed from `get_monthly_cost`,
`get_period_budget` and the cache `SubscriptionsRepository` already keeps. **Done when** the screen
renders all three against the live instance (port 8282) and is the app's landing screen — Dashboard
moves to the top of the drawer per plan §5.4's sketch, ahead of Subscriptions.

Two things plan §10/§4.6 leave open are settled by this decomposition, the same way M7 settled
catalog granularity, so no step below has to re-derive them:

- **No `VersionStorage`, no version-string comparison.** Plan §4.6 reads as "store `version.php`'s
  result and gate `get_period_budget` on it ahead of calling it," but neither `WALLOS_API.md` nor
  the live PHP (`docker exec wallos cat api/subscriptions/get_period_budget.php`, checked while
  decomposing this) names a minimum version — there is no server-side version check to mirror, and
  no changelog in the container to read one off. Guessing a cutoff would be exactly the kind of
  unconfirmed claim `CLAUDE.md` rules out. The gate that already exists for free is reactive:
  `WallosEnvelopeParser` turns any 404 into `WallosError.UnsupportedEndpoint`
  (`core/api/.../WallosEnvelopeParser.kt:35`) for every endpoint, `get_period_budget` included —
  8.1's repository just has to let that surface untouched, and 8.4's UI turns that one specific
  error into "hide the budget card" instead of a banner. This closes the Phase-4 slice of "To
  review"'s unowned version-gating item without building storage nothing would read yet (7.4's
  precedent for not building unreached code). `set_budget`'s period fields, `logo_variant` and
  `square_icons` stay Phase 5's job, and whichever of those steps first needs a real stored version
  is where `VersionStorage` gets built.
- **Network-only, no Room cache.** Unlike `SubscriptionsRepository`, `get_monthly_cost` and
  `get_period_budget` are period-relative snapshots, not a list to page through offline — matching
  `feature:categories`/`household`/`paymentmethods`'s existing precedent (7.2's note) of a plain
  round trip wrapped in `resultOf`, no `observe*`/`refresh*`. Upcoming payments is the one card that
  stays available offline, because it reads `SubscriptionsRepository.observeSubscriptions()`, which
  already is a cache.
- **`feature:dashboard:domain` depends on `feature:subscriptions:domain`**, the second cross-feature
  dependency in the repo after 7.2's mapper one — upcoming payments is `Subscription` rows filtered
  and re-sorted, and duplicating that type here would fork it in two places for no caller.

- [x] **8.1 — feature:dashboard: dto + data + domain — monthly cost & period budget**
  `MonthlyCostDTO`/`PeriodBudgetDTO` (`WALLOS_API.md` §3.5–3.6); domain `MonthlyCost`/
  `PeriodBudget` trimmed to what the screen renders (2.1's rule) — `period_label` is already a
  human-readable string from the server, so check whether a `budget_period_type` enum earns its
  place before adding one. `DashboardRepository` (or a narrower name if one call turns out to want
  no companion) with `getMonthlyCost(month, year)` / `getPeriodBudget(referenceDate)`, hand-written
  against `WallosApiClient` like `SubscriptionsApi` — neither endpoint fits `CrudApi<T>`'s
  add/edit/delete shape, so no `core:crud` dependency (mirrors 7.5's note for the same reason).
  `monthly_cost` is a comma-grouped string, not a JSON number — reuse `MoneyFormatter.parse`
  (`utils:formatter:decimal`), don't write a second parser.
  *Verify:* `./gradlew :feature:dashboard:data:testAndroidHostTest` — both calls' happy path
  against `MockEngine` fixtures, the comma-grouped `monthly_cost` string parsed correctly, and a
  404 on `get_period_budget` surfacing as `WallosError.UnsupportedEndpoint` unchanged.
  ·  *Ref:* `WALLOS_API.md` §3.5–3.6, plan §4.6
  Remember the two easy misses 7.2/7.3 already paid for once: the new modules need a line in root
  `build.gradle.kts`'s `kover { }` block, and `DashboardDataModule`/`DashboardDomainModule` need
  `AppModule`'s `includes` even before anything calls them.
  **Note:** confirmed both endpoints against the live PHP (`docker exec wallos cat
  api/subscriptions/get_{monthly_cost,period_budget}.php`) rather than the doc summary alone —
  both matched exactly. No `feature:dashboard:mapper` module: unlike categories/household/
  paymentmethods, the DTO→domain step here is plain field selection plus one `MoneyFormatter.parse`
  call, with no HTML-unescaping and no second caller, so `MonthlyCostMapper`/`PeriodBudgetMapper`
  are `@Single` classes living in `feature:dashboard:data` itself (still one per file, still
  unit-tested) rather than a separate Gradle module — CLAUDE.md's "add a layer when a real
  repository or a second caller turns up" argues against a module neither condition asks for. No
  `DashboardDomainModule` either: every existing `domain` module (categories/household/
  paymentmethods/subscriptions) has zero `@Single`-annotated definitions to scan, and this one is
  the same — nothing in `domain` needs Koin. `DashboardRepository` came out as one interface with
  both calls, not the narrower split the step floated; nothing pushed the two apart.
  `MonthlyCost` kept `title` (the server's own "March 2025" label) despite the trim, since 8.4's
  card plausibly wants a month heading and it costs nothing to carry. `PeriodBudget` kept both
  `amountRemainingThisPeriod` and `amountOverBudget` rather than deriving one from the other — the
  server clamps the former to 0 once over budget, so an "over by X" display needs the latter
  separately. `composeApp/build.gradle.kts` needed its own new `implementation(projects.feature.
  dashboard.data)` line too (7.2/7.3's reminder didn't name it, but every prior feature's data
  module is wired there the same way).

- [x] **8.2 — feature:dashboard:domain: upcoming payments**
  A pure class (`UpcomingPaymentsCalculator` or similar) taking the cached `List<Subscription>`
  from `SubscriptionsRepository.observeSubscriptions()` and returning the active ones sorted by
  next occurrence. "Derived locally from `next_payment` + `cycle`" (plan §8) means more than a
  sort: the server's own cron keeps `next_payment` current, but this app's cache can lag behind a
  missed refresh, so a `nextPayment` already in the past has to be rolled forward by `cycle` +
  `frequency` until it's today or later before sorting — otherwise a stale row would show at the
  top as "due" when it's already renewed server-side. Excludes inactive rows and ones with a null
  `nextPayment` or an unrecognised `cycle` (`BillingCycle.fromCode` returning `null`), the same way
  the list screen already treats an unparseable row.
  *Verify:* `./gradlew :feature:dashboard:domain:testAndroidHostTest` — a future `nextPayment`
  passes through unchanged; a past one rolls forward the right number of cycles for each
  `BillingCycle` value; inactive/null-cycle rows are excluded; output is sorted ascending.
  ·  *Ref:* plan §8 Phase 4
  **Note:** the exclusion list above turned out to be incomplete — checked the live cron
  (`docker exec wallos cat endpoints/cronjobs/updatenextpayment.php`) that actually rolls
  `next_payment` forward server-side, and its query is `WHERE next_payment < :currentDate AND
  auto_renew = 1 AND inactive = 0`. A past-due row with `autoRenew == false` is never touched by
  that cron, so it stays stuck in the past on the server forever — there is no real "next"
  occurrence to invent client-side, and the user confirmed (asked, since this wasn't in the step
  text) to exclude such a row rather than roll it forward anyway. `BillingCycle.ONE_TIME` gets the
  same treatment when past-due, for the same reason (no periodicity to roll by, and a one-time row
  is never auto-renewing in practice) — a future one-time `nextPayment` still passes through
  unchanged. Also excludes a non-positive `frequency` defensively, since the roll-forward loop
  would never terminate otherwise (the DTO has no validation ruling this out).

- [x] **8.3 — feature:dashboard:domain: DashboardHomeUseCase**
  Composes 8.1's two repository calls and 8.2's calculator over
  `SubscriptionsRepository.observeSubscriptions()` into one result the ViewModel collects — the
  app's first real use case (plan §6: "Wallos has real use-case candidates — see §8 Phase 4").
  The three sources fail independently (`UnsupportedEndpoint` on the budget call must not blank out
  the other two), so the shape this step has to decide is per-source `Result`s rather than one
  failure sinking the whole screen.
  *Verify:* `./gradlew :feature:dashboard:domain:testAndroidHostTest` — all three sources present;
  a period-budget failure still leaves monthly cost and upcoming payments populated; an
  upcoming-payments feed with no active subscriptions renders empty, not failed.
  ·  *Ref:* plan §6 "Use cases only when a screen needs multiple repository calls"
  **Note:** `DashboardHomeUseCase.getDashboardHomeData(today: LocalDate): DashboardHomeData` —
  `today` is a parameter, not read from `Clock.System` internally, mirroring 8.2's calculator
  signature and keeping the use case deterministic to test. `getMonthlyCost` derives month/year
  from it and `getPeriodBudget` gets it as `referenceDate`; `UpcomingPaymentsCalculator` gets it
  unchanged. `DashboardHomeData` (three independent fields: `Result<MonthlyCost>`,
  `Result<PeriodBudget>`, `List<Subscription>`) lives directly as data, not wrapped in an outer
  `Result` — wrapping it would put the three sources back behind one failure, which is the thing
  this step exists to avoid. Upcoming payments comes from a single `observeSubscriptions().first()`
  snapshot rather than staying subscribed to the flow — nothing on this screen refreshes the cache
  itself (8.4's own "nothing on this screen writes"), so there is no second emission to react to
  yet; a future screen that adds pull-to-refresh would be the point to revisit this. This is the
  first domain module with any Koin content in the app (every prior one scanned to zero), so it
  needed `alias(libs.plugins.wallosmobile.kmp.di)` added to its `build.gradle.kts`, a new
  `DashboardDomainModule` (`@Module @Configuration @ComponentScan`), and that module wired into
  `AppModule`'s `includes` in `Koin.kt` — `KoinGraphTest` confirmed the wiring. `composeApp` also
  needed a direct `implementation(projects.feature.dashboard.domain)` line: it already had
  `feature.dashboard.data`, but that dependency is `implementation`, not `api`, so the domain
  module class wasn't visible transitively. `UpcomingPaymentsCalculator` stays unannotated and is
  constructed directly inside `DashboardHomeUseCaseImpl` rather than injected — it has zero
  dependencies of its own, the same "stop injecting it" case CLAUDE.md's cache-repository bullet
  describes, and it keeps the constructor at 2 params instead of 3 for no test benefit (8.2 already
  covers it in isolation). `LocalDate.monthNumber` is deprecated in kotlinx-datetime 0.8.0 but its
  replacement (`.month.number`) doesn't exist in this version's `Month` enum yet, so the deprecated
  member stays rather than reaching for an API that isn't there.

- [ ] **8.4 — feature:dashboard:ui: the home screen, and it becomes the landing screen**
  `DashboardRoute`/`DashboardScreen`/`DashboardViewModel`/`DashboardUiState`: a monthly-cost card,
  a period-budget card that 8.1's `UnsupportedEndpoint` hides rather than errors (this milestone's
  version-gating decision, applied), and an upcoming-payments list whose rows navigate to the
  existing `SubscriptionDetailRoute(id)` — no new detail surface needed. New route registered in
  `DrawerDestination`/`DRAWER_NAV_ITEMS`/`NavKeySerializers` (miss any of the three and either the
  drawer entry does nothing or `NavKeySerializersTest`/process-death restore breaks silently, per
  `CLAUDE.md`'s nav3 rule). Drawer entry added **above** Subscriptions, matching plan §5.4's
  sketch, and **`START_DESTINATION` flips from `SubscriptionsRoute` to `DashboardRoute`** — settled
  here since a drawer ordering that puts Dashboard first only makes sense if it's also where the
  app opens. No FAB, no offline-write gating — nothing on this screen writes.
  *Verify:* on the emulator against the live instance (port 8282, `docs/local-info.txt`) — launch
  the app, land on Dashboard (not Subscriptions), see this month's cost and the upcoming-payments
  list with a real row, tap one and land on its detail screen, back, open the drawer and confirm
  Dashboard sits above Subscriptions. Confirm the period-budget card renders against this instance
  (v5.4.2) rather than only exercising its absence — the hide-on-404 path needs an older instance
  to actually prove, which `docs/local-info.txt`'s throwaway instances may not cover; note in this
  step whether one was available.
  ·  *Ref:* plan §5.4, §7.1 UI-state patterns, `CLAUDE.md`'s Screen/Content split

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

