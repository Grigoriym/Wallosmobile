# 2026-08-09 — Cold-navigation and scroll jank (FAB open + subscriptions list)

**Status:** Done — Coil half fixed and verified; JIT/rendering half deferred (see What landed)
**Link:** none (internal; filed in `docs/CHECKLIST.md`'s "To review", 2026-08-07 and 2026-08-08)
**Updated:** 2026-08-09

## Report

Two separate backlog items from `docs/CHECKLIST.md`, merged into one investigation at the
user's request, on the hunch that they share a cause:

1. **"The FAB → add-subscription screen is still slower to open than list → detail, after
   4.4's fix."** (filed 2026-08-07). The add-subscription editor visibly takes longer to open
   than list→detail, even after 4.4 added per-picker loading spinners. Prior session's
   diagnosis (already partly verified, re-used rather than re-derived here): two causes — (a) a
   ~500–700ms network stagger on 2 of the 3 picker calls, confirmed server-side; (b) a JIT
   warm-up tax on cold navigation, found via a Perfetto frame-by-frame breakdown.
2. **"The subscriptions list scrolls laggy."** (filed 2026-08-08). Scrolling the list feels
   janky. No investigation had been done — only guesses (Coil logo loads per row, ViewModel
   flow recomposition, something in the `LazyColumn` item content).

**Environment**: local Wallos instance (docker `wallos`, port 8282), 35 subscriptions (28
active, 7 inactive), each with a distinct uploaded logo image. Verified on the
`Medium_Phone_API_36.1` AVD, booted `-gpu swiftshader_indirect` (software rendering — see the
methodology caveat in Findings), `gplay` debug build.

**What the reports don't say**: neither original entry included a captured trace — item 1's
JIT/network numbers came from a prior session and are re-used here, not re-measured; item 2 had
zero profiling data before this session.

## Findings

Two 8-second Perfetto traces were captured on-device with identical swipe sequences (three
`input swipe 540 2000 540 300 150` gestures) over the same section of the list: `scroll` (the
very first scroll in the process — all 35 logos loading for the first time) and `scroll_warm`
(the same swipes immediately after, images already decoded and cached). Category set:
`sched freq idle am wm gfx view input dalvik hal res memory binder_driver` (the
`emulator-testing` skill's Step 4b recipe).

**Methodology caveat — applies to every RenderThread/frame-duration number below.** The AVD
boots `-gpu swiftshader_indirect` (software rendering), per this project's own boot recipe —
confirmed live in the trace: `RenderThread`'s `Drawing  0.00  0.00 1080.00 2400.00` slice (a
full-screen software-rasterization marker) fires on every frame at a 13–16ms average, which is
already most of a 60Hz frame budget before the app does anything. Absolute RenderThread/frame
numbers on this AVD are therefore inflated relative to a real device's hardware-accelerated GPU
path and should not be read as literal on-device numbers. The *relative* cold-vs-warm
comparison, and the CPU/lock-contention findings (Coil, JIT), are backend-independent and are
the trustworthy part of this evidence.

1. **Static code trace rules out all three of the original report's guesses for item 2.** Read
   `SubscriptionsScreen.kt:146`, `SubscriptionCard.kt`, `SubscriptionsUiState.kt:22-32`,
   `SubscriptionsViewModel.kt:152-215`:
   - `items(items = uiState.items, key = { it.id })` — a stable key is already present
     (`SubscriptionsScreen.kt:146`).
   - `SubscriptionUiItem` is a `data class` of only `Int`/`String`/`Boolean`/`BillingCycle?`
     fields (`SubscriptionsUiState.kt:22-32`) — fully stable, no unstable collection/lambda
     params.
   - `uiState.items` is `ImmutableList<SubscriptionUiItem>` (`persistentListOf()`), not `List`.
   - `observeCache()` (`SubscriptionsViewModel.kt:152-161`) only re-emits on a real DB flow
     change, a filter/sort change, or a successful-refresh `refreshGeneration` bump — nothing
     re-emits per scroll tick or per frame.
   None of "missing key," "unstable item type," or "combined flow re-emitting during scroll"
   survive a read of the code.

2. **`SubscriptionLogo.kt:64-67` builds a fresh `ImageRequest` on every recomposition**, not
   `remember`ed. A minor allocation cost, not confirmed as measurable — Coil's
   `AsyncImageModelEqualityDelegate` compares by value, not instance (per the file's own doc
   comment, `SubscriptionLogo.kt:36-39`), so a fresh instance with the same `data`/cache-key
   doesn't itself trigger a reload. Flagged as inference, not measured directly.

3. **The cold scroll (35 distinct logos loading for the first time) is heavily janky, by two
   independent measures:**
   - `actual_frame_timeline_slice`: 136 of 168 frames (81%) carry a non-`None` `jank_type`
     during the ~2.5s of active swiping. Worst frames: 69.74ms, 69.37ms, 66.07ms, 62.27ms
     (60Hz's budget is 16.67ms). The dominant types include `Buffer Stuffing` (30 frames, avg
     33.04ms) and combinations with `App Deadline Missed`/`SurfaceFlinger CPU Deadline
     Missed` — the more actionable categories (see point 5 for the more ambiguous one).
   - Real lock contention inside **Coil's own disk cache**, not this app's code: 18
     `monitor contention … coil3.disk.DiskLruCache$Editor`/`.Snapshot` slices (50.6ms total),
     spread across 8 distinct `DefaultDispatcher-worker` threads, all blocked on the same
     `synchronized` file-editor lock inside `DiskLruCache.edit()` / `Editor.file()` /
     `Editor.commitAndGet()` (`DiskLruCache.kt:391`/`:722`/`:749` — Coil3's own source). This is
     Coil serializing ~20+ concurrent first-time image downloads' disk-cache writes against one
     lock, as the fling reveals rows whose logos have never been fetched in this process.

4. **The warm re-scroll of the identical rows (images already decoded and cached) shows nearly
   the same jank *frame count*, but far lower *severity* — the key falsification check:**
   - 132 of 184 frames (72%) still carry a jank type — barely different from cold's 81%.
   - Peak severity drops sharply: worst frame 20.77ms (cold: 69.74ms — a 3.4x drop); worst
     `RenderThread` "Running" span 15.07ms (cold: 39.43ms — a 2.6x drop).
   - Coil contention nearly disappears: 1 event, 2.7ms total (cold: 18 events, 50.6ms) —
     confirms the contention is specific to *first-time* loads, not a structural property of
     scrolling this list.
   - `Buffer Stuffing` disappears entirely as a jank type on the warm pass; the remaining jank
     is almost all bare `Prediction Error` (127 of 132) — Android's frame-timeline term for
     "the actual frame differed from what VSync predicted," which is at least partly a
     scheduling artifact rather than necessarily app-caused work, and the most ambiguous of the
     jank categories here.

5. **The FAB investigation's JIT-compilation tax (4.4, 2026-08-07) recurs during scrolling, and
   shares its exact mechanism.** `Lock contention on Jit code cache for mutator` — the marker
   the FAB investigation's own trace-frame breakdown used to explain its 121.9ms first-frame
   cost — appears in both scroll traces: cold, 119 slices totalling 4.57ms of wait; warm, 113
   slices (a near-identical *count*) but only 0.29ms total wait — a 16x drop in wait time for
   almost the same number of lock acquisitions. Broader JIT activity (`Compiling
   optimized`/`Compiling baseline`/`JIT compiling …`) stays high on both passes (cold: 1802
   slices; warm: 1556) — the process is still JIT-warming code paths exercised for the first
   time by scrolling (`Card` elevation/shadow drawing, `SubcomposeAsyncImage`'s subcomposition,
   Coil's decode pipeline), even on the "warm" pass, since "warm" here only means the *images*
   were cached — not that every code path the scroll exercises had already run once. This is
   genuinely the same root mechanism as the FAB screen's slow open (ART JIT-compiling cold code
   under a lock the main thread's rendering work contends on), triggered here by scrolling into
   new rows instead of opening a heavier screen.

## Root cause

Two distinct, real costs — confirmed by trace, not guessed — and only partially overlapping
between the two original backlog items:

- **The subscriptions list's scroll jank is caused primarily by Coil loading many
  previously-unfetched logo images at once during a fast fling** — real lock contention inside
  Coil's own disk-cache writer, plus the general cost of decoding/uploading ~20 new bitmaps in a
  couple of seconds. The cold-vs-warm comparison isolates this cleanly: peak frame cost 69.74ms
  → 20.77ms, Coil contention 50.6ms → 2.7ms, once the same rows are re-scrolled warm.
- **Both the FAB-open slowness and a `Prediction Error`/JIT floor that persists on the list even
  once images are warm are the same mechanism 4.4's investigation already found**: ART
  JIT-compiling this process's cold code paths under a lock the main thread's rendering work
  contends on. It is not specific to the FAB screen — it recurs anywhere a code path is
  exercised for the first time in the process, including scrolling.
- None of the original report's three guesses (missing `key`, item-type instability, ViewModel
  flow re-emission during scroll) are the cause — all three are ruled out by the code itself,
  independent of the trace.

## Impact

- The subscriptions list is very likely the first screen a real user's cold app process
  scrolls, since it's one tap from Dashboard, the start destination — so this is a
  first-scroll-of-the-session cost for most users, **every session**, not a one-time
  first-install cost. Scrolling the list again later in the same process (without killing the
  app) should be visibly smoother, per the warm-pass numbers.
- No correctness or data-loss impact — purely perceived smoothness.
- No user-side workaround exists; nothing in the app currently mitigates either cause.

## Open questions

- **How much of the JIT floor is fixable without a Baseline Profile?** The FAB investigation
  already named Baseline Profiles as the standard fix for this class of problem and explicitly
  left it unscoped ("plan §8/Phase 5 territory... not investigated further"). This session
  confirms the same mechanism recurs on the list but doesn't reduce that scoping question.
  Generating one needs the `androidx.baselineprofile` Gradle plugin
  (`build-logic`/`gradle/libs.versions.toml` → a `Gate-change:` line) and a
  `MacrobenchmarkRule`-driven profile-generation module — real setup cost, not evaluated here.
- **Whether the residual bare-`Prediction Error` jank on the warm pass (127 of 132 janky
  frames) is a real app cost or a `swiftshader_indirect`-emulator artifact** is unresolved —
  this session had no path to real hardware. The options below treat it as unconfirmed, not
  actionable on its own.
- **Whether Coil's concurrent-request limit is configurable from this app's
  `AppModule.provideImageLoader`** wasn't checked — that's the natural lever for the
  disk-cache-contention finding, and belongs in the fix-design step, not this doc.
- **The FAB screen's network-stagger half (item 1(a), ~500–700ms on 2 of 3 picker calls) was
  not re-investigated here** — it's unrelated to JIT/rendering, and the prior session's finding
  stands as-is. Restated in Options for completeness since it's part of the same backlog line.

## Options

1. **Do nothing further; leave both items in "To review."** Zero cost, zero risk. Leaves a
   real, now well-evidenced smoothness issue unaddressed on every user's first scroll of a
   session. Listed for completeness, not recommended, since the user asked for this
   investigation specifically.

2. **Cap or stagger Coil's concurrent disk-cache writes**, to fix the scroll-specific finding,
   leaving the JIT floor to a future Baseline Profile. Directly targets the strongest, most
   isolated finding (Coil contention: 18 events → 1 event, cold vs. warm) without touching
   JIT/rendering, which is unscoped, larger work already flagged by the FAB investigation.
   *Pros*: small, scoped, independently verifiable (re-run the same cold-scroll trace, compare
   contention-event counts before/after). *Cons*: won't close the full cold-vs-warm gap (some of
   the delta is JIT, not Coil); the right Coil API/setting to change wasn't identified this
   session (open question above), so this starts with a small research step, not a known
   one-line fix.
3. **Generate an Android Baseline Profile** covering cold app start, first list scroll, and
   first editor open — closing the JIT-tax half of both backlog items in one piece of work,
   since both trace back to the same ART mechanism. *Pros*: addresses the actual shared root
   cause, not just its most visible symptom on one screen. *Cons*: real setup cost (Gradle
   plugin + `libs.versions.toml` entry → a `Gate-change:` line, a macrobenchmark module, a
   `MacrobenchmarkRule`-driven generator, CI/release wiring to ship the generated profile) — the
   FAB investigation already called this "unscoped, separate work," and this investigation
   didn't reduce that scope. Milestone-sized, not step-sized.
4. **Do both 2 and 3, as two independently landable pieces of work**, since they target
   different, independently-confirmed causes — Coil contention is real and isolated regardless
   of the JIT question, and the JIT floor is real and isolated regardless of the Coil question —
   and neither blocks the other.

**Recommendation: option 4, sequenced — land 2 first as one scoped step, then decide separately
whether to take on 3 as its own milestone.** (2) is small, has a clean before/after metric
already established in this doc (the Coil contention-event count), and delivers a real,
measurable improvement to the worst-case cold-scroll frames on its own. (3) is real work the
project has now deferred twice (the FAB investigation, and again here) rather than re-scoped
down — bundling it into "the scroll fix" would turn a small step into a milestone. Landing 2
first doesn't foreclose 3 later; if anything, removing the lock-contention noise first makes it
easier to isolate how much of the remaining jank the JIT floor alone accounts for, if 3 is
picked up.

## Decision

**2026-08-09, user: proceed with option 4, sequenced — option 2 now, option 3 deferred.**

Confirmed the lever for option 2 exists before starting: Coil3's own source
(`coil-core-3.5.0-sources.jar`, `ImageLoader.kt:256-272`) shows `ImageLoader.Builder` has no
built-in concurrency cap — `EngineInterceptor`/`RealImageLoader` run every fetch and decode
directly on whatever `fetcherCoroutineContext`/`decoderCoroutineContext` is configured
(`ImageLoader.kt:259`/`268`, both default to plain `Dispatchers.IO`, which is effectively
unbounded). Passing `Dispatchers.IO.limitedParallelism(N)` as the fetcher context is the
supported way to cap how many fetches — and so how many concurrent Coil disk-cache writes — run
at once, which is the mechanism this investigation caught contending.

Single testable part:

- **Cap `AppModule.provideImageLoader`'s fetcher concurrency** via
  `.fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(4))` on the `ImageLoader.Builder`
  (`composeApp/.../di/Koin.kt:109-119`). 4 chosen as a real cap well under the ~8-20 concurrent
  fetches the trace caught piling up, while still letting several logos load in parallel rather
  than serializing to 1.
- **Verification**: re-run this doc's exact cold-scroll recipe (`pm clear` first, so every logo
  is unfetched again — reusing the *warm* app process would not reproduce the contention to
  disprove) and compare the `monitor contention … coil3.disk.DiskLruCache` slice count/total
  duration against this doc's baseline (18 events, 50.6ms). A material drop, with the app still
  visibly loading images (not stalling on a near-serial 1-at-a-time queue), is success. No
  existing automated test covers Coil wiring (`AppModule` is DI glue, exercised by
  `KoinGraphTest` for resolvability only) — this is a manual on-device check, the same category
  as 4.4's own picker-spinner verification.
