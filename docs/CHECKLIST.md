# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `11/11` · M2 `7/7` — **v1 done** · M3 `12/12` — **Phase 2b done** ·
M4 `5/5` — **Phase 2c done** · M5 `6/6` — **M5 done** · M6 `2/2` — **M6 done** · M7 `8/9`
**Current step:** 7.9 — feature:subscriptions: logo via multipart upload

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
through M6**, verbatim. On 2026-08-06 the per-step Deviations log that used to sit at the bottom of
this file moved to [`archive/DEVIATIONS.md`](./archive/DEVIATIONS.md), **frozen** rather than
carried forward: almost every row already had a permanent home in `IMPLEMENTATION_PLAN.md`
(`now in plan §X`), so the practice of appending one was retired as pure duplication — the fold
into the plan is what future sessions actually read. A step's own `Note:` is still where a
deviation gets written down, same as always.

**M7 decomposes plan §8's Phase 3** (subscription writes, reference-data pickers) the way Phase 2b
became M3. **M5 is done**: the list below filed six defects and all six are now closed, so what
remains is policy and deferred *features* rather than known-wrong behaviour. **M6 is done** too —
the launcher icon and the store flavours, the two things only visible from outside the code.

---

## Still open after v1

Written when M2 closed, as the place a verification step files a defect it finds rather than
fixing in place (**3.12** kept to that shape). Six entries left this list to become M5 — see
`archive/DEVIATIONS.md` and `archive/CHECKLIST-DONE.md` for how each closed; resolved entries
aren't repeated here. What's left is two standing decisions the user owns, plus two pieces of
deferred surface that belong to phases nobody has started yet. **Don't re-open the first two per
step; they have both been settled twice.**

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

---

## M7 — Subscriptions: writes + reference data (plan §8, Phase 3)

Goal: a subscription can be created, edited and deleted from the app, with its logo, category,
payer, currency and payment method chosen from real server data rather than typed free-hand.
**Done when** all three actions round-trip against a live instance and the picker lists are the
account's own categories, household members and payment methods.

Two decisions plan §10 left open for "when Phase 3 starts" are settled by this decomposition
rather than by a step of its own, so no step below has to re-litigate them:

- **Three feature modules on `core:crud`, not one `feature:catalog`.** §3.4 already sketched this
  shape and it matches the granularity TaigaMobileNova uses for the same four resources — plan
  §10 moves this from "Still open" to "Settled" alongside this milestone.
- **Currencies don't get a fourth module.** `feature:subscriptions` already has a full read path
  for currencies — 2.3's `currencySymbol` join and 3.11's `observeCurrencies` cache — so a
  `feature:currencies` module here would duplicate it for no caller. 7.6's currency picker reads
  that existing flow; a standalone module with `add`/`edit` (rate maintenance) is Phase 5's
  management-screen work, and it can sit on `core:crud` like the other three when it lands.

- [x] **7.1 — core:crud: the shared CRUD contract**
  `CrudResource` (`id`, `name`, `inUse`) and `CrudApi<T>` (`getAll`/`add`/`edit`/`delete`) per plan
  §3.4, plus whatever the four `set_*.php` endpoints genuinely share: the `action=add|edit|delete`
  form shape, the per-resource ID-parameter alias, and the `"<Resource> in use"` delete failure
  (`WALLOS_API.md` §3.10). No DTOs live here — each feature's own DTO satisfies `CrudResource`
  through its mapper, the same way `feature:subscriptions` DTOs never touch `core:api` directly.
  *Verify:* `./gradlew :core:crud:testAndroidHostTest` — a fake resource type declared in the test
  round-tripped through `MockEngine` for `get`/`add`/`edit`/`delete`, plus the "in use" delete
  failure surfacing as a typed error.  ·  *Ref:* plan §3.4, `WALLOS_API.md` §3.10
  **Note:** the plan sketch was interfaces only; making `getAll`/`add` actually generic needed one
  more type, `CrudEndpoint` (`getPath`, `setPath`, `listKey`, `idParam`) — the list's wrapper key
  and the id alias differ per resource (`categories` vs `fakeId`-style aliases in the doc), so the
  one implementation, `WallosCrudApi<T>`, decodes to a raw `JsonObject` and pulls both out by name
  rather than needing a per-resource response DTO. The "in use" delete failure needed no crud-side
  code at all: `WallosErrorMapper`'s `title.endsWith(" in use")` branch already covers it for every
  endpoint, crud included. A feature's data module is expected to compose `WallosCrudApi` by
  delegation (`CrudApi<CategoryDTO> by WallosCrudApi(...)`) rather than reimplement the four calls —
  unverified until 7.2 actually does it.

