# 2026-08-10 — The Login screen doesn't scroll on a real device

**Status:** Done — root cause confirmed, fix applied and self-verified by the user on the
reporting device
**Link:** none (internal; user-filed, tracked in `docs/CHECKLIST.md`'s "To review" backlog)
**Updated:** 2026-08-10

## Report

The user, on their own connected device, found that focusing a field (e.g. username) with the
keyboard open left password and everything below it unreachable — the screen would not scroll to
bring the rest of the form into view.

**What the report didn't say, initially:** which device. This mattered — see Findings.

## Findings

1. **The checklist's pre-filed lead — `Arrangement.spacedBy(FIELD_SPACING,
   Alignment.CenterVertically)` on a `Modifier.verticalScroll(...)` `Column`
   (`LoginScreen.kt:107-114`) breaking the scroll range — did not reproduce.** Tested on
   `Medium_Phone_API_36.1` (Android 16 / API 36), portrait: the TOTP screen (the heaviest content
   state — cleartext warning, TOTP message, field, hint, Connect, API-key link) never overflowed
   the viewport even with the keyboard open, so nothing was there to fail to scroll. Forced
   genuine overflow by rotating to landscape (drastically less vertical space): content did
   overflow, and a manual swipe **did** scroll correctly, revealing the Connect button and the
   "I have an API key" link both with and without the keyboard open. The `CenterVertically`
   hypothesis is refuted on this emulator/API level — scrolling itself works.
2. **The real repro needed the user's own physical device.** `SM-A920F` (Samsung), Android 10 /
   API 29, 1080×2220. There, tapping a field to open the keyboard didn't just push unreached
   content below the fold — the *entire* content area rendered blank/black behind the keyboard,
   a strictly worse failure than anything seen on the API 36 emulator.
3. **Root cause, found by the user directly:** `androidApp/src/main/AndroidManifest.xml`'s
   `<activity>` declared no `android:windowSoftInputMode` at all. Compose's `imePadding()`
   modifier (used here at `LoginScreen.kt:111`, and also in the authenticated shell at
   `AuthenticatedMainScreen.kt:90`) depends on the window receiving correct IME
   `WindowInsets` — without `adjustResize`, the activity window doesn't resize when the IME
   appears, and on API 29 the insets Compose receives were wrong/absent, so `imePadding()` had
   nothing correct to push content up against. The API 36 emulator's own automatic edge-to-edge
   insets handling appears to mask this gap — Android 16 dispatched usable IME insets even
   without the manifest flag, which is exactly why this didn't reproduce there.

## Root cause

Missing `android:windowSoftInputMode="adjustResize"` on `MainActivity` in
`androidApp/src/main/AndroidManifest.xml`. Every screen using `imePadding()` (currently: Login,
and the authenticated shell's `AuthenticatedMainScreen.kt`) was exposed on any API level whose
insets dispatch doesn't cover the gap — confirmed broken on Android 10/API 29, confirmed *not*
reproducible on Android 16/API 36.

## Impact

- On an affected API level, the Login screen was unusable with the keyboard open — every field
  below the focused one was unreachable, which can block onboarding entirely for a user on such
  a device. Not scoped to Login alone: the same missing flag could affect any other screen using
  `imePadding()` under the authenticated shell, on the same class of device.
- No data-loss or correctness impact — a rendering/input-reachability bug only.
- No workaround from inside the app; a user on an affected device could not complete login at
  all without rotating (if that happens to bring enough of the form into a reachable position)
  or some other incidental escape.

## Open questions

- **The exact API-level boundary is not established** — confirmed broken at API 29, confirmed
  not reproducing at API 36. Nothing here pins down which API level(s) in between are affected;
  `adjustResize` fixes all of them regardless, so this wasn't chased further.
- **Whether the authenticated shell (`AuthenticatedMainScreen.kt`'s `imePadding()` use) was
  independently exercised on the affected device** — not tested this session; the fix is a
  single Activity-level manifest flag, so it covers that screen too, but no on-device screenshot
  confirms it.

## Options

Not applicable — the user found and applied the fix directly (a single, standard, well-known
manifest attribute) before this doc's Options step was reached.

## Decision

Fix applied by the user, 2026-08-10: add `android:windowSoftInputMode="adjustResize"` to
`MainActivity` in `androidApp/src/main/AndroidManifest.xml`. Confirmed working by the user on the
reporting device (`SM-A920F`, Android 10). Not independently re-verified on the API 36 emulator
in this session (the failure never reproduced there to begin with).

## What landed

```diff
         <activity
             android:exported="true"
-            android:name=".MainActivity">
+            android:name=".MainActivity"
+            android:windowSoftInputMode="adjustResize">
```

One-line manifest fix, applies to the whole app (Activity-level), not just Login. Verified by the
user directly on the device that reproduced the bug. No test coverage added — this is a
platform/manifest configuration fix with no unit-testable surface in `commonMain`.
