# Dashboard shows zeros/empty right after login

**Status:** Done
**Link:** none (reported directly by the user)   **Updated:** 2026-08-24

## Report

User-reported, verbatim: "when logging in, the data in the dashboard page shows nothing/zeros,
only if you close the app and open it again the dashboard will show the data."

- **Symptom:** immediately after a successful login, the Dashboard screen (the app's default
  start destination) renders with zero/empty values. Closing and relaunching the app makes the
  Dashboard show correct data.
- **Environment:** not stated — which login path (API-key entry vs. username/password bridge),
  build flavor, whether this is a first-ever login or a re-login on a device that had data before.
- **Reporter's diagnosis:** none given — only the symptom and the workaround they found.

Open question the report doesn't answer, and that matters for confirming the mechanism below:
whether, between "dashboard shows zeros" and "close/reopen fixes it", the user visited any other
tab (Subscriptions in particular) before closing the app. See Root cause.

## Findings

- `DashboardHomeUseCase.getDashboardHomeData()` (`feature/dashboard/domain/.../usecase/DashboardHomeUseCase.kt:47`)
  builds the dashboard from **one snapshot** of the subscriptions cache:
  `subscriptionsRepository.observeSubscriptions().first()`. Its own doc comment (lines 17-22)
  states the assumption explicitly: *"a single current snapshot, not a re-fetch, since nothing on
  this screen writes and the cache is refreshed elsewhere."* Nothing in this use case, or in
  `DashboardViewModel` (`feature/dashboard/ui/.../DashboardViewModel.kt:41-43`), ever calls
  `SubscriptionsRepository.refreshSubscriptions()`.
- `SubscriptionsRepository` (`feature/subscriptions/domain/.../repo/SubscriptionsRepository.kt:13-24`)
  is explicitly offline-first: *"the cache is the only thing anyone reads, and the network only
  ever writes to it... Empty until one succeeds."* `observeSubscriptions()` returns rows from Room,
  never triggers a network call itself.
- The **only** call site of `refreshSubscriptions()` in the app is
  `SubscriptionsViewModel.load()` (`feature/subscriptions/ui/.../list/SubscriptionsViewModel.kt:169-183`),
  run from that ViewModel's `init` (line 92-95). Confirmed by grep — no other caller exists
  (`grep -rn "refreshSubscriptions"` across the repo, excluding tests, matches only the interface
  declaration, the impl, and this one call site).
- No code path refreshes the subscriptions cache at login or at app start. Confirmed by reading
  `SetupRepositoryImpl` (`feature/setup/data/.../SetupRepositoryImpl.kt`, the only place
  `apiKeyStorage.setKey`/`.clear()` are called for login) and `WallosApp.kt`
  (`androidApp/.../WallosApp.kt`) — neither references `SubscriptionsRepository`.
- `StartDestination.default()` is `Dashboard` (`core/storage/.../startdestination/StartDestination.kt:27`),
  so Dashboard is the first screen shown after `ApiKeyStorage.isConnected` flips to `true`
  post-login (`WallosAppContent.kt:91-105`), unless the user has changed their start destination
  in Settings.
- `ApiKeyStorage.clear()` — called by both login paths before validating a new key
  (`ApiKeyStorage.kt:22-30`, confirmed at `SetupRepositoryImpl.kt:66` and `:128`) — drops
  "the key **and everything cached under it**", i.e. it wipes the Room subscriptions cache too.
  This applies even to a *re*-login with the same account: any subscriptions data left over from
  a previous session is deliberately erased at that point.
- Consequence for what actually renders as "zero" on first login: of `DashboardHomeUseCase`'s
  fields, `monthlyCost` and `periodBudget` (`DashboardRepository`, direct network calls,
  `DashboardHomeUseCase.kt:44-45`) do **not** depend on the subscriptions cache and should render
  real values even on the very first Dashboard load. `subscriptionStats`, `overdueRenewals`,
  `upcomingPayments`, and the `monthlyBudget` card's "active monthly cost"/used-percent (which is
  derived from `subscriptionStats.yourSubscriptions.monthlyCost`, `DashboardViewModel.kt:56-60`)
  all derive from the empty cache snapshot and would read as zero/empty. This is **inference**,
  not confirmed on-device — worth checking against what the reporter actually saw (literally
  everything blank, or specifically the subscription-derived cards) since it discriminates between
  this cause and a second, independent one.

## Root cause

