# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `11/11` · M2 `7/7` — **v1 done** · M3 `12/12` — **Phase 2b done** ·
M4 `5/5` — **Phase 2c done** · M5 `6/6` — **M5 done** · M6 `2/2` — **M6 done** · M7 `9/9` ·
M8 `4/4` — **M8 done** · M9 `0/9` (decomposed, deferred to last) · M10 `7/9` (reopened)
**Current step:** 10.8 — M10 reopened the day it closed (two real number mismatches found
comparing it against the live web dashboard); M9 stays parked until M10 has nothing left in it.

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
through M8**, verbatim. M10 was archived once too (2026-08-08, all seven steps) and then pulled
back out the same day once two more real gaps turned up — see M10's own section below for why, and
its own preamble for the "reopened" note. On 2026-08-06 the per-step Deviations log that used to
sit at the bottom of
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
and its new landing screen; see `archive/CHECKLIST-DONE.md` for its four steps. **M10 is not done**
— inserted out of phase order the same way M5 was, to close a defect the app's own use uncovered:
comparing 8.4's dashboard against the real Wallos web UI found it didn't show what the web shows.
Its first seven steps closed that gap and briefly closed the milestone, but comparing the *numbers*
against a real logged-in web session (not just card presence) found two more, real ones the same
day — 10.8/10.9, below. **Phase 5 (Management screens, M9) is deliberately parked until M10 has
nothing left in it** — decided with the user 2026-08-08, so dashboard work doesn't hand off to M9
piecemeal one gap at a time.

---

## M9 — Management screens (plan §8, Phase 5)

Goal: full add/edit/delete UI for the four catalog resources (categories, household, payment
methods, currencies), plus a budget editor — everything plan §7.3's "Manage" row and "Profile +
budget" row sketch. **Done when** each of the four has a list screen (reachable from a new
*Manage* drawer group) with add/edit/delete against the live instance, and Settings has a Profile
sub-screen showing the budget and letting it be edited.

Server-side display settings (`get_settings.php`/`set_settings.php`) are **explicitly out of this
milestone** — decided 2026-08-08 with the user: it's an open ~12-field map (color theme, custom
CSS, `week_starts_sunday`, …) that mostly governs the Wallos *web* UI's own rendering, and nothing
in this app has a concrete reason to edit any of it yet. Revisit if one turns up.

Several things are settled here, checked against the live instance (port 8282) and live PHP source
(`docker exec wallos cat api/...`) while scoping this milestone, so no step below has to re-derive
them:

- **Categories, household and payment methods already have full add/edit/delete** — built in M7
  (`CategoriesRepository`/`HouseholdRepository`/`PaymentMethodsRepository`, all on `core:crud`),
  tested, and unused by any screen. This milestone's work on those three is **UI only**: a list
  screen and an add/edit form per resource, the same `SubscriptionEditorRoute(id: Int?)` shape
  7.6/7.7 already established for "one form, nullable id decides add vs. edit."
- **Currencies gets its own module, `feature:currencies`** (dto/domain/data/ui, on `core:crud`),
  rather than extending `feature:subscriptions`. §3.4's original note left this open ("a standalone
  module... can sit on `core:crud`... when it lands") but a stale aside in §7.2 said the opposite
  ("never earns its own module") — both now corrected in `IMPLEMENTATION_PLAN.md`. Decided with the
  user: `feature:subscriptions` keeps its existing trimmed, read-only `Currency` (no `rate`/`inUse`)
  for the price picker and list join, unchanged; the new module gets its own `CurrencyDTO` (with
  `rate`/`inUse` restored) and full CRUD. A small duplication of one DTO, accepted over a
  cross-feature reach into a module that has no business owning currency management.
  `WallosCrudApi.getAll()`'s generic shape drops `get_currencies.php`'s top-level `main_currency`
  field (it only reads `envelope[listKey]`), so **9.1 has a real choice to make**: skip surfacing
  "which currency is main" in the list (delete on it still fails cleanly — see below) or hand-write
  `getAll()` the way `SubscriptionsApi.getCurrencies()` already does or to get `main_currency`
  alongside the list, same as it does for `feature:subscriptions`.
