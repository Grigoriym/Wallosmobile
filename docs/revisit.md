# Revisit list

Findings worth fixing that are bigger than "small and isolated" — deferred here instead of fixed
inline, per `docs/CHECKLIST.md`'s M17 instructions. Numbered, not dated; each entry stays until
it's actually done, then gets deleted (git has the history).

Empty as of 2026-08-12 — its one entry (`androidLibrary { }` → `android { }`, filed during the
9.7.0 wrapper bump) closed the same day: confirmed a pure rename (the deprecated interface carries
`ReplaceWith("android")` and is identical to the new one), applied to both modules, and verified on
device — all 16 `:core:storage` Room DAO tests and all 8 `:feature:subscriptions:ui` Compose UI
tests pass on `Medium_Phone_API_36.1`.
