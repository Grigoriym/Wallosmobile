# WallosMobile — Build Checklist

The executable companion to [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md). The plan holds
the *why*; this file holds the *what next*. Every step is written to be doable in one fresh
context, with no memory of previous sessions.

**Progress:** M0 `7/7` · M1 `11/11` · M2 `7/7` — **v1 done** · M3 `12/12` — **Phase 2b done** ·
M4 `5/5` — **Phase 2c done** · M5 `6/6` — **M5 done** · M6 `2/2` — **M6 done** · M7 `0/9`
**Current step:** 7.1 — core:crud: the shared CRUD contract

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
  **M3 raised the stakes and 3.11 raised them again**: there is now a Room schema in the picture, and
  it is already at version 2, so a released app needs real migrations rather than the destructive
  fallback the pre-v1 rule permits.
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

- [ ] **7.1 — core:crud: the shared CRUD contract**
  `CrudResource` (`id`, `name`, `inUse`) and `CrudApi<T>` (`getAll`/`add`/`edit`/`delete`) per plan
  §3.4, plus whatever the four `set_*.php` endpoints genuinely share: the `action=add|edit|delete`
  form shape, the per-resource ID-parameter alias, and the `"<Resource> in use"` delete failure
  (`WALLOS_API.md` §3.10). No DTOs live here — each feature's own DTO satisfies `CrudResource`
  through its mapper, the same way `feature:subscriptions` DTOs never touch `core:api` directly.
  *Verify:* `./gradlew :core:crud:testAndroidHostTest` — a fake resource type declared in the test
  round-tripped through `MockEngine` for `get`/`add`/`edit`/`delete`, plus the "in use" delete
  failure surfacing as a typed error.  ·  *Ref:* plan §3.4, `WALLOS_API.md` §3.10

- [ ] **7.2 — feature:categories: data + domain + dto + mapper on core:crud**
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

- [ ] **7.3 — feature:household: data + domain + dto + mapper on core:crud**
  Same shape as 7.2, copied — the second instance of the pattern should take a fraction of 7.2's
  time. Adds the optional `email` field. `memberId`/`id` alias.
  *Verify:* `./gradlew :feature:household:data:testAndroidHostTest`
  `:feature:household:mapper:testAndroidHostTest`, same coverage as 7.2.
  ·  *Ref:* `WALLOS_API.md` §3.10

- [ ] **7.4 — feature:paymentmethods: data + domain + dto + mapper on core:crud**
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

- [ ] **7.5 — feature:subscriptions: add / edit / delete on the repository**
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

- [ ] **7.6 — feature:subscriptions:ui: the add/edit form (no logo)**
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

- [ ] **7.7 — feature:subscriptions:ui: edit entry point + delete**
  An edit action on the detail screen opens 7.6's form pre-filled from the subscription the screen
  already has loaded — no extra round trip, the same reasoning 2.5 used to justify the detail
  screen's own re-read. A delete action (confirmation dialog) calls
  `SubscriptionsRepository.delete` and navigates back to the list.
  *Verify:* on the emulator against `wallos-scratch` — edit a field on a scratch row and confirm it
  lands; delete a different row and confirm it leaves the list. Not against `gregorz`'s real 35
  rows.  ·  *Ref:* plan §7.3

- [ ] **7.8 — feature:subscriptions: logo via `logo_url`**
  A text field in the editor for a source URL; the server fetches it server-side (max 3 redirects,
  5 s timeout, SSRF guard — `WALLOS_API.md` §3.4). **Re-read after write to confirm the logo
  landed** (plan §8's own Enforce bullet for this phase) — the `add`/`edit` response carries no
  resolved `logo` filename, so `get_subscription.php` afterward is the only way to know the fetch
  succeeded.
  *Verify:* on the emulator against `wallos-scratch` — set a `logo_url`, submit, and see the
  fetched logo render on the detail screen without restarting the app.
  ·  *Ref:* `WALLOS_API.md` §3.4

- [ ] **7.9 — feature:subscriptions: logo via multipart upload**
  A device image picker (Android's `ActivityResultContracts` — the one platform seam this
  milestone needs, `expect`/`actual` per the no-`androidMain`-in-features rule, the same shape
  1.4's Keystore access and 3.7's trust manager already use) feeding a multipart `logo` field
  alongside the rest of the form (png/jpg/jpeg/gif/webp, resized server-side to 135×42).
  *Verify:* `adb push` a small jpg onto the AVD's gallery path first if it has none, then on the
  emulator against `wallos-scratch` — pick it from the form, submit, and see it render as the logo.
  ·  *Ref:* `WALLOS_API.md` §3.4, §4