- **Payment methods gets its icon multipart upload now too**, decided with the user: reuse 7.9's
  shape (`MultipartFile`, `postMultipart`, Android's `ActivityResultContracts`) rather than
  deferring again — `PaymentMethodsRepository.addPaymentMethod`/`editPaymentMethod` already carry an
  `iconUrl` param and a doc comment naming this exact gap ("no picker calls it before Phase 5"),
  and 7.9 already paid the one-time cost (the module's first `androidMain` directory, the
  `MultipartFile` carrier type in `core:api`). Confirmed live (`docker exec wallos cat
  api/payment_methods/set_payment_methods.php`): the upload resizes server-side to **70×48**, not
  135×42 like a subscription logo — a different constant, now in `WALLOS_API.md` §3.10.
- **A budget editor is a Settings sub-screen, not a new drawer destination** — decided with the
  user: `SettingsRow` → a new `ProfileRoute`, the same shape `InterfaceRoute`/`AboutRoute` already
  use, not a fifth top-level `DrawerDestination`.
- **The "Manage" drawer group is `DrawerItem.Group`'s first real use.** `WallosDrawerWidget`
  already renders it (a header plus its nested `Destination`s) — built, never exercised, since
  `DrawerItemsBuilder` has only ever emitted flat `Destination`s. Each of the four catalog screens
  is still its own `DrawerDestination` enum case with its own sub-stack, registered in
  `DRAWER_NAV_ITEMS`/`NavKeySerializers` exactly like Dashboard/Subscriptions/Settings — "Manage" is
  purely a `WallosDrawerWidget` grouping, not a different navigation shape.
- **Two delete guards are pre-existing defaults, not in-use rows**, confirmed in the live PHP:
  category id `1` and household member id `1` can never be deleted (`"Cannot delete category"` /
  `"Cannot delete member"`, `docker exec wallos cat api/categories/set_categories.php` /
  `api/household/set_household.php`), the same shape currency's "can't delete the main currency"
  guard already has (`"Cannot delete currency"`). All three already map to `WallosError.InUse` —
  `WallosErrorMapperTest` already covers all three titles — so no new error-handling code is
  needed; an editor screen *may* choose to disable Delete on these proactively for a better message
  than "that item is still in use elsewhere," but it isn't required for correctness. Payment
  methods has no such guard.
- **A real `WALLOS_API.md`/`IMPLEMENTATION_PLAN.md` bug, found and fixed while scoping this
  milestone**: three places (`WALLOS_API.md` §1 and §3.9, `IMPLEMENTATION_PLAN.md` §4.4, and a
  comment plus a test in `WallosEnvelopeParser.kt`) claimed `get_user.php` returns `notes` as an
  empty *string* rather than an array. A live `curl` against `get_user.php` on this instance
  returns `"notes": []` — a real array, same as every other endpoint checked. All four corrected;
  `WallosEnvelopeParser`'s defensive safe-cast is left in place (costs nothing) but its comment no
  longer claims the string case is observed. The source of the original wrong claim isn't known.

- [ ] **9.1 — feature:currencies: dto + domain + data — full CRUD on `core:crud`**
  Mirrors 7.2's shape for `feature:categories` exactly: `CurrencyDTO : CrudResource` (`id`, `name`,
  `symbol`, `code`, `rate`, `inUse`) — `api(projects.core.crud)` in the `dto` module's
  `build.gradle.kts`, per 7.2's own reminder. `CurrenciesApi : CrudApi<CurrencyDTO>` delegating to
  `WallosCrudApi` (`get_currencies.php`/`set_currencies.php`, id param `currencyId`/`id`,
  `docs/WALLOS_API.md` §3.10). Domain `Currency` carries `rate`/`inUse` this time — unlike
  `feature:subscriptions`'s trimmed one, this screen is exactly where both matter.
  `CurrenciesRepository`: `getCurrencies()`, `addCurrency(name, symbol, code, rate)`,
  `editCurrency(id, name, symbol, code, rate)`, `deleteCurrency(id)` — decide here whether `getAll()`
  stays `WallosCrudApi`'s generic form (drops `main_currency`) or a hand-written `getCurrencies()`
  keeps it alongside the list, per this milestone's preamble.
  *Verify:* `./gradlew :feature:currencies:data:testAndroidHostTest` — happy path against
  `MockEngine` fixtures for all four calls, `rate` round-trips as a string on the wire and a
  `Double` in the domain model, and a delete on an in-use or main currency surfaces as
  `WallosError.InUse` unchanged.
  ·  *Ref:* `WALLOS_API.md` §3.10, plan §3.4, this milestone's preamble