`DashboardHomeUseCase` reads the subscriptions Room cache as a single snapshot and never refreshes
it; the only thing in the app that ever calls `SubscriptionsRepository.refreshSubscriptions()` is
`SubscriptionsViewModel`, scoped to the Subscriptions tab. Since login (`ApiKeyStorage.clear()` +
`setKey()`) wipes that cache, and `Dashboard` is the app's default start destination, the very
first Dashboard render after any login sees a cache nothing has populated yet — hence
zero/empty subscription-derived cards.

"Close and reopen the app" is not, by the evidence above, actually what fixes it — no code runs a
refresh at app start either. The far more likely mechanism: the user visits the Subscriptions tab
at some point between the first blank Dashboard view and closing the app (even briefly, e.g. via
the drawer) — that visit runs `SubscriptionsViewModel.init` → `refreshSubscriptions()`, which
writes real rows into Room. Room's cache is durable across process death, so the *next* time
Dashboard's `DashboardViewModel.init { load() }` runs (on reopen), it reads a now-populated cache
and looks correct. The reopen coincides with the fix but isn't its cause.

This matches the use case's own doc comment (`DashboardHomeUseCase.kt:21`: *"the cache is
refreshed elsewhere"*) — that assumption is true once the app has been used for a while, and false
for the very first screen a freshly-logged-in user sees.

## Impact

Every login (first-ever or re-login) lands on a Dashboard that under-reports or shows zero for
subscription counts, monthly/yearly cost, savings, upcoming payments and overdue renewals, until
the user happens to visit the Subscriptions tab at least once. This is the default, out-of-the-box
first impression of the app for a new user — high visibility, no crash, no error state (the cards
render successfully with "0" rather than showing a loading/error affordance, so nothing hints at
what's wrong). Workaround exists (visit Subscriptions, or the reporter's own "restart the app"
which only works if a Subscriptions visit happened before the restart) but is not obvious to a user
who doesn't know the cache-refresh model.

## Open questions

- ~~Does the reporter's "zeros" mean literally the whole screen, or specifically the
  subscription-derived cards~~ — **Answered by the user 2026-08-24**: monthly budget card shows 0,
  and the upcoming-payments section shows "no upcoming payments". Both are subscription-cache-
  derived fields (`monthlyBudget`'s active cost comes from `subscriptionStats`; the upcoming list
  comes straight from the same empty snapshot) — matches the root cause exactly, no second cause
  indicated.
- Does the same gap affect a fresh, never-onboarded install (no prior cache at all) the same way
  as a re-login on a device with stale cached data, or does `clear()`'s eviction make any
  practical difference between the two? (Both end up with an empty cache either way — likely no
  difference, but not confirmed. Not blocking the fix.)

## Options

1. **Have `DashboardViewModel`/`DashboardHomeUseCase` refresh the subscriptions cache itself**
   before reading it (call `refreshSubscriptions()`, or a new `observeSubscriptions()` +
   `refreshSubscriptions()` pair, ahead of/alongside the snapshot read) — the same offline-first
   shape `SubscriptionsViewModel` already uses (`observeCache()` combined with a `load()` that
   refreshes).
   - Pros: fixes the root cause directly; Dashboard becomes correct on first render regardless of
     which tab the user visited first; consistent with the "cache is only ever written by a
     refresh, read by observe*" contract stated in `SubscriptionsRepository`'s own doc comment.
   - Cons: changes the documented contract in `DashboardHomeUseCase.kt:17-22`/`DashboardViewModel.kt:26-29`
     — that comment and the class's whole shape (`load()` not `observe*`/`refresh*`) would need to
     be corrected, not just the code; adds a second place in the app that triggers subscriptions
     refresh, so it's worth deciding whether Dashboard's use case owns that call or whether
     `DashboardViewModel` does, to keep the "use case only when a screen needs multiple calls"
     convention in `CLAUDE.md` from splitting the responsibility oddly. Slightly slower dashboard
     open on a fresh cache since it now waits on a real subscriptions round trip rather than an
     instant (empty) snapshot — matches what `SubscriptionsViewModel` already accepts.
   - Blast radius: `feature:dashboard:domain`, `feature:dashboard:ui`, their tests
     (`DashboardHomeUseCaseTest`, `DashboardViewModelTest`) — both currently assume a snapshot read
     and would need fakes/assertions updated to also expect a refresh call.

