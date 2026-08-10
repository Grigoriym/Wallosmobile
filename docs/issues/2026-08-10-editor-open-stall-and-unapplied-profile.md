# 2026-08-10 — The add-subscription editor still stalls one frame ~150ms, and M13's own profile was never actually installed

**Status:** Done — Options 2 and 3 landed and verified; Option 4 (the remaining first-touch
class-load cost) left open, unscoped
**Link:** none (internal; user-driven investigation, not from `docs/CHECKLIST.md`'s "To review")
**Updated:** 2026-08-10

## Report

The user, testing on a debug build (`gplayDebug`), found that commenting out
`SubscriptionEditorContent` (`SubscriptionEditorScreen.kt:127`) entirely made FAB→editor
navigation feel instant, while the full screen still felt slow to open — despite M13
(`docs/archive/CHECKLIST-DONE.md`) and its same-day addendum having measured this exact
complaint as fixed (FAB→editor 245ms/250ms vs. list→detail 242ms/250ms, `dumpsys
gfxinfo`/`framestats`, two runs, `gplayNonMinifiedRelease`).

**What the report doesn't say:** which build variant the user tested on. Debug builds never
carry a baseline profile at all — `baseline-prof.txt` lives only in
`androidApp/src/gplayRelease/generated/baselineProfiles/`, a `gplayRelease`-scoped Gradle
source set — so a debug-build finding alone wouldn't have contradicted M13, which measured
release. The user confirmed debug when asked. That still left two live possibilities: the
delay is purely the known, permanent debug-build JIT-cold tax (nothing to chase), or something
in `SubscriptionEditorContent` costs real time even on the release build M13 measured. Only the
second turned out to be true, and a third, unrelated thing turned up underneath it.

## Findings

All measurements below: `Medium_Phone_API_36.1` AVD, `-gpu swiftshader_indirect`,
`gplayNonMinifiedRelease`, cold process each run (`am force-stop` → `am start` → navigate to
Subscriptions → `dumpsys gfxinfo reset` → tap FAB → `dumpsys gfxinfo framestats`), same
methodology M13/13.2 and its addendum used. Full framestats captures and the Perfetto trace are
not committed (verification artifacts, not source) — the commands to reproduce are in this
doc's Options section if needed again.

1. **The debug-only theory is false — the release build stalls too, reproducibly.** Two clean
   runs on `gplayNonMinifiedRelease` with the current, unmodified `SubscriptionEditorContent`:
   worst single frame **150ms both runs** (7 frames each, 57%/29% jank). Commenting out the
   entire `SubscriptionEditorContent(...)` call (`SubscriptionEditorScreen.kt:127`) dropped the
   worst frame to **31ms** — a 5x reduction, on the exact build variant M13's own "fixed"
   measurement used.
2. **The picker fields (`ExposedDropdownMenuBox`-based) are not the dominant cost.**
   Commenting out only the five picker calls (`PickerField`×4, `CyclePickerField`×1 — currency,
   cycle, category, payer, payment method) while leaving every `OutlinedTextField`, both
   `DateField`s, `LogoFilePicker` and the three `SwitchRow`s in place only moved the worst frame
   from 150ms to **129ms** — a 14% drop, not the dominant share the initial hypothesis expected.
   What's left after removing pickers is still enough to stall a frame ~130ms on its own.
3. **A Perfetto trace of the full-content tap (`sched freq idle am wm gfx view input dalvik hal
   res memory binder_driver`, `emulator-testing` skill Step 4b) attributes the stall.** The
   113.85ms `Choreographer#doFrame` covering the tap splits into an **87.58ms "animation"
   phase** (Compose composition) and a **26.26ms "traversal" phase** (View measure/layout/draw,
   `Record View#draw()` ≈26ms of it). Inside the animation phase: **~600 ART class-load
   slices** (trace category `dalvik`, one per class descriptor, e.g.
   `Landroidx/compose/foundation/text/selection/TextFieldSelectionManager;`,
   `.../UndoManager;`, `.../EditingBuffer;`, `.../BringIntoViewRequesterImpl;`,
   `.../KeyboardActionRunner;`, `AndroidLegacyPlatformTextInputServiceAdapter;`), almost
   entirely `androidx.compose.foundation.text`/`material3` text-field internals, plus **32
   `Lock contention` slices** (25 on `ClassLinker classes lock`, 4 on `InternTable lock`) where
   the main thread's class-loading blocked briefly on other threads — concretely, the three
   background coroutines `loadCategories`/`loadPayers`/`loadPaymentMethods`
   (`SubscriptionEditorViewModel`) loading their own DTO/serializer classes
   (`CategoryDTO`, `HouseholdMemberDTO`, `PaymentMethodDTO` + `$$serializer`) concurrently,
   holding the same global ART lock the main thread needs. The individually-named slices only
   sum to ~16.5ms of the 87.58ms animation phase (13.88ms class-load + 2.65ms lock
   contention) — the remaining ~70ms is real CPU time with no trace marker of its own, since
   this project has no Compose composition-tracing dependency wired in (confirmed: no
   `androidx.compose.runtime.tracing`/`traceMarkers` config anywhere in `build-logic` or
   `gradle/libs.versions.toml`), so it can't be attributed more finely than "inside Compose's
   own composition machinery" without adding that.
4. **This is very likely the first screen in the whole navigation path that uses
   `OutlinedTextField` at all.** Dashboard and the Subscriptions list use no text-entry fields
   (`Filter` is not a text field). If true, the ~600-class stall is a genuine one-time cost of
   Compose's text-editing machinery being touched for the first time in this process, not
   something specific to this screen's own code — inference, not directly verified this
   session (would need a trace of every prior screen in the path to confirm no earlier
   `OutlinedTextField` use).
5. **A separate, more foundational gap: the baseline profile was never actually applied to any
   build this project has measured.** `adb shell dumpsys package com.grappim.wallosmobile`
   showed `[status=verify] [reason=install]` on the just-installed `gplayNonMinifiedRelease`
   APK — ART's install-time dexopt used bytecode *verification only*, never the bundled
   `assets/dexopt/baseline.prof`. Forcing it —
   `adb shell cmd package compile -m speed-profile -f com.grappim.wallosmobile` (confirmed via
   the same `dumpsys package` command flipping to `[status=speed-profile]
   [reason=cmdline]`) — then re-running the identical cold-tap measurement dropped the worst
   frame from 150ms to **109ms, reproducibly across two runs**. Nothing in this project's
   install or measurement recipe (`:benchmark`'s generator, M13's own Verify steps, this
   session's own first two runs) ever ran that compile step, and **`androidx.profileinstaller`**
   — the runtime library that makes a bundled baseline profile self-apply on first launch
   outside Google Play — is not a dependency anywhere in `gradle/libs.versions.toml` or any
   module (confirmed via grep). Play-distributed installs get profile-guided compilation
   automatically as part of Play's own install process regardless of this library, but this
   project also ships an F-Droid flavor, and neither an F-Droid install nor a plain `adb
   install`/sideload gets it without `profileinstaller`. **This means M13's own "0 JIT
   lock-contention slices" result (13.2) and its addendum's 245ms/250ms "indistinguishable"
   verdict were almost certainly measured against the same never-actually-applied profile** —
   nothing in that session's recorded commands (`docs/archive/CHECKLIST-DONE.md`'s 13.2 Note)
   differs from this session's first two runs, which reproduced the un-applied `verify` state.
6. **Even correctly compiled, ~109ms of real stall remains — gap 5 does not explain all of gap
   1.** The `speed-profile` numbers (109ms) are still far above the `SubscriptionEditorContent`-removed
   baseline (31ms). The Perfetto trace in Finding 3 was captured *before* forcing
   `speed-profile` (i.e., under the same `verify` state as the 150ms runs) — it was **not**
   re-captured after; whether the same ~600 class-loads still dominate the remaining 109ms, or
   whether `speed-profile` eliminates most of them and something else accounts for the rest, is
   an open question (below), not measured this session.

## Root cause

Two distinct, independently-confirmed causes, only the first of which M13 targeted:

- **A real, structural gap in how this project installs and measures its own baseline
  profile**: nothing forces `speed-profile` dexopt, and `androidx.profileinstaller` — the
  library that would make that happen automatically outside Play — was never added. Every
  on-device measurement of the profile's effect in this project's history (M13's 13.2, its
  addendum, and this session's first two runs) was almost certainly taken against an
  unapplied profile.
- **A genuine, separate first-touch cost**: composing this screen's `OutlinedTextField`s for
  the first time in the process loads ~600 classes (Compose text-field/selection/undo/IME
  internals), a large share of it contending with concurrent ViewModel data loads on ART's
  global `ClassLinker` lock. This costs real time (~109ms, `speed-profile`-corrected)
  independent of whether the profile is applied, and is not concentrated in any one composable
  this session tried removing (pickers: only 14% of the effect).

## Impact

- Every real user's first open of the add-subscription editor in a fresh process pays this
  cost — likely every session, since the process is usually killed between app uses on a phone.
  Same shape as the 2026-08-09 doc's scroll-jank finding: a first-touch tax, not a persistent
  one within a session.
- The profile-not-applied gap (Finding 5) affects every measurement this project has made of
  M13's effectiveness, not just this screen — the scroll-jank numbers 13.2 called "did not
  improve, read worse" were likely also measured unprofiled, meaning that verdict, too, is now
  unconfirmed rather than settled.
- No correctness or data-loss impact — perceived smoothness only.
- No user-side workaround. `androidx.profileinstaller` (Finding 5's fix) helps every user
  automatically once shipped; nothing helps existing installs until they update.

## Open questions

- **Does `speed-profile` compilation reduce the ~600 class-loads, or just their execution
  speed?** Not re-traced after forcing `speed-profile` — the 109ms number came from `dumpsys
  gfxinfo` only. A baseline-profile *method* entry (`SPL` flag) tells `dex2oat` to AOT-compile a
  method ahead of time; whether that also front-loads the *class* linking/verification cost, or
  only speeds up the method once its class is already loaded, wasn't determined this session.
- **Is Finding 4 (first-`OutlinedTextField`-screen-in-the-path) actually true?** Inference from
  the app's own navigation structure, not a trace of every prior screen.
- **Does `androidx.baselineprofile`'s generator (`:benchmark`) already capture the class-load
  entries this screen needs, or only method entries?** The committed `baseline-prof.txt` has
  both bare class lines and `SPL`-prefixed method lines for this feature (confirmed via grep
  earlier in this session), but whether the *specific* ~600 classes in Finding 3 are among the
  bare class lines wasn't checked line-by-line.
- **Would broadening the `addSubscriptionEditorOpen` macrobenchmark journey
  (`BaselineProfileGenerator.kt:41`) to actually focus/type into a field — not just wait for the
  screen to appear — capture more of these classes** (`TextFieldSelectionManager`, `UndoManager`,
  IME adapters are plausibly focus-gated, not composition-gated)? Untested.
- **Does the scroll-jank verdict from 13.2 change once measured against a correctly
  `speed-profile`-compiled build?** Not re-measured this session; flagged in Impact.

## Options

1. **Do nothing further; leave this doc as the record.** Zero cost. Leaves the profile inert
   for every real non-Play install (including every F-Droid user) and leaves M13's "fixed"
   claims uncorrected in the checklist/plan beyond this doc's own existence.
2. **Add `androidx.profileinstaller` as an `androidApp` runtime dependency**, so a real install
   (Play, F-Droid, or sideload) self-applies the bundled profile on first launch, matching what
   Play already does automatically for Play-distributed installs. Small, mechanical, and is the
   documented, standard fix for exactly this gap — Google's own baseline-profile guide names it
   as required for non-Play distribution. *Cons:* doesn't touch Finding 6's remaining ~109ms;
   needs its own on-device re-verification (a fresh install, first launch, and enough idle time
   for `ProfileInstaller`'s background compile job to run — not immediate like the `cmd package
   compile -f` used to test this session, so the verify step is slower and less deterministic).
3. **Re-run M13's own scroll-jank comparison (13.2) against a correctly `speed-profile`-compiled
   build**, to find out whether that "did not improve, read worse" verdict changes — directly
   answers the Impact section's open concern that 13.2's own conclusion may be resting on the
   same unapplied-profile gap. *Cons:* real device time, and a "still doesn't improve" result
   wouldn't be actionable beyond confirming the current verdict stands.
4. **Investigate reducing Finding 3's first-touch class-load cost directly** — e.g., broaden the
   `addSubscriptionEditorOpen` benchmark journey to focus a field (Open Question 4), or add a
   deliberate warm-up touch of `OutlinedTextField` earlier in the navigation path (e.g. on
   Dashboard, off the user's critical path) so the FAB tap isn't the first time these classes
   load. *Cons:* unscoped research — the shape of a real fix isn't known yet, only that pickers
   aren't it; risks being a much larger milestone than it looks, the same pattern M13 itself
   already went through once (2026-08-09 doc → M13, real setup cost, partial result).
5. **Do 2 and 3 together, then decide separately whether 4 is worth it.** 2 is small, is
   correct regardless of what else is found, and re-establishes solid footing for every future
   measurement in this project (including a fresh 3). 3 directly answers whether M13's own
   record needs a bigger correction than this doc alone gives it. Both are prerequisites to
   deciding whether 4 (real, larger work) is worth taking on, rather than sizing it against
   numbers that may not reflect a correctly-installed app.

**Recommendation: Option 5.** 2 is unambiguously correct and cheap regardless of any other
finding here — this project already ships an F-Droid flavor that gets zero benefit from the
baseline profile work already done, and that's worth fixing on its own. 3 is what actually
settles whether M13's "closed" status needs reopening beyond this doc, which the checklist and
plan should not leave hanging once this doc exists. 4 is real but unscoped, exactly the shape
CLAUDE.md says shouldn't be sized until 2 and 3 give an honest baseline to size it against.

## What landed

**Option 2 — `androidx.profileinstaller` added** (`gradle/libs.versions.toml`,
`androidApp/build.gradle.kts`, version 1.4.1). Verified end-to-end on-device, not just by
compiling: fresh `adb install -r` of `gplayNonMinifiedRelease` → `[status=verify]
[reason=install]` (unchanged — Play's own install-time compile is what normally covers this, not
this library). Launched the app once; `adb logcat` confirmed `ProfileInstaller: Installing
profile for com.grappim.wallosmobile` fired automatically via the bundled
`ProfileInstallerInitializer` (`androidx.startup`, confirmed present in the built APK's manifest
via `aapt2 dump xmltree`, alongside the `ProfileInstallReceiver` with its `INSTALL_PROFILE`/
`SAVE_PROFILE`/`BENCHMARK_OPERATION` intent filters). Forcing the system's background dexopt job
to run now instead of waiting for real device idle+charging (`adb shell cmd package
bg-dexopt-job` — a genuine system mechanism, not a synthetic override) flipped the status to
`[status=speed-profile] [reason=bg-dexopt]` with **no manual `cmd package compile` step** —
confirming the automatic, production path actually works, not just the manual force used to
diagnose the gap. Re-measuring the same cold FAB-tap: first post-compile run 150ms (a one-time
disk-cache-cold artifact — the just-written `.odex` hadn't been read yet), then **117ms
reproducibly across two further runs** — close to, and consistent with, the 109ms the manual
`cmd package compile -f` force found in Finding 5. A real, if partial, win: ~22–27% off the
worst frame, landed via the real mechanism a user's device will actually use.

**Option 3 — 13.2's scroll-jank comparison re-run against this correctly-compiled build.**
Same recipe as 13.2 and the 2026-08-09 doc (3× `input swipe 540 2000 540 300 150`, Perfetto,
same category set), first scroll of a fresh cold process, now with `[status=speed-profile]
[reason=bg-dexopt]` confirmed active: **94.8% jank (55/58 frames), worst frame 107.53ms, 0 `Jit
code cache` lock-contention slices.** This is statistically indistinguishable from 13.2's own
two post-profile-but-unapplied runs (93%/101.9ms and 88%/107.3ms) — **the scroll-jank verdict
does not change.** Unlike the editor-open journey, the profile-application gap (Finding 5) does
not explain 13.2's "did not improve, read worse" result; that verdict stands on its own,
unaffected. The dominant jank types here are `Buffer Stuffing`/`Prediction Error`, already
flagged by the 2026-08-09 doc as possibly AVD/software-rendering artifacts rather than app code
— still unconfirmed, still needs real hardware to settle (per that doc's own open question).

**Net correction to M13's record:** the FAB-open "indistinguishable from list→detail" verdict
was wrong (it was true only because it compared two equally-unprofiled builds; a
correctly-profiled build measures ~117ms, not 245ms, but that's still well above "instant" and
well below list→detail's own unmeasured-here baseline — the two are no longer known to be
equal). The scroll-jank "did not improve" verdict was right, and stays right. M13's own
mechanism-level claim (0 JIT-code-cache lock contention) holds in both journeys, in both the
applied and unapplied state — it was simply never the metric that explained either journey's
felt slowness on this AVD.

**Left open, deliberately:** Option 4 (the ~600-class first-touch cost from Finding 3) — real,
unscoped, and this session's numbers (109–117ms remaining, down from 150ms) are the honest
baseline to size it against if it's picked up later. Also open: whether `Buffer Stuffing`/
`Prediction Error` on the scroll journey are AVD-specific (Open Questions, both docs) — still
needs real hardware.
