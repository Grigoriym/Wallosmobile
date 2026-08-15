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