2. **Refresh subscriptions once, centrally, on login** (e.g. from `SetupRepositoryImpl` after
   `setKey()` succeeds, or from wherever `isConnected` flips to `true`) rather than from Dashboard.
   - Pros: fixes it for every screen that reads the cache on first render, not just Dashboard;
     keeps Dashboard's own "single snapshot, no refresh" shape/doc comment as-is and true.
   - Cons: `SetupRepositoryImpl` (`feature:setup:data`) would need a new dependency on
     `SubscriptionsRepository` (`feature:subscriptions:domain`) it doesn't have today — a
     cross-feature reach that doesn't obviously belong to "setup"; a fire-and-forget refresh
     started during/after login also races the login flow's own navigation to Dashboard, so it
     could still lose the race on a slow network and reproduce the exact same symptom, just
     less often — doesn't provably close the gap the way option 1 does.
   - Blast radius: `feature:subscriptions:domain`'s module boundary comment in `CLAUDE.md`
     ("`core:storage` never depends on any `feature:*:domain`") isn't touched, but this does add a
     new inter-feature dependency (`setup` → `subscriptions`) that doesn't exist today.

3. **Do nothing / leave as a known first-run quirk.**
   - Pros: zero risk, zero code change.
   - Cons: leaves a real, high-visibility first-impression bug for every new user; the reporter
     already found it without any special effort.

**Recommendation: Option 1.** It fixes the bug at the layer that owns the wrong assumption
(`DashboardHomeUseCase`'s doc comment already says what it *should* be true, just isn't enforced),
doesn't add a cross-feature dependency, and doesn't have option 2's race. The doc-comment/shape
change is a feature of the fix, not a cost — the current comment is describing behavior that this
investigation shows is false the moment login is fresh.

## Decision

**Option 1**, chosen by the user 2026-08-24: "I want it to work like on the web, so, dashboard
should have its own refresh."

## What landed

`DashboardHomeUseCaseImpl.getDashboardHomeData()` (`feature/dashboard/domain/.../usecase/DashboardHomeUseCase.kt`)
now launches `subscriptionsRepository.refreshSubscriptions()` as a fourth concurrent `async`
alongside `monthlyCost`/`periodBudget`/`user`, awaits it before reading
`observeSubscriptions().first()`, and logs a failure at `WARN` (never swallowed silently) without
otherwise affecting the rest of the dashboard — a failed refresh leaves the cache exactly as it
was, same offline-first contract as `SubscriptionsRepository` documents everywhere else. Doc
comments on the use case (interface + impl) and on `DashboardViewModel` were corrected to no
longer claim "nothing refreshes this cache" / "single snapshot, not a re-fetch."

Tests: two new cases in `DashboardHomeUseCaseTest.kt` —
`subscriptions are refreshed before the dashboard reads them` (cache starts empty, only populated
via a fake `subscriptionsAfterRefresh`; asserts `refreshCallCount == 1` and that the row is
visible) and `a subscriptions refresh failure still leaves the rest of the dashboard populated`
(refresh fails, asserts monthly cost/upcoming payments from the pre-existing cache still render).
`FakeSubscriptionsRepository` was changed from a static `flowOf(subscriptions)` to a
`MutableStateFlow` cache that `refreshSubscriptions()` actually mutates, so the fake can
distinguish "before refresh" from "after refresh" — this is what makes the first new test capable
of failing before the fix. Confirmed it does: temporarily reverted the use case change alone and
reran the suite — the new refresh test failed (`AssertionError`), the other 8 passed unchanged;
restored the fix, full suite green again (9/9).

Verified on-device (`Medium_Phone_API_36.1`, `installGplayDebug -PgplayBuild`): `pm clear` for a
guaranteed empty cache, fresh username/password login against `http://10.0.2.2:8282`
(`docs/local-info.txt`), and the Dashboard's very first render showed real data — €496.63 monthly
cost, three real upcoming payments (Vodafone/1&1 Telekom/Lightroom Photo Editor), 28 active
subscriptions, €102.82 monthly savings — with no visit to the Subscriptions tab first. No crash,
no `DashboardHomeUseCaseImpl` WARN log (refresh succeeded). This is the exact repro conditions the
original report described.

Deliberately left out (noted, not fixed): no new UI error affordance for a subscriptions-refresh
failure specifically — it degrades the same way `monthlyCost`/`periodBudget` failures already do
on this screen (silently reads whatever's cached, no banner), which is a pre-existing gap in
Dashboard's error handling, not something this fix's scope covers.
