# Revisit list

Findings worth fixing that are bigger than "small and isolated" — deferred here instead of fixed
inline, per `docs/CHECKLIST.md`'s M17 instructions. Numbered, not dated; each entry stays until
it's actually done, then gets deleted (git has the history).

Empty as of 2026-08-14 (M26/26.1) — its only entry, a custom `lintChecks` detector unable to see a
dependency module's own source, closed with the `detekt-rules` port; see `docs/CHECKLIST.md`'s
26.1 `Note:` for the account.

## 1. Subscriptions list's FAB overlaps the last row

Found 2026-08-15 during 27.5's device pass (on-screen at both the default font scale and 1.3x —
not a font-scale-specific regression, just where the pass happened to look). `SubscriptionsScreen`'s
`LazyColumn` (`feature/subscriptions/ui/.../list/SubscriptionsScreen.kt`) uses a uniform
`contentPadding = PaddingValues(SCREEN_PADDING)`, and Material3's `Scaffold` (`MainScaffold` in
`composeApp/.../AuthenticatedMainScreen.kt`) does **not** include the `floatingActionButton` in the
`innerPadding` it hands to content — that's normal Material behavior (a FAB is meant to float over
content), but it means the list's last row can render underneath the `+` FAB with no scroll room to
clear it, confirmed with the local instance's 8-row-tall list (last row "Claude Code"'s price cut
off behind the FAB in every screenshot taken this session, scaled or not).

Not a one-line fix: needs a deliberate bottom-clearance value (FAB touch target + Material's usual
16dp margin, not a guessed number) added to the list's `contentPadding`, and a check for whether any
other FAB-bearing screen has the same gap — out of scope for M27 (accessibility), which is why this
wasn't fixed inline during 27.5. Fix by giving `SubscriptionsList`'s `contentPadding` an asymmetric
`bottom` value sized to actually clear the FAB.

## 2. No mobile-side renewal notifications

Raised 2026-08-24 while explaining what the subscription editor's "Notify before renewal" switch
(`SubscriptionEditorScreen.kt`) actually does: it only writes the server's own `notify` /
`notify_days_before` fields (`docs/WALLOS_API.md` §3.12). Delivery is entirely Wallos'
`endpoints/cronjobs/sendnotifications.php` cron job, through whatever channel (email, Discord,
ntfy, …) is configured on the server — confirmed there is **no** notification code anywhere in
this app (`NotificationManager`, `NotificationCompat`, `WorkManager`, `POST_NOTIFICATIONS`: zero
hits). A self-hosted instance with no channel configured server-side gives the toggle no effect
at all, silently.

Worth investigating, not fixing inline: whether this app should grow its **own** local
notification, independent of the server's channels — e.g. a `WorkManager` job that reads the
already-cached `core:storage` rows and posts a reminder for renewals due soon, using
`notify`/`notify_days_before` as the per-subscription opt-in it already is. Needs its own design
pass: `POST_NOTIFICATIONS` runtime permission (Android 13+), a notification channel, a schedule
that doesn't fight the existing refresh cadence, and a decision on whether it duplicates or
replaces the subtitle now on that switch (added the same session, `SubscriptionEditorScreen.kt`'s
`SwitchRow` `subtitle` param) once it exists.

## 3. Only Subscriptions survives offline — Dashboard (and everything else) has no cache

Raised 2026-08-24: offline, the Dashboard's Monthly/Period budget cards show the raw "Couldn't
reach that server…" error instead of a stale banner over old numbers, and the "Showing saved data"
banner (`StaleBanner`, `feature/subscriptions/ui/.../widgets/`) exists only on the subscriptions
list. Both are the same root cause, not two bugs: `feature:subscriptions` is the **only** module
with a Room cache (`grep`-confirmed — `core.storage.db`/`@Dao`/`RoomDatabase` usage is nowhere
under `feature/dashboard`, `feature/paymentmethods`, `feature/household` or `feature/profile`).
`DashboardRepository`'s own docstring says so on purpose ("no cache behind either call... every
call is a round trip", M8) and `DashboardUiState`'s docstring: "No cache behind any of the
sources" — there is no cached row for a failed call to leave standing, so `isLoading` is the only
state that screen has. `StaleBanner`/`isStale`/`isFailed` (plan §7.1) are a real pattern, but they
depend on a DAO `Flow` existing to be stale *over* — Dashboard has none.