- [x] **7.2 — feature:categories: data + domain + dto + mapper on core:crud**
  The simplest of the four resources — `name` only, plus `order` and `in_use`. `CategoryDTO`,
  domain `Category(id, name, inUse)` (`order` stays out of the domain model — nothing in Phase 3
  reads it; Phase 5's list screen adds it if it needs it, per 2.1's "domain model only what the
  screen renders"). `CategoriesRepository` wraps a `CrudApi<CategoryDTO>` pointed at
  `get_categories.php` / `set_categories.php` (`categoryId`/`id` alias). Category names are
  HTML-escaped on the wire like everything else server-rendered — reuse `HtmlUnescaper`, don't
  write a second one.
  *Verify:* `./gradlew :feature:categories:data:testAndroidHostTest`
  `:feature:categories:mapper:testAndroidHostTest` — get/add/edit/delete against `MockEngine`
  fixtures, and `"Category in use"` mapped to the typed delete-failure from 7.1.
  ·  *Ref:* `WALLOS_API.md` §3.10, plan §3.4
  **Note:** `CategoriesRepository` has no cache behind it — unlike `SubscriptionsRepository`
  (3.4), reference data has no offline requirement in this milestone, so `get`/`add`/`edit`/
  `delete` are plain round trips wrapped in `resultOf`, not `observe*`/`refresh*`. Reusing
  `HtmlUnescaper` means `feature:categories:mapper` takes `implementation(projects.feature.
  subscriptions.mapper)` — the first cross-feature dependency in the repo — and any module that
  constructs a real `CategoryMapper` in tests (`feature:categories:data`'s repository test) needs
  its own `implementation` line on it too, since `categories:mapper` doesn't re-export it as `api`.
  `CategoriesDataModule`/`CategoriesMapperModule` were wired into `AppModule`'s `includes` in this
  step, same as 2.3 did for subscriptions, even though nothing calls `CategoriesRepository` until
  7.6 — `KoinGraphTest`'s `verify()` costs nothing on an unreached definition and a forgotten
  `includes` line is a runtime crash waiting for whichever step first injects it.

- [x] **7.3 — feature:household: data + domain + dto + mapper on core:crud**
  Same shape as 7.2, copied — the second instance of the pattern should take a fraction of 7.2's
  time. Adds the optional `email` field. `memberId`/`id` alias.
  *Verify:* `./gradlew :feature:household:data:testAndroidHostTest`
  `:feature:household:mapper:testAndroidHostTest`, same coverage as 7.2.
  ·  *Ref:* `WALLOS_API.md` §3.10
  **Note:** confirmed `email` against the live PHP (`api/household/set_household.php`) rather than
  trusting the doc's summary alone — both `add` and `edit` run `email` through the same `validate()`
  as `name`, so `HouseholdMemberMapper` unescapes both, not just the name. The response's list key
  is `"household"`, not the plan's illustrative `"members"` — §3.4's `CrudEndpoint` sketch used a
  generic placeholder name, not the real one. Domain model is `HouseholdMember` (not `Household`,
  which would name the collection rather than a row) with methods `getMembers`/`addMember`/
  `editMember`/`deleteMember` on `HouseholdRepository`, matching 7.2's `Category`/`getCategories`
  naming pattern one level down. `composeApp/build.gradle.kts` needed the two new module lines
  (`implementation(projects.feature.household.data/.mapper)`) that 7.2 already added for categories
  — `AppModule`'s `includes` alone doesn't make the classes resolvable, the module needs the
  dependency too; `:androidApp:compileGplayDebugKotlin --rerun-tasks` is what caught the miss.

- [x] **7.4 — feature:paymentmethods: data + domain + dto + mapper on core:crud**
  Same shape again; `enabled` (`1`/`0`) and `icon`. **`icon` is already a full relative path**
  (`images/uploads/icons/paypal.png`), unlike a subscription's bare `logo` filename — its display
  URL is `{base}/{icon}` directly, no prefix to add client-side (`WALLOS_API.md` §4).
  `paymentId`/`id` alias. The write side accepts an `icon_url` fetch (same shape as subscriptions'
  `logo_url`, 7.8) and a `paymenticon` file upload — the upload is **out of scope here**: no picker
  screen calls it until Phase 5's management UI exists, and building it unreached would be the
  untested code CLAUDE.md's simplicity rule rules out.
  *Verify:* `./gradlew :feature:paymentmethods:data:testAndroidHostTest`
  `:feature:paymentmethods:mapper:testAndroidHostTest`, same coverage as 7.2.
  ·  *Ref:* `WALLOS_API.md` §3.10, §4
  **Note:** read the live PHP (`api/payment_methods/{get,set}_payment_methods.php`) rather than
  trusting the doc's summary alone — confirmed `enabled` is a SQLite `INTEGER` (`1`/`0` in the
  JSON, not a boolean), so `PaymentMethodDTO.enabled` is an `Int` and `PaymentMethodMapper` folds
  it to `Boolean` the same way `SubscriptionMapper` folds `inactive` (3.4's `INACTIVE_FALSE`
  pattern, here `ENABLED = 1`). `icon_url` **is** in scope, unlike the file upload: it costs
  nothing but an optional `String?` parameter on `addPaymentMethod`/`editPaymentMethod` — no
  platform code, no picker, so it carries its own host tests same as every other field, whereas
  the multipart upload would need 7.9's unreached `expect`/`actual` image-picker plumbing to even
  compile a test against. `PaymentMethodsRepository`'s methods therefore take one more parameter
  than 7.2/7.3's (`name`, `enabled`, `iconUrl: String? = null`) rather than mirroring either
  precedent exactly.

- [x] **7.5 — feature:subscriptions: add / edit / delete on the repository**
  `SubscriptionsRepository` gains `add(params): Result<Int>`, `edit(id, params): Result<Unit>`,
  `delete(id): Result<Unit>` — the one place in this feature where 3.4's "`observe*`/`refresh*`,
  never `get*`" rule doesn't apply as written, since a write is neither: it mutates the server, and
  the cache catches up by a refresh afterward (`add`/`edit` re-run the existing
  `refreshSubscriptions()` path; `delete` removes the row from Room directly rather than waiting on
  a refetch). New `AddSubscriptionParams`/`EditSubscriptionParams` in `domain`, encoding: `cycle`
  restricted to 1–4 at the type level — no `ONE_TIME`, `WALLOS_API.md` §3.4's server-side rejection
  kept client-side so the error never reaches the wire — dates as strict `YYYY-MM-DD` strings, and
  `"1"`/`"0"` (not `true`/`false`) for `auto_renew`/`notify`/`inactive`. No UI in this step.
  *Verify:* `./gradlew :feature:subscriptions:data:testAndroidHostTest` — add/edit/delete against
  `MockEngine`, the cache updated after each, and a server-rejected write mapped to `WallosError`.
  ·  *Ref:* `WALLOS_API.md` §3.4
  **Note:** read the live `set_subscriptions.php` (`docker exec wallos cat
  api/subscriptions/set_subscriptions.php`) rather than trusting the doc summary alone — it matched
  exactly, including that `add`'s response key is `subscriptionId` while `edit`/`delete` accept
  `id`/`subscriptionId`/`subscription_id` as aliases for the same parameter. "Cycle restricted at
  the type level" became a new `WritableBillingCycle` enum (`DAYS`/`WEEKS`/`MONTHS`/`YEARS`, no
  `ONE_TIME` member at all) rather than a runtime check on `BillingCycle` — 7.6's picker reads this
  enum, not `BillingCycle` filtered by `isWritable`. This feature does **not** route through
  `core:crud`: `set_subscriptions.php` shares the `action=add|edit|delete` shape but has ~18 fields
  against a resource-agnostic `name`, plus response/request id-key asymmetry `CrudEndpoint` doesn't
  model, so `SubscriptionsApi` grew three hand-written methods instead (mirroring `WallosCrudApi`'s
  envelope handling, not reusing it). This needed `feature:subscriptions:data` to add the
  `kmp.serialization` plugin (previously absent — the module had never decoded raw `JsonObject`
  before) to resolve `kotlinx.serialization.json`; not a tripwire path, no `Gate-change:` line.
  `add`/`edit` re-run `refreshSubscriptions()` and propagate its failure like any other step in the
  call; `delete` calls the new `SubscriptionDao.deleteById` directly. Three pre-existing hand-written
  fakes (`core:storage`'s and `feature:subscriptions:ui`'s two `FakeSubscriptionsRepository`s) needed
  the new abstract members added to keep compiling — the UI ones stub to `error("not used by this
  test")` per plan §6.1, since no screen calls them until 7.6/7.7.