- Option 3 (Baseline Profile) stays out of scope for this step — left as its own future
  milestone decision, not reopened here.

## What landed

**The change**: `composeApp/.../composeapp/di/Koin.kt` — `AppModule.provideImageLoader` now
passes `.fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(COIL_FETCH_PARALLELISM))`
(`COIL_FETCH_PARALLELISM = 4`) to the `ImageLoader.Builder`, capping how many Coil fetches (and
so how many concurrent disk-cache writes) run at once. Needed `@OptIn(ExperimentalCoilApi::class)`
— `fetcherCoroutineContext` is marked experimental in coil3 3.5.0. `detekt`/`ktlintCheck` and
`:androidApp:compileGplayDebugKotlin --rerun-tasks` all pass.

**Verification — the Coil half is a clean, confirmed win.** Re-ran this doc's exact cold-scroll
recipe three times post-fix, on-device (`Medium_Phone_API_36.1`, `pm clear` then a fresh login,
matching the baseline's method):

| Run | Condition | `DiskLruCache` contention events | Total contention time | Distinct threads hit |
|---|---|---|---|---|
| Baseline (pre-fix) | fresh install, first scroll | 18 | 50.6ms | 8 |
| Fix, run 1 | fresh install, first scroll | 6 | 3.45ms | 3 |
| Fix, run 2 | same warm process, disk cache only cleared, re-scrolled | **0** | — | — |

Contention drops monotonically and disappears entirely in run 2 — the fix does exactly what it
was designed to do, with no regression (images still visibly load; nothing serialized to feel
like a 1-at-a-time queue in either post-fix run's screenshots).

**What was deliberately left out, and why — an honest result, not a partial one.** Across all
three post-fix conditions, the *overall* `actual_frame_timeline_slice` jank numbers stayed
essentially flat versus the pre-fix baseline: 131/166 and 133/170 frames janky (78–79%) post-fix
vs. 136/168 (81%) pre-fix, worst frames 92–100ms post-fix vs. 69.74ms pre-fix. Run 2 in
particular — **zero** Coil contention, yet jank count/severity indistinguishable from the
baseline — is the clearest evidence in this whole investigation that Coil's disk-cache
contention, while real and now fixed, was never the dominant source of the *aggregate* frame
jank this AVD measures. That leaves the JIT-compilation floor (Finding 5) and/or the
`swiftshader_indirect` software-rendering confound (the methodology caveat at the top of
Findings) as the more likely dominant causes — and this session has no way to separate those two
from each other, or to confirm how much of either generalizes to a real device, without hardware
to test on. Option 3 (a Baseline Profile) remains the next real lever for the JIT half, and stays
a deferred, separate milestone decision rather than something this step attempted.

**Net effect for the user**: a real, verified reduction in one confirmed cause of scroll jank,
landed as a small, low-risk change — but not a claim that the list now scrolls smoothly, which
this session cannot confirm from this AVD alone.
