# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `11/11` · M2 `7/7` — **v1 done** · M3 `12/12` — **Phase 2b done** ·
M4 `5/5` — **Phase 2c done** · M5 `6/6` — **M5 done** · M6 `2/2` — **M6 done** · M7 `9/9` ·
M8 `4/4` — **M8 done** · M9 `4/9` · M10 `9/9` — **M10 done**
**Current step:** 9.5 — feature:paymentmethods: icon via multipart upload.

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
through M8, and now M10**, verbatim. M10 was archived once before too (2026-08-08, its first seven
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
`archive/CHECKLIST-DONE.md` for its nine steps. **Phase 5 (Management screens, M9) starts next** —
it was deliberately parked until M10 had nothing left in it, decided with the user 2026-08-08 so
dashboard work didn't hand off to M9 piecemeal one gap at a time; that condition is now met.

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

- [x] **9.1 — feature:currencies: dto + domain + data — full CRUD on `core:crud`**
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
  ·  *Note:* Took the hand-written path, mirroring `SubscriptionsApi.getCurrencies()`: `CurrenciesApi`
  is *not* `CrudApi<CurrencyDTO>` (that interface's `getAll(): List<T>` has no room for
  `main_currency`) — its `getAll()` returns a `CurrenciesPayload` decoded straight into a
  `CurrenciesResponse` DTO (`apiClient.post<CurrenciesResponse>`, no manual `JsonObject`/`JsonArray`
  parsing needed, since the envelope parser already decodes arbitrary shapes). `add`/`edit`/`delete`
  still delegate to a private `WallosCrudApi<CurrencyDTO>` instance held by composition rather than
  interface delegation. Domain `Currency` carries a fourth new field, `isMain` — computed by
  `CurrencyMapper.toDomain(dto, mainCurrencyId)` comparing each row's id against the payload's
  `main_currency` — rather than a separate `mainCurrencyId` sitting beside the list, so 9.6's list
  screen has one thing to render per row instead of two to correlate. `rate.toDoubleOrNull() ?: 1.0`
  in the mapper is a genuine fallback, not one hit by any live data: every row on both the live and
  the scratch instance already carries a plain decimal string (`"1"`, `"1.1000"`), confirmed via the
  `wallos` MCP and `set_currencies.php`'s live PHP source. Wired into `AppModule`/`Koin.kt` and
  `composeApp`'s `build.gradle.kts` now, same as 7.2/7.3/7.4/10.1 all did at their own data-only
  step rather than waiting for a UI step to land.

- [x] **9.2 — feature:categories:ui: list + add/edit/delete**
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
  ·  *Note:* Ran only the host-test half of Verify — the emulator half needs a way to reach this
  screen, and nothing does yet: the FAB is entirely shell-driven (`RouteConfigProvider.getConfig`
  in `composeApp`, unwired until 9.7), and the "Manage" drawer group that would open `CategoriesRoute`
  is also 9.7's own scope, by this milestone's own preamble ("Wires all four new routes..."). So the
  on-device add/edit/delete/in-use-error check for all four catalog screens is deferred to land right
  after 9.7 wires navigation, not skipped. Two other choices worth recording for 9.3–9.6, which
  will hit the same shape:
  (1) **One route pair, not three** — `CategoriesRoute` (list) and `CategoryEditorRoute(categoryId,
  name)` (add/edit/delete together). A category is one field, so there is no separate detail screen
  the way subscriptions has one; edit and delete both live on the editor, gated on `categoryId`.
  (2) **`CategoryEditorRoute` carries the row's `name` alongside `categoryId`**, populated straight
  from the list screen's own `uiState.items` at the tap — `CategoriesRepository` has no
  single-row fetch (only `getCategories()`), so this avoids a second full-list round trip just to
  prefill one field.
  (3) **`CategoriesViewModel` has no `init { load() }`** — deliberately, unlike every other
  no-cache ViewModel so far (`DashboardViewModel`, `SubscriptionDetailViewModel`). This list has to
  reload both on first open and on every return trip from the editor after a write, and Nav3
  disposes a covered entry's composition and restarts it when it comes back on top — so
  `CategoriesScreen`'s own `LaunchedEffect(Unit) { uiState.onRetryClick() }` is the single load path
  for both cases, and an `init` block would just double the first load. Data and DI wiring
  (`CategoriesUiModule`, `AppModule`'s `includes`, `composeApp`'s `build.gradle.kts`) landed now,
  same as 9.1 did for its data module — `KoinGraphTest` already resolves both new ViewModels.
  Nav wiring itself (`NavKeySerializers`, `DrawerDestination`, `RouteConfigProvider`, entry
  providers) stays 9.7's, per its own scope.

- [x] **9.3 — feature:household:ui: list + add/edit/delete**
  Same shape as 9.2, two fields (name, optional email) per `HouseholdRepository.addMember`/
  `editMember`. Reuses whatever generic list/editor/delete-dialog composables 9.2 produces if they
  turn out reusable across resources — worth checking before writing a second copy.
  *Verify:* `./gradlew :feature:household:ui:testAndroidHostTest`, and on the emulator — add,
  edit, delete a household member; confirm member id 1 (or one still referenced by a subscription)
  fails with the in-use error.
  ·  *Ref:* `WALLOS_API.md` §3.10, this milestone's preamble
  ·  *Note:* No reusable list/editor/delete-dialog composables came out of 9.2 to share — its
  `CategoriesScreen`/`CategoryEditorScreen` are plain, one-field-specific Composables with nothing
  factored out, so this step wrote its own `HouseholdScreen`/`HouseholdMemberEditorScreen` following
  the same shape rather than extracting a shared widget for a second, still-small user. Same
  deferral as 9.2's own note: the emulator half of Verify needs a way to reach this screen, and
  nothing does until 9.7 wires the "Manage" drawer group, so only the host-test half ran here — all
  14 tests pass (`HouseholdViewModelTest`, `HouseholdMemberEditorViewModelTest`), `detekt`/
  `ktlintCheck` pass project-wide, and `KoinGraphTest` resolves both new ViewModels after a clean
  `:androidApp:compileGplayDebugKotlin --rerun-tasks`. One field differs from 9.2's shape: `email` is
  optional (`HouseholdMember.email` is a plain, always-present `String` — blank reads as absent, same
  as the domain model's own doc comment), so `HouseholdMemberEditorViewModel.onSaveClick` validates
  only `name`, never `email`. Data/DI wiring (`feature:household:ui`'s `build.gradle.kts`,
  `settings.gradle.kts`, root `kover { }`, `composeApp`'s `build.gradle.kts` and `Koin.kt`) landed
  now, same as 9.1/9.2 did at their own step. Nav wiring itself (`NavKeySerializers`,
  `DrawerDestination`, `RouteConfigProvider`, entry providers) stays 9.7's, per its own scope.

- [x] **9.4 — feature:paymentmethods:ui: list + add/edit (name, enabled, icon_url) + delete**
  Same shape again: name, an enabled toggle, and `icon_url` as a text field (server-fetched,
  7.8's precedent) — the multipart picker is 9.5, not this step. Delete behind the same
  confirmation dialog.
  *Verify:* `./gradlew :feature:paymentmethods:ui:testAndroidHostTest`, and on the emulator — add a
  payment method with an `icon_url`, see the fetched icon render, edit its enabled state, delete a
  method not referenced by any subscription; confirm one that is referenced fails with the in-use
  error.
  ·  *Ref:* `WALLOS_API.md` §3.10, this milestone's preamble
  ·  *Note:* Same deferral as 9.2/9.3's own note: the emulator half of Verify needs the "Manage"
  drawer group, which is 9.7's scope, so only the host-test half ran here — all 15 tests pass
  (`PaymentMethodsViewModelTest`, `PaymentMethodEditorViewModelTest`), `detekt`/`ktlintCheck` pass
  project-wide, and `KoinGraphTest` resolves both new ViewModels (including the `Boolean`
  `@InjectedParam` on `PaymentMethodEditorViewModel` — not on `verify()`'s primitive whitelist, so
  this is the first step to actually exercise that path rather than rely on the whitelist) after a
  clean `:androidApp:compileGplayDebugKotlin --rerun-tasks`.
  One route choice worth recording: unlike `HouseholdMemberEditorRoute`, `PaymentMethodEditorRoute`
  does *not* carry `iconUrl` alongside `name`/`enabled` on the edit path — `PaymentMethod.icon` (the
  list's own field) is a server-*resolved* path, not the source URL a caller submits, and the two
  are never the same string. Leaving the field blank on open is exactly the value that already
  means "leave the icon untouched" per `PaymentMethodsRepository.editPaymentMethod`'s own doc
  comment, so there is nothing to prefill.
  The list row's icon reuses `BaseUrlProvider` (`core:api`) the same way `feature:subscriptions:ui`
  does for a logo, but through its own `toIconUrl` (`icon` is already root-relative, confirmed in
  `WALLOS_API.md` §4 — no `images/uploads/.../` segment to insert, unlike a subscription logo's
  bare filename) and its own small `PaymentMethodIcon` composable — a cross-feature reach into
  `feature:subscriptions:ui` for a private, non-`api` widget isn't a seam this codebase uses.
  `PaymentMethodIcon` skips `SubscriptionLogo`'s `logoRefreshToken` (5.6): that token exists to
  retry a request Coil considers already `Error` without changing its cache key, needed because a
  subscription's `logo` field can go from unreachable to reachable while staying the *same*
  filename (a flaky server); here every reload is a fresh `getPaymentMethods()` call building a
  brand new `PaymentMethodUiItem` list from whatever `icon` the server currently reports, so a
  request that resolves differently is already a different request. Data/DI wiring
  (`feature:paymentmethods:ui`'s `build.gradle.kts`, `settings.gradle.kts`, root `kover { }`,
  `composeApp`'s `build.gradle.kts` and `Koin.kt`) landed now, same as 9.1–9.3 did at their own
  step. Nav wiring itself (`NavKeySerializers`, `DrawerDestination`, `RouteConfigProvider`, entry
  providers) stays 9.7's, per its own scope.

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
steps rather than duplicated in this list. Resolved
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