- [x] **7.6 — feature:subscriptions:ui: the add/edit form (no logo)**
  New screen + ViewModel: name, price, currency (a picker over the *existing* `observeCurrencies`
  flow — no new module per this milestone's second settled decision), cycle + frequency (a picker
  over `BillingCycle`, excluding one-time), `next_payment`/`start_date`, category/payer/payment
  method (pickers over 7.2–7.4's repositories), notes, url, notify + `notify_days_before`,
  `auto_renew`, `inactive`. Form state carried in `SavedStateHandle` as one JSON string, per 5.2's
  precedent. New route registered in `NavKeySerializers`, reached from a FAB on the subscriptions
  list (1.8 parked `FabConfig` for exactly this). **Watch detekt's `allowedConstructorParameters:
  6`** — this is the first ViewModel taking four repositories at once (subscriptions, categories,
  household, payment methods) plus `SavedStateHandle`; if a sixth lands, split it the way 3.4 split
  `SubscriptionsCache` rather than widening the rule. No logo field and no delete yet — both are
  separate steps so this one stays reviewable on its own.
  *Verify:* on the emulator, against `wallos-scratch` (port 8284) rather than the live instance —
  open the FAB, fill every field with `input keyevent KEYCODE_TAB` between them, submit, and see
  the new subscription in the list.  ·  *Ref:* plan §7.3, `CLAUDE.md`'s SavedStateHandle note
  (5.2), `WALLOS_API.md` §3.4
  **Note:** the ViewModel took exactly the five dependencies the step text itself counted (four
  repositories plus `SavedStateHandle`) — no `subscriptionId`, so this step is add-only; 7.7 is
  what turns it into the add/edit form the title promises, and it will have to decide how a sixth
  dependency lands without tripping the "split rather than widen" rule. Currencies, categories,
  household members and payment methods are each a picker built from a new, reusable
  `EditorPickerUiState` (selected id + options + callback) rather than three loose parameters per
  field — that is also what keeps `PickerField` at four Composable parameters instead of six.
  Pickers use `ExposedDropdownMenuBox`/`ExposedDropdownMenu`, new to this repo (no prior
  dropdown/menu component existed) — the precedent for 7.7's edit form and Phase 5's catalog
  screens. `next_payment`/`start_date` use Material3's `DatePicker`/`DatePickerDialog` rather than
  free text, converting through `kotlin.time.Instant` at UTC (`atStartOfDayIn`/`toLocalDateTime`)
  since `initialSelectedDateMillis` is UTC millis — no `DateFormatter` injection needed, so the
  field just displays the raw `YYYY-MM-DD` it will send. **`Icons.Filled.Add` is in
  `material-icons-core` after all** — CLAUDE.md's "not even `Add`" line (written for 1.8, before
  a FAB existed to need it) doesn't hold for the resolved `1.7.3` artifact
  (`unzip`-and-`grep` on `material-icons-core-1.7.3.jar` shows `AddKt.class`); no
  `material-icons-extended` needed, corrected in `CLAUDE.md`. `FabConfig` (`None`/`Standard`),
  parked as a `RouteConfig` field since 1.8, is now real: `AuthenticatedMainScreen`'s `Scaffold`
  reads `appState.currentRouteConfig.fabConfig` and the FAB is never offline-gated, since
  navigating to the editor is not itself a write — only the form's own Save button is.

- [x] **7.7 — feature:subscriptions:ui: edit entry point + delete**
  An edit action on the detail screen opens 7.6's form pre-filled from the subscription the screen
  already has loaded — no extra round trip, the same reasoning 2.5 used to justify the detail
  screen's own re-read. A delete action (confirmation dialog) calls
  `SubscriptionsRepository.delete` and navigates back to the list.
  *Verify:* on the emulator against `wallos-scratch` — edit a field on a scratch row and confirm it
  lands; delete a different row and confirm it leaves the list. Not against `gregorz`'s real 35
  rows.  ·  *Ref:* plan §7.3
  **Note:** "no extra round trip" needed the domain `Subscription` model widened first — it carried
  `categoryName`/`paymentMethodName`/`payerName` (resolved text) but never the *ids* the editor's
  pickers need to pre-select an option, nor `autoRenew`/`notify`/`notifyDaysBefore`, which 7.6's form
  also has switches for. Added all six (`categoryId`, `paymentMethodId`, `payerUserId`, `autoRenew`,
  `notify`, `notifyDaysBefore`) with no defaults, matching the model's existing style — every other
  call site is a named-argument construction, so the compiler found every one that needed updating.
  This is 2.1's rule ("domain model only what the screen renders") working as designed: the editor
  is now a screen that renders these, so they earned their place. Threaded the same six fields
  through `SubscriptionEntity`/`SubscriptionEntityMapper` (cached, not re-fetched) and bumped
  `WallosDB` to version 3 — pre-v1 destructive fallback, no migration. `SubscriptionEditorRoute`
  went from `data object` to `data class(subscriptionId: Int? = null)`; the editor ViewModel takes
  `subscriptionId` as a sixth `@InjectedParam`/constructor dependency (still within detekt's
  `allowedConstructorParameters: 6`) and pre-fills asynchronously from
  `SubscriptionsRepository.observeSubscription(id).first()` in `init`, guarded by a
  `hadStoredForm` flag read *before* `persistForm()` starts writing — otherwise every construction
  would see a non-null saved form and skip the pre-fill, add or edit alike. An edit resubmits every
  field rather than diffing against `EditSubscriptionParams`' "omitted fields keep their current
  value" semantics, since the form always holds a real value for each one. Delete is a top-bar
  trash icon on the detail screen, gated behind an `AlertDialog` (`CertTrustDialog`'s shape, 3.8);
  only the dialog's own confirm button is offline-gated, matching 7.6's FAB precedent that
  navigating to a write is not itself a write. `Icons.Filled.Edit`/`Delete` are both in
  `material-icons-core` — no `material-icons-extended` needed, checked the same
  `unzip`-the-jar way as 7.6's `Add`.

- [x] **7.8 — feature:subscriptions: logo via `logo_url`**
  A text field in the editor for a source URL; the server fetches it server-side (max 3 redirects,
  5 s timeout, SSRF guard — `WALLOS_API.md` §3.4). **Re-read after write to confirm the logo
  landed** (plan §8's own Enforce bullet for this phase) — the `add`/`edit` response carries no
  resolved `logo` filename, so `get_subscription.php` afterward is the only way to know the fetch
  succeeded.
  *Verify:* on the emulator against `wallos-scratch` — set a `logo_url`, submit, and see the
  fetched logo render on the detail screen without restarting the app.
  ·  *Ref:* `WALLOS_API.md` §3.4
  **Note:** the "re-read after write" concern turned out already satisfied by 7.5's own shape —
  `add`/`edit` both call `refreshSubscriptions()` (the full list, not a single-row `get_subscription`)
  before returning, and that response's `logo` field is already the server-resolved filename, so no
  new re-read logic was needed. `logoUrl: String? = null` added to `AddSubscriptionParams`/
  `EditSubscriptionParams`, forwarded to `logo_url` in `SubscriptionsRepositoryImpl.toFormParams()`
  the same `?.let` way as `notes`/`url`. Confirmed against the live `set_subscriptions.php` PHP
  (`docker exec wallos cat …`) that a blank/omitted `logo_url` on edit leaves the existing logo
  untouched — matches `EditSubscriptionParams`' existing "omitted fields keep their current value"
  contract, so no special-casing needed there either. UI: one more `OutlinedTextField` in
  `SubscriptionEditorScreen`, not pre-filled on edit (the domain model only carries the bare logo
  *filename*, not a re-fetchable URL). Verified both paths live: added a subscription with a
  GitHub-hosted PNG as `logo_url` and watched it render on the list and detail screens after
  `Save` with no restart, then edited it with a second image URL and watched the detail screen's
  logo swap — both via `wallos-scratch`, both confirmed against the `Ktor` REQUEST/RESPONSE log
  (`set_subscriptions.php` → 200, followed by the existing `refreshSubscriptions()` triad).

- [ ] **7.9 — feature:subscriptions: logo via multipart upload**
  A device image picker (Android's `ActivityResultContracts` — the one platform seam this
  milestone needs, `expect`/`actual` per the no-`androidMain`-in-features rule, the same shape
  1.4's Keystore access and 3.7's trust manager already use) feeding a multipart `logo` field
  alongside the rest of the form (png/jpg/jpeg/gif/webp, resized server-side to 135×42).
  *Verify:* `adb push` a small jpg onto the AVD's gallery path first if it has none, then on the
  emulator against `wallos-scratch` — pick it from the form, submit, and see it render as the logo.
  ·  *Ref:* `WALLOS_API.md` §3.4, §4