Not a small fix: making the banner "app-wide" means giving Dashboard, Payment Methods, Household
and Profile the same `observe*`/`refresh*` + Room cache architecture Subscriptions got in M3 —
schema, DAOs, mappers, `isStale`/`isFailed` derivation, tests, per feature. Worth a real design
pass per feature (is `get_monthly_cost`/`get_period_budget` even meaningful to cache as a stale
snapshot, or would a cached number just be actively misleading once it's a day old — unlike a
subscription row, which doesn't change shape when stale) before deciding which of these four
modules actually gets one, rather than mechanically repeating M3 four times.

## 4. `MoneyFormatter` (and anything built on it) is fixed-format, not locale-aware

Raised 2026-08-24 while looking at `DashboardViewModel.kt:106`'s
`usedPercent = monthlyBudget?.let { "${moneyFormatter.format(it.used, "")}%" }.orEmpty()` — the
literal `%` appended in Kotlin rather than through a string resource. Checked and left alone for
now: `MoneyFormatter`'s own docstring already commits to this on purpose ("The grouping and the
decimal point are fixed... not taken from the device locale... A locale-aware formatter is
possible later... and is not what v1 wants") and the web mirrors it exactly (`index.php:286`,
`:344`: `number_format($…, 2)."%"`, no translation string for the sign either) — so this one
call site isn't a new inconsistency, just the same already-declared decision surfacing again.

Worth a real pass once the app actually ships more than one locale (`strings/…/composeResources/`
has only a `values/` directory right now — no `values-xx` exists yet, so there is nothing to be
inconsistent *with* today): `MoneyFormatter.format` mixes two concerns that a locale-aware version
would need to split — matching the *server's* number shape (thousands/decimal separators, which
must stay fixed regardless of device locale, since the app has to agree with whatever the same
instance renders) versus symbol/percent *placement*, which varies by locale (`"42 %"` vs `"%42"`)
and currently doesn't. The docstring already names the mechanism (`expect`/`actual` over the
platform `NumberFormat`) and the tradeoff (out of reach of a host test) — this entry is only to
make sure it doesn't get re-litigated from scratch, and to catch every fixed-format call site
(not just this one `usedPercent` line) in the same pass rather than one at a time.

## 5. No deep-link readiness plan yet

Found 2026-09-01 while triaging `agentic-grappim/investigations/reference-app-scouting.md`'s
HedvigInsurance findings. Three of that scan's patterns share one trigger — none is worth adopting
today, all three become the right move the moment deep links (or any synthetic/reconstructed back
stack) land:

- **Nav-key marker interfaces** (Hedvig: `TopLevelTabRoot`, `DeepLinkAncestry`, etc.) for letting
  the shell ask "does this screen do X" without importing the feature — WallosMobile's drawer shell
  currently has exactly one cross-cutting need (which items show in the drawer), handled fine by
  plain state; not worth the interface machinery for one property.
- **`navigateUp` reserved for the top-bar back arrow, everything else uses a plain pop** —
  `core/navigation/.../Navigator.kt`'s `goBack()` is WallosMobile's only pop primitive, so there is
  no deep-link-aware/plain-pop split for this discipline to protect yet. Confirmed no deep-link
  code exists anywhere in the repo (grepped while checking this).
- **Deep-link matcher aggregation + pending-deep-link-while-logged-out queue** — the actual
  mechanism a deep-link feature would need; nothing to build until one is planned.

Not fixing inline because there's nothing to fix — no code exists to hold the discipline yet, and
adding the marker interfaces/split/queue speculatively would be exactly the kind of unrequested
"flexibility" `CLAUDE.md`'s Simplicity-first rule warns against. Revisit when a deep-link feature
(or a second tab-parking-style shell need) is actually planned — that session should read
`docs/architecture/navigation-and-di.md` in `/home/gregory/proj/HedvigInsurance` for the worked
mechanism rather than re-deriving it.