- [ ] **9.2 — feature:categories:ui: list + add/edit/delete**
  `CategoriesRoute`/`CategoriesScreen` (list, FAB per 7.6's `FabConfig` precedent) and
  `CategoryEditorRoute(categoryId: Int?)`/`CategoryEditorScreen` (one text field: name). Delete
  behind a confirmation dialog, same shape as 7.7's subscription detail
  (`isDeleteDialogOpen`/`onDeleteClick`/`onDeleteConfirm`/`onDeleteDialogDismiss`). No cache (this
  milestone's whole surface is reference data, per 7.2's precedent) — a plain load-on-open list, a
  refresh after any successful write.
  *Verify:* `./gradlew :feature:categories:ui:testAndroidHostTest`, and on the emulator against the
  live instance — add a category, edit its name, delete it, and confirm deleting the default
  category (id 1) or one still referenced by a subscription surfaces the in-use error rather than
  crashing or silently failing.
  ·  *Ref:* `WALLOS_API.md` §3.10, `CLAUDE.md`'s Screen/Content split, this milestone's preamble

- [ ] **9.3 — feature:household:ui: list + add/edit/delete**
  Same shape as 9.2, two fields (name, optional email) per `HouseholdRepository.addMember`/
  `editMember`. Reuses whatever generic list/editor/delete-dialog composables 9.2 produces if they
  turn out reusable across resources — worth checking before writing a second copy.
  *Verify:* `./gradlew :feature:household:ui:testAndroidHostTest`, and on the emulator — add,
  edit, delete a household member; confirm member id 1 (or one still referenced by a subscription)
  fails with the in-use error.
  ·  *Ref:* `WALLOS_API.md` §3.10, this milestone's preamble

- [ ] **9.4 — feature:paymentmethods:ui: list + add/edit (name, enabled, icon_url) + delete**
  Same shape again: name, an enabled toggle, and `icon_url` as a text field (server-fetched,
  7.8's precedent) — the multipart picker is 9.5, not this step. Delete behind the same
  confirmation dialog.
  *Verify:* `./gradlew :feature:paymentmethods:ui:testAndroidHostTest`, and on the emulator — add a
  payment method with an `icon_url`, see the fetched icon render, edit its enabled state, delete a
  method not referenced by any subscription; confirm one that is referenced fails with the in-use
  error.
  ·  *Ref:* `WALLOS_API.md` §3.10, this milestone's preamble

- [ ] **9.5 — feature:paymentmethods: icon via multipart upload**
  Mirrors 7.9 exactly: an Android image picker (`ActivityResultContracts`, this module's first
  `androidMain`) feeding a multipart `paymenticon` field. Server resizes to 70×48 (confirmed live,
  this milestone's preamble) — different from a subscription logo's 135×42, so don't assume the
  picker's crop/preview aspect ratio without checking.
  *Verify:* on the emulator against the live instance — pick an image for a payment method's icon,
  save, and see it render on the list without restarting the app (7.9's own verify shape).
  ·  *Ref:* `WALLOS_API.md` §3.10, §4; archive `CHECKLIST-DONE.md` 7.9

- [ ] **9.6 — feature:currencies:ui: list + add/edit/delete**
  Fields: name, symbol, code, rate (default `1.0`). Same list/editor/delete-dialog shape as
  9.2–9.4. Decide here whether the list marks the main currency (per 9.1's `getAll()` choice) and
  whether the editor disables Delete on it proactively, or leaves it to the server's
  `"Cannot delete currency"` error (this milestone's preamble covers both are already correct,
  just a UX choice).
  *Verify:* `./gradlew :feature:currencies:ui:testAndroidHostTest`, and on the emulator — add,
  edit, delete a currency; confirm the main currency and any currency still referenced by a
  subscription fail to delete with the in-use error.
  ·  *Ref:* `WALLOS_API.md` §3.10, this milestone's preamble

- [ ] **9.7 — composeApp: the "Manage" drawer group**
  Wires all four new routes into `DrawerDestination`/`DRAWER_NAV_ITEMS`/`NavKeySerializers`
  (miss one and `NavKeySerializersTest` catches it, per `CLAUDE.md`'s nav3 rule) and adds a
  `DrawerItem.Group("Manage", [...])` entry to `DrawerItemsBuilder`, below Settings — the first
  real use of `DrawerItem.Group`, which `WallosDrawerWidget` already renders correctly (this
  milestone's preamble). New entry providers per screen (`nav/entries/`), added to `MainNavHost`.
  *Verify:* on the emulator — open the drawer, confirm a "Manage" header with all four screens
  listed under it, open each one.
  ·  *Ref:* plan §5.4, this milestone's preamble

- [ ] **9.8 — feature:profile: dto + domain + data — `get_user` + `set_budget`**
  New module. `UserDTO` (`WALLOS_API.md` §3.9 — `id`, `username`, `email`, `main_currency`,
  `budget`, `period_budget`, `budget_period_type`, `budget_period_anchor_date`, `totp_enabled`;
  skip `password`/`api_key`, always masked). `ProfileRepository.getUser()` /
  `setBudget(monthlyBudget, periodBudget, periodType, anchorDate)` — **always send all three period
  fields together** when touching any of them (this milestone's preamble / `WALLOS_API.md` §3.8):
  sending `period_budget` alone silently resets type and anchor to `monthly`/today.
  *Verify:* `./gradlew :feature:profile:data:testAndroidHostTest` — `get_user`'s happy path against
  `MockEngine` fixtures, and `set_budget` sending all three period fields whenever any one of them
  changes, never a partial set.
  ·  *Ref:* `WALLOS_API.md` §3.8–3.9, this milestone's preamble

- [ ] **9.9 — feature:profile:ui: a Settings sub-screen for the budget**
  `ProfileRoute`/`ProfileScreen`, reached from a new `SettingsRow` on `SettingsScreen` (this
  milestone's preamble — not a drawer destination). Shows the current budget and period budget,
  editable, saved through 9.8's `setBudget`.
  *Verify:* on the emulator against the live instance — open Settings, tap into Profile, change the
  budget, save, and confirm `get_user`/the Dashboard's period-budget card reflect the new value.
  ·  *Ref:* plan §7.3, this milestone's preamble

---

## M10 — Dashboard: web parity (not in plan §8's phase order)

Goal: close the four gaps `docs/CHECKLIST.md`'s own "To review" filed against the dashboard (8.4)
after comparing it directly to the real Wallos web UI (`/home/gregory/proj/other/Wallos`, confirmed
`v5.4.2`, same as the docker instance) — `index.php` and `includes/stats_calculations.php`, not
`WALLOS_API.md` alone, are the source of truth for what's built here. **Takes priority over M9, and
keeps it as long as this milestone has open steps** (reaffirmed when reopened, below): decided with
the user 2026-08-08 — the dashboard doesn't ship as "done" while it disagrees with the one UI its
own numbers are supposed to describe. **Done when** the mobile dashboard's sections match the web
dashboard's *numbers*, not just its card set, verified card-by-card against the live instance.

**Reopened 2026-08-08, the same day it first closed.** 10.6/10.7's own on-device verification only
checked that each card *rendered*, not that its numbers matched the web's own — the user then asked
directly where Monthly Budget's number came from, having never seen it on the web, and flagged
Your Subscriptions' cost figures as different from the web's too. Logging into the live web UI with
a real session cookie (`curl`, not just reading PHP) and diffing its rendered dashboard against the
app confirmed two real gaps, filed as 10.8/10.9 below. **M9 (Management screens) is deliberately
pushed to last** — decided with the user: dashboard work doesn't hand off to M9 piecemeal one gap at
a time, to avoid tracking two active fronts at once; M9 starts only once this milestone has nothing
left in it.

Settled while scoping this milestone, each checked against the live PHP source rather than assumed:

- **`feature:profile` is pulled forward from M9, built minimal.** Decided with the user: dto +
  domain + data only, `getUser()` alone — enough for `user.budget`, which nothing on the dashboard
  can reach today (`get_period_budget.php`'s own SQL only ever selects `period_budget`,
  `budget_period_type`, `budget_period_anchor_date` — never `budget`; confirmed reading
  `api/subscriptions/get_period_budget.php` live). M9's 9.8 later *adds* `setBudget()` to this same
  repository rather than building a second `get_user.php` caller — the module isn't otherwise
  different from what 9.8 already specified, just built ahead of its number.
- **Everything else needs no new endpoint** — Overdue Renewals, the Upcoming cap, Your
  Subscriptions and Your Savings are all computable from `SubscriptionsRepository`'s existing cache
  plus `MonthlyCost` (already fetched). Confirmed reading `stats_calculations.php`'s own loop
  (lines ~195–262): active count, monthly/yearly cost and inactive count/savings are a single pass
  over the same subscription list this app already caches.
- **Both dashboard queries the web actually runs exclude `cycle = 5` (one-time) entirely** —
  `index.php:76` (Upcoming) and `:85` (Overdue) both carry `AND cycle != 5`. Our
  `UpcomingPaymentsCalculator.resolve()` does not: a *future* one-time subscription
  (`nextPayment >= today`) passes through unchanged regardless of cycle, so today's dashboard can
  show a one-time purchase the web's own dashboard never would. Found while scoping this milestone,
  not previously known.
- **Overdue Renewals is exactly the set `UpcomingPaymentsCalculator` (8.2) already excludes** —
  `index.php:85`'s query (`next_payment < today AND auto_renew = 0 AND inactive = 0 AND cycle !=
  5`, unlimited, no rolling) selects precisely the rows 8.2's own cron precedent
  (`endpoints/cronjobs/updatenextpayment.php`, `WHERE next_payment < :currentDate AND auto_renew =
  1 AND inactive = 0`) already established the server itself never advances. 8.2's calculator
  already computes and discards this exact set; it becomes a second output instead of a dropped
  one. The **roll-forward behavior stays** for auto-renewing past-due rows — that's this app's own
  compensation for a cache the web doesn't have (8.2's own reasoning), not something to remove for
  parity.
- **Upcoming Payments caps at 3** (`index.php:76`, `LIMIT 3`) — a `.take(3)` after sorting, since
  the cache is already local; no query-level limit needed.
- **"Budget" was one card; the web has two, gated differently.** `index.php:259–370` /
  `stats_calculations.php:295–334`:
  - **Monthly Budget** — shown whenever a monthly cost exists (i.e. almost always), and *contains*
    Monthly Cost as one of its rows rather than being a separate card. Its
    `budget`/`budget_used`/`budget_remaining`/`over_budget` sub-rows only appear when
    `user.budget > 0` (this app's own `monthlyBudget - totalCostPerMonth`, min/max-clamped exactly
    as `stats_calculations.php:301–304` does it — mirror that formula, don't re-derive one).
  - **Period Budget** — shown only when the active period is *not* the plain calendar month
    (`stats_calculations.php:290–293`: compares `period_start`/`period_end` against the calendar
    month's own start/end). `PeriodBudgetDTO`/`PeriodBudget` dropped `period_start`/`period_end` in
    8.1 ("8.4's card doesn't need them") — they need restoring for this gate to be computable at
    all. **10.9 found this gate was incomplete** — see below.
- **A known simplification, not yet decided**: the web's `totalSavingsPerMonth` subtracts the
  monthly cost of any inactive row's `replacement_subscription_id` (`stats_calculations.php:242–
  262`) — cancelling A for B nets the saving against what B now costs. `Subscription` (domain)
  dropped `replacementSubscriptionId` on purpose (2.1's trim), though `SubscriptionDTO` already
  carries it. The step that builds Your Savings decides whether to restore the field and mirror the
  offset, or ship the simpler "sum of inactive rows' prices" and say so in its `Note:`.
- **AI Recommendations is out of scope** — reads as a paid/hosted-only feature from its own name
  (`index.php:183–257`, a per-user `ai_recommendations` table with "savings" copy no endpoint in
  `WALLOS_API.md` describes), not a candidate for parity.

Settled while **reopening** this milestone 2026-08-08, root-caused by logging into the live web UI
(`curl` with a session cookie) and diffing its rendered dashboard against the app, not just reading
PHP:

- **`get_monthly_cost.php` and `stats_calculations.php`'s own `$totalCostPerMonth` are two different
  metrics that happen to share the name "monthly cost."** `get_monthly_cost.php` (`MonthlyCost`,
  fetched since 8.1, and what 10.6/10.7 both display) sums every billing *occurrence* landing within
  a named calendar month — a weekly subscription counts 4–5 times, confirmed both in its own PHP
  loop and in the `wallos` MCP tool's own description. The web's Dashboard cards instead read
  `$totalCostPerMonth`, which normalizes each active subscription to a single monthly-equivalent via
  `getPricePerMonth` — exactly what `SubscriptionStatsCalculator.pricePerMonth` already ports, today
  used only for `Your Savings`'s inactive-row sum. Confirmed live: web `€496.63`, mobile `€711.39`,
  same account, same moment. `get_monthly_cost.php`'s own metric isn't wrong, it's `stats.php`/
  `calendar.php`'s "amount due this month" (`grep amountDueThisMonth` — computed in
  `stats_calculations.php` but never rendered by `index.php`), a page this app has no equivalent of;
  not a candidate fix. Filed as **10.8**.
- **Period Budget's gate was missing a third condition.** The web's own gate (`index.php:317`) is
  `$periodDiffersFromCalendarMonth && isset($userData['period_budget']) &&
  $userData['period_budget'] > 0` — three conditions ANDed. `PeriodBudget.isRedundantWithCalendarMonth`
  (10.2) only encodes the first; nothing checks whether the budget amount itself is `> 0`. Confirmed
  live: this account's `period_budget` is `0` (`get_period_budget.php`'s own `notes` field already
  says `"Period budget is set to 0."`), so the web never renders a Period Budget section at all, but
  `get_period_budget.php` still answers `success: true` with a zeroed budget rather than an error, so
  10.6's card shows anyway. **Not** a case for deleting the card outright — a genuinely non-calendar
  period *with* a real period budget set should still show it. Filed as **10.9**.
- **Aside, not filed as a step**: `Your Savings`'s monthly figure also differs from the web
  (`€38.82` web vs. `€102.82` mobile) — but this is the *already-known, already-documented*
  `replacement_subscription_id` simplification from 10.4's own `Note:`, confirmed exactly on this
  account (Vattenfall id 12's replacement, eprimo id 37, costs €64.00/month; €102.82 − €64.00 =
  €38.82, the web's own number). 10.4 already decided and documented this gap; not re-opened here.
  The web's `Your Savings` card also has a "Yearly Savings" row (`€465.84`) mobile has no equivalent
  of — noted in case it's wanted later, not filed as its own step since nobody asked for it.

- [x] **10.1 — feature:profile: dto + domain + data — `getUser()` only**
  New module, minimal by design (this milestone's preamble). `UserDTO` (`WALLOS_API.md` §3.9 —
  `id`, `budget`, `period_budget`, `main_currency`; skip `password`/`api_key`, always masked, and
  anything M9's 9.9 needs that this card doesn't). `ProfileRepository.getUser(): Result<User>`,
  hand-written against `WallosApiClient` like `DashboardRepository` (neither `core:crud` nor a
  cache fit a single-row endpoint with no `add`/`edit`/`delete`).
  *Verify:* `./gradlew :feature:profile:data:testAndroidHostTest` — happy path against `MockEngine`
  fixtures, `budget`/`period_budget` parsed as numbers.
  ·  *Ref:* `WALLOS_API.md` §3.9, this milestone's preamble
  **Note:** `get_user.php` nests the row under a `"user"` key (`{"success":true,"user":{...}}`),
  unlike `get_monthly_cost.php`/`get_period_budget.php`'s flat envelope — confirmed against the
  live PHP shown in this milestone's preamble. Needed a `UserResponse(val user: UserDTO)` wrapper
  in `feature:profile:dto`, the same shape `get_subscription.php`'s own `SubscriptionResponse`
  already uses for its own nested `"subscription"` key; `ProfileApiImpl.getUser()` reads
  `apiClient.post<UserResponse>(...).user`. Domain `User` mirrors `UserDTO`'s four fields
  one-for-one (`id`, `budget`, `periodBudget`, `mainCurrencyId`) since none is dead weight at this
  trim. No `ProfileDomainModule`: `feature:profile:domain` has zero `@Single`-annotated
  definitions, the same as every other feature's `domain` module before 8.1's own
  `DashboardDomainModule` fixed on that fact (`docs/archive/CHECKLIST-DONE.md` 8.1's note) — its
  `build.gradle.kts` carries only `kmp.library`, no `kmp.di`. `feature:profile:data` needed its own
  `implementation(projects.feature.profile.data)` line in `composeApp/build.gradle.kts` and
  `ProfileDataModule::class` in `AppModule`'s `includes` even though nothing calls it yet — 8.1's
  same reminder, repeated here since nothing enforces it structurally.

- [x] **10.2 — feature:dashboard: budget domain rework**
  Restore `period_start`/`period_end` to `PeriodBudgetDTO`/`PeriodBudget` (dropped in 8.1). Add a
  derived `PeriodBudget.isRedundantWithCalendarMonth: Boolean` (or equivalent), computed by
  comparing `periodStart`/`periodEnd` against `today`'s calendar-month bounds — mirror
  `stats_calculations.php:290–293` exactly, don't re-derive the comparison from scratch. New
  `MonthlyBudget` domain model (`amount` from `feature:profile`'s `User.budget`, `used`/
  `remaining`/`overBudget` derived against `MonthlyCost.amount` the same clamped formula
  `stats_calculations.php:301–304` uses), built where `MonthlyCost`/`PeriodBudget` already live —
  a pure calculation, no new endpoint, so no new Koin-scanned class needed for it specifically.
  *Verify:* `./gradlew :feature:dashboard:domain:testAndroidHostTest` — `isRedundantWithCalendarMonth`
  true for a plain monthly period, false for a period anchored elsewhere (matching the live
  instance's own `Jul 18–Aug 17` period, `budget_period_anchor_date: "2026-07-18"`); `MonthlyBudget`
  derivation clamps `remaining` to 0 and only sets `overBudget` when cost exceeds budget, and is
  entirely absent (not zero) when `user.budget` is 0 — mirrors `isset($monthlyBudget) &&
  $monthlyBudget > 0`'s gate, not just its arithmetic.
  ·  *Ref:* `WALLOS_API.md` §3.6, §3.9, this milestone's preamble
  **Note:** `MonthlyBudget` is a plain `data class` with a `from(budget, monthlyCost): MonthlyBudget?`
  factory in its own file (`model/MonthlyBudget.kt`) — no `@Single`/`@Factory`, per the step's own
  "no new Koin-scanned class needed." `isRedundantWithCalendarMonth(today: LocalDate)` on
  `PeriodBudget` takes `today` as a parameter rather than reading a clock, matching
  `UpcomingPaymentsCalculator.calculate`'s existing shape. `PeriodBudgetDTO.periodStart`/
  `periodEnd` are `String` (mapper parses via `DateFormatter`, injected — `feature:dashboard:data`
  needed a new `implementation(projects.utils.formatter.datetime)` line) and required rather than
  nullable: unlike `next_payment`/`start_date`, the API doc gives no "unset" case for a period's own
  bounds, so the mapper throws (via `requireNotNull`, caught by `resultOf` same as `MonthlyCostMapper`'s
  `Malformed` case) rather than silently dropping the period. `used`/`overBudget` on `MonthlyBudget`
  mirror `stats_calculations.php`'s own variable names precisely (`$monthlyBudgetUsed` is a 0–100
  percentage, not an absolute amount) — worth flagging since "used" reads as an amount at first
  glance.

- [x] **10.3 — feature:dashboard:domain: Overdue Renewals + Upcoming Payments capped at 3**
  Extends `UpcomingPaymentsCalculator` (or splits it into a class that returns both lists in one
  pass over the same filtered/sorted sequence — decide here) to also return the past-due,
  non-auto-renewing rows it currently drops, and to exclude `BillingCycle.ONE_TIME` from the
  *upcoming* side unconditionally (this milestone's preamble — today it only excludes a *past-due*
  one-time row, not a future one). Both lists exclude inactive rows and mirror the web's `cycle !=
  5` filter; Overdue Renewals also excludes `nextPayment == null`/unrecognised-`cycle` rows the same
  way Upcoming already does, but is otherwise unlimited (`index.php:85`, no `LIMIT`). Upcoming caps
  at 3 after sorting.
  *Verify:* `./gradlew :feature:dashboard:domain:testAndroidHostTest` — a past-due, non-auto-renewing
  row lands in Overdue, not dropped; a past-due auto-renewing row still rolls forward into Upcoming
  exactly as 8.2 left it; a future one-time subscription is excluded from Upcoming (regression for
  the gap this milestone found); more than 3 eligible upcoming rows yields exactly 3, the 3 soonest.
  ·  *Ref:* `WALLOS_API.md` §3.1, this milestone's preamble
  **Note:** Kept a single `UpcomingPaymentsCalculator` class (the "decide here" this step left open)
  rather than splitting it — `calculate()` now returns a new `UpcomingAndOverdue(upcoming, overdue)`
  in one pass: a private `Eligible(subscription, nextPayment, cycle)` step filters inactive rows,
  null `nextPayment`/`cycle`, and `BillingCycle.ONE_TIME` up front (mirrors `cycle != 5` on *both*
  web queries, not just Upcoming's — the step text only called out excluding one-time from Upcoming,
  but the preamble's own "both dashboard queries... exclude cycle = 5 entirely" covers Overdue too,
  confirmed reading `index.php:76`/`:85` together), then a single `forEach` buckets each row into
  `upcoming` (future as-is, or past-due+auto-renew rolled forward, per 8.2's existing logic) or
  `overdue` (past-due+non-auto-renew, kept at its original date) before each list is sorted
  ascending and only `upcoming` is `.take(3)`. `DashboardHomeData` gained a required
  `overdueRenewals: List<Subscription>` field (no default, since `feature:dashboard:ui`'s
  `DashboardViewModel` already ignores fields it doesn't read yet) — this rippled into all seven
  `DashboardHomeData(...)` construction sites in `DashboardViewModelTest`, same shape 10.2's own
  `periodStart`/`periodEnd` addition already rippled into that file. `feature:dashboard:ui` itself
  is untouched; wiring `overdueRenewals` into `DashboardUiState`/the screen is 10.6.

- [x] **10.4 — feature:dashboard:domain: Your Subscriptions + Your Savings**
  A pure class over the cached subscription list (no new endpoint, this milestone's preamble):
  active count, monthly cost (already fetched — reuse it, don't resum), yearly cost
  (`monthlyCost × 12`, matching `stats_calculations.php`'s own `$totalCostPerYear`); inactive count,
  and a savings figure — decide and document here whether it includes the
  `replacement_subscription_id` offset (this milestone's preamble's open item) or the simpler sum,
  either way with a `Note:` saying which.
  *Verify:* `./gradlew :feature:dashboard:domain:testAndroidHostTest` — counts and both cost figures
  against a small fixed subscription list; savings excludes inactive one-time rows the same way
  the other two calculators do, if that's where the step lands.
  ·  *Ref:* this milestone's preamble
  **Note:** New `SubscriptionStatsCalculator` (`calculator/`) plus `YourSubscriptions`/`YourSavings`
  domain models (`model/`), following `UpcomingPaymentsCalculator`'s own shape — a plain class,
  constructed directly rather than Koin-injected, since `DashboardHomeUseCaseImpl` will build it the
  same way it already builds `UpcomingPaymentsCalculator` (that wiring is 10.5, untouched here).
  Went with the **simpler sum, no `replacement_subscription_id` offset**: `Subscription` (domain)
  doesn't carry that field (2.1's trim) and restoring it just for this one card's edge case (a
  cancelled row replaced by another) outweighs the parity gain — the number is real, just not
  identical to the web's when a replacement exists. `activeCount` mirrors
  `stats_calculations.php`'s own count exactly (excludes `cycle = 5` even though the row is active);
  `inactiveCount` has no such filter, matching the PHP, since a one-time row's `getPricePerMonth`
  already contributes 0 to savings regardless of whether it's counted.

- [x] **10.5 — feature:dashboard:domain: `DashboardHomeUseCase` recomposition**
  Composes 10.1's `ProfileRepository.getUser()` alongside 8.1's two calls and 10.3/10.4's
  calculators into a wider `DashboardHomeData` (independent `Result`s per source, same reasoning
  8.3 already established: a failed `getUser()` must not blank out monthly cost or the subscription
  lists). `feature:dashboard:domain` gains a dependency on `feature:profile:domain` the same shape
  it already has on `feature:subscriptions:domain`.
  *Verify:* `./gradlew :feature:dashboard:domain:testAndroidHostTest` — every source still present;
  a `getUser()` failure leaves the rest of `DashboardHomeData` populated.
  ·  *Ref:* plan §6, this milestone's preamble
  **Note:** `DashboardHomeData` gained `user: Result<User>` plus two *derived* fields —
  `monthlyBudget: MonthlyBudget?` and `subscriptionStats: SubscriptionStats?` (10.4's
  `SubscriptionStatsCalculator`, wired in here as this step's own text anticipated). Both derived
  fields are `null`, not zeroed, whenever `monthlyCost` itself failed — `MonthlyBudget.from` and
  `SubscriptionStatsCalculator.calculate` both need the unwrapped `Double` amount, and a fabricated
  0 would read as "no cost" rather than "unknown," so `monthlyCostAmount?.let { … }` gates both
  computations on that one `Result` rather than each guessing independently; `monthlyBudget`
  additionally needs `user` to have succeeded, since it reads `User.budget`. `DashboardHomeUseCaseImpl`
  gained a third constructor parameter (`profileRepository: ProfileRepository`) and a second
  calculator field (`subscriptionStatsCalculator`), fetched via a third `async` alongside the
  existing two. `KoinGraphTest` confirmed the wider constructor still resolves — `ProfileRepository`
  was already bound by 10.1's `ProfileRepositoryImpl`/`ProfileDataModule`, already in `AppModule`'s
  includes, so no DI wiring changed outside this module.
  Widening `DashboardHomeData`'s required-field list rippled into two test files exactly the way
  10.2/10.3's own field additions already did: `DashboardHomeUseCaseTest` (this module, three new
  cases — full population, a `getUser()` failure, and a `monthlyCost` failure proving both derived
  fields go absent) and `feature:dashboard:ui`'s `DashboardViewModelTest`, whose seven
  `DashboardHomeData(...)` construction sites all needed `user`/`monthlyBudget`/`subscriptionStats`
  added (`null`/a fixed success `User`, since the ViewModel doesn't read any of the three yet —
  that's 10.6/10.7). The `User` ripple needed a new `commonTest`-only
  `implementation(projects.feature.profile.domain)` line in `feature/dashboard/ui/build.gradle.kts`
  — `dashboard:domain`'s own dependency on `profile:domain` is `implementation`, not `api`, so the
  type wasn't visible there transitively (the same rule `CLAUDE.md`'s DI section already documents
  for plain types, paid a new time here in test scope rather than main-source).

- [x] **10.6 — feature:dashboard:ui: Overdue, Upcoming (capped), Monthly Budget, Period Budget**
  Rebuilds the screen's card set: an Overdue Renewals section above Upcoming Payments (only when
  non-empty), Upcoming Payments now genuinely capped, `MonthlyCostCardUiState` folded into a wider
  Monthly Budget card (cost always shown; budget/used/remaining/over-budget rows only when present,
  per 10.2's gate) and `PeriodBudgetCardUiState` hidden (not just its own `isHidden` from
  `UnsupportedEndpoint`, per 8.4) whenever 10.2's `isRedundantWithCalendarMonth` is true.
  *Verify:* `./gradlew :feature:dashboard:ui:testAndroidHostTest`, and on the emulator against the
  live instance — confirm Monthly Budget's Monthly Cost line matches the web's `Hello John` page
  reading side by side, Period Budget shows (this instance's period is `Jul 18–Aug 17`, genuinely
  not a calendar month), and Upcoming Payments shows at most 3 rows even with more than 3
  eligible — check whether an Overdue row exists on this account to actually exercise that section,
  and note in this step if none did.
  ·  *Ref:* this milestone's preamble, `CLAUDE.md`'s Screen/Content split
  **Note:** `MonthlyCostCard.kt` was renamed to `MonthlyBudgetCard.kt` (state renamed
  `MonthlyCostCardUiState` → `MonthlyBudgetCardUiState`, `amount` → `costAmount`); its "Monthly
  cost" row label is now a sub-row of the wider card rather than the card's own title, and the card
  title itself, plus `PeriodBudgetCardUiState`'s, come from two new strings
  (`dashboard_monthly_budget_title`/`dashboard_period_budget_title`) matching the web's own labels
  (`en.php`'s `monthly_budget`/`period_budget`) rather than the placeholder "Budget" both cards
  shared before. `dashboard_budget_remaining`/`dashboard_budget_over` are now reused by both cards
  rather than being period-budget-only. `UpcomingPaymentUiItem`/`UpcomingPaymentRow` are reused
  as-is for Overdue rows (identical shape — id/name/price/next payment — confirmed against
  `index.php:137–180`'s overdue markup), not duplicated into a second type. The redundancy check
  itself moved to the **domain** layer rather than being recomputed in the ViewModel: `today` is
  only available in `DashboardViewModel.load()` via `Clock.System.todayIn(...)`, a real wall-clock
  read, and computing `isRedundantWithCalendarMonth` there would have made `DashboardViewModelTest`
  depend on the actual current date (this environment's real "today" being 2026-08-08, itself
  inside the existing Aug-1–Aug-31 test fixtures' calendar month — the collision was not
  hypothetical, three tests would have started failing the moment this ran). `DashboardHomeData`
  gained a `isPeriodBudgetRedundant: Boolean`, computed once in `DashboardHomeUseCaseImpl` against
  the same deterministic `today` its own tests already fix, mirroring how `monthlyBudget`/
  `subscriptionStats` were already "derived, not fetched" there (10.5). Verified on the emulator
  against the live instance: Monthly Budget showed `€711.39` (matches `get_monthly_cost` exactly)
  with no budget sub-rows, since this account's `budget` is `0`; Period Budget showed and was not
  hidden, period label `Jul 18 - Aug 17` confirming a genuinely non-calendar period; Upcoming
  Payments showed exactly 3 of this account's 28 active subscriptions. **No Overdue row exists on
  this account** — confirmed via `wallos_list_subscriptions`, every one of the 28 active rows has
  `auto_renew: 1`, so none can ever land on the overdue side of 10.3's calculator; the section's
  absence is therefore correct, not unverified.
  **Correction, 2026-08-08 (10.9 filed against this)**: "Period Budget showed and was not hidden"
  above was verified as "renders without crashing," not as "matches the web" — the web never shows
  this section on this account at all (`period_budget` is `0`), so what 10.6 called a pass was a
  narrower check than it read as. See 10.9.

- [x] **10.7 — feature:dashboard:ui: Your Subscriptions + Your Savings**
  Two more cards from 10.4's stats, shown only when their counts are `> 0` — matching
  `index.php:373`/`:411`'s own gates, not shown unconditionally.
  *Verify:* `./gradlew :feature:dashboard:ui:testAndroidHostTest`, and on the emulator — confirm
  the active count and monthly/yearly cost match the web's "Your Subscriptions" card, and that
  Your Savings is absent if this account has no inactive subscriptions (check and note which it
  was).
  ·  *Ref:* this milestone's preamble
  **Note:** `YourSubscriptionsCardUiState`/`YourSavingsCardUiState` and their two widgets
  (`YourSubscriptionsCard.kt`/`YourSavingsCard.kt`) follow `MonthlyBudgetCard`'s own
  Card/Column shape exactly — title, then one `bodyMedium` row per field, each row a single
  combined "label: value" string (new `dashboard_active_subscriptions`/
  `dashboard_monthly_cost_amount`/`dashboard_yearly_cost`/`dashboard_inactive_subscriptions`/
  `dashboard_monthly_savings` strings, `%1$d`/`%1$s` printf style per `CLAUDE.md`). Both cards
  read `currencySymbol` off `data.monthlyCost`'s own `Result` (`getOrNull()?.currencySymbol`,
  falling back to `""`) rather than adding a new source — `SubscriptionStats` carries no currency
  of its own, and the web's own figures are in the same main-currency total `MonthlyCost.amount`
  already is. `DashboardScreen`'s `LazyColumn` appends both cards after Upcoming Payments,
  gated on `activeCount > 0`/`inactiveCount > 0`; this doesn't match the web's own card order
  (Monthly Budget/Period Budget sit *after* Overdue/Upcoming there, `index.php:184-411`) since
  10.6 already fixed this app's order the other way around and reordering existing cards was out
  of this step's scope.
  Verified on the emulator against the live instance: **Your Subscriptions** showed
  "Active subscriptions: 28", "Monthly cost: €711.39" (matching Monthly Budget's own figure),
  "Yearly cost: €8,536.68" (`711.39 × 12`, exact). **Your Savings** was present — this account
  has 7 inactive subscriptions (`wallos_list_subscriptions(state: "inactive")` confirmed the
  count) — "Inactive subscriptions: 7", "Monthly savings: €102.82"; hand-summing each row's
  `getPricePerMonth` (Fiton €2.6658 + Congstar €18 + Disney+ €8.99 + Flo €0.8325 + Praktika AI
  €8.3325 + Vattenfall €59 + Komoot €4.9992) lands on exactly €102.82, confirming
  `SubscriptionStatsCalculator`'s math on real data. One of the seven (Vattenfall, id 12) does
  carry a `replacement_subscription_id` (37) — 10.4's own known simplification (no offset) is
  therefore live on this account's real numbers, not just a hypothetical.
  **Correction, 2026-08-08 (10.8 filed against this)**: "Monthly cost: €711.39 (matching Monthly
  Budget's own figure)" above was true but not the point — that figure is `get_monthly_cost.php`'s
  own metric, matching Monthly Budget only because 10.6 fetched the same wrong source for both
  cards, not because either matches the web. Real web figure on this account: `€496.63`/`€5,959.57`.
  See 10.8.

- [ ] **10.8 — feature:dashboard: active-subscription monthly cost, normalized like the web's — not `get_monthly_cost.php`'s**
  Replace `MonthlyCost.amount` as the source for `MonthlyBudget.from`'s cost figure and
  `YourSubscriptions.monthlyCost`/`.yearlyCost` with a locally computed sum of `pricePerMonth` over
  *active* subscriptions — mirrors the sum `SubscriptionStatsCalculator` already runs over inactive
  rows for `Your Savings`, just applied to the other side, and matches `stats_calculations.php`'s
  own `$totalCostPerMonth` (`inactive == 0`, unconditional on `cycle`, since `getPricePerMonth`
  already returns 0 for `cycle = 5`). This milestone's preamble ("Settled while reopening…") has the
  full root cause and the confirmed live numbers (web `€496.63`/`€5,959.57` vs. mobile's current
  `€711.39`/`€8,536.68`). **Decide here** whether `DashboardRepository.getMonthlyCost()`/
  `get_monthly_cost.php` stays fetched for anything afterward — `MonthlyCost.title` ("August 2026")
  and `.currencySymbol` are still read by the Monthly Budget card's sub-label, so the call may still
  be needed for those even once `.amount` isn't, or that label may be computable locally instead;
  say which in the `Note:`.
  *Verify:* `./gradlew :feature:dashboard:domain:testAndroidHostTest` — the normalized active sum
  matches `stats_calculations.php`'s formula against a fixed subscription list (extend 10.4's own
  fixtures rather than duplicating them), and excludes one-time rows the same way the existing
  savings sum does. On the emulator against the live instance: Monthly Budget's Monthly Cost row and
  Your Subscriptions' Monthly Cost/Yearly Cost rows read `€496.63`/`€5,959.57` — not `€711.39`/
  `€8,536.68` — matching the logged-in web dashboard read side by side.
  ·  *Ref:* `stats_calculations.php:195-224` (`$totalCostPerMonth`), this milestone's preamble

- [ ] **10.9 — feature:dashboard:ui: Period Budget — hide when the account's own period budget is 0**
  Add a `periodBudget.periodBudget <= 0` check to `DashboardViewModel.toPeriodBudgetCardState`,
  alongside the existing `isRedundantWithCalendarMonth`/`UnsupportedEndpoint` gates — mirrors
  `index.php:317`'s third ANDed condition (`$userData['period_budget'] > 0`), which nothing here
  checks today. This milestone's preamble has the full root cause and the confirmed live account
  state (`period_budget: 0`, `get_period_budget.php`'s own `notes` already saying so).
  *Verify:* `./gradlew :feature:dashboard:ui:testAndroidHostTest` — a `PeriodBudget` with
  `periodBudget <= 0` hides the card even when `isRedundantWithCalendarMonth` is false and the fetch
  succeeded (a case 10.6's own tests never covered, since its fixture always used a positive
  `periodBudget`). On the emulator against the live instance, confirm Period Budget no longer
  renders at all — this account's real `period_budget` is `0`.
  ·  *Ref:* `index.php:317`, this milestone's preamble

---

## To review

Written when M2 closed, as the place a verification step files a defect it finds rather than
fixing in place (**3.12** kept to that shape) — renamed from "Still open after v1" once it grew
past that: a park for anything that isn't today's work, whether an agent found it mid-step or the
user found it using the app, to come back to once there's room. Six entries left this list to
become M5, and one — the dashboard-vs-web comparison filed 2026-08-08 — left it to become **M10**;
see `archive/DEVIATIONS.md` for how the first six closed. Two more, filed the same day the user
compared 10.6/10.7's numbers against the real logged-in web dashboard, became M10's own **10.8**
and **10.9** instead of a fresh milestone — M10's own preamble has the root cause for both; see it
there, not here, since it stays with the steps rather than duplicated in this list. Resolved
entries aren't repeated here. Two of what's left are
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

