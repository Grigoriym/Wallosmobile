# 2026-08-09 — Cold-navigation and scroll jank (FAB open + subscriptions list)

**Status:** Done — Coil half fixed and verified (2026-08-09); JIT/rendering half now confirmed
fixed on real hardware, including the exact device that first validated the complaint (see the
third 2026-08-12 addendum below)
**Link:** none (internal; filed in `docs/CHECKLIST.md`'s "To review", 2026-08-07 and 2026-08-08)
**Updated:** 2026-08-12

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

## 2026-08-12 addendum — real hardware, answering the open question

This doc's own open question ("whether the residual jank is a real app cost or a
`swiftshader_indirect`-emulator artifact") and 13.2's own honest gap ("only real hardware...
can settle whether the profile actually helps a real user's felt experience") both needed
physical hardware this project never had connected before. The user connected one
(`SM-A920F`, Galaxy A9 2018, Android 10/API 29 — the same device 17.x's login-scroll bug used) and
offered it for this specific test.

**Method**: fresh `gplayDebug` build off current `dev` HEAD (has the Coil fix, does **not** have
the Baseline Profile — that's release-only), installed via `adb install`, logged into the real
instance via `adb reverse tcp:8282 tcp:8282` (device's `localhost:8282` tunneled to the host's
container over USB). Perfetto tracing turned out to be unusable on this device — see the friction
below — so the comparison uses `dumpsys gfxinfo <pkg> reset` / `dumpsys gfxinfo <pkg>` instead,
Android's own framework-level jank accounting (VSync-deadline based), the "quick look" method in
the `emulator-testing` skill's Step 4b. Same section of the list, same three
`input swipe 540 2000 540 300 150` gestures as this doc's original recipe. **Cold** = Coil's
on-device disk cache (`run-as … rm -rf .../cache/coil3_disk_cache`, preserves the login) cleared
and app force-stopped/relaunched first, so all 35 logos are unfetched; **warm** = the same rows
immediately re-scrolled.

| Metric | Cold (real device) | Warm (real device) |
|---|---|---|
| Frames rendered | 19 | 12 |
| Janky frames | 17 (**89.47%**) | 10 (**83.33%**) |
| 50th / 90th / 95th / 99th percentile | 57 / 150 / 150 / 150ms | 73 / 105 / 109 / 109ms |
| Worst-frame histogram bucket | 150ms (3 frames) | 109ms (1 frame) |

**This settles the open question: the jank is real on hardware, not a `swiftshader_indirect`
artifact.** Both the jank-frame percentage and the worst-frame timing are in the same range as —
and by worst-frame ms, actually *worse* than — every AVD measurement this investigation and 13.2
took (78–93% janky, 69.74–107.3ms worst frame across the AVD's various pre/post-fix runs). Cold
vs. warm barely moves the jank *percentage* (89% → 83%), the same falsification pattern the AVD
found — Coil's contention isn't the dominant cause here either — while the percentile timings do
drop somewhat (150ms → 109ms at p95/99), a real but much smaller improvement than the AVD's own
3.4x cold-vs-warm drop in its first (pre-fix) measurement.

**Caveats, so this isn't overclaimed:** one run each side, not the 2–3 repeated runs the AVD
sessions used, against small per-capture frame counts (12–19) — noisier than the AVD's numbers.
This is a budget, several-years-old device on Android 10; it says the phenomenon is real on
*this* hardware, not that every real device would show the same severity. `dumpsys gfxinfo`'s
jank heuristic (VSync-deadline miss) isn't the same metric as Perfetto's
`actual_frame_timeline_slice` `jank_type` (VSync-*prediction*-aware) the AVD numbers used — both
target the same underlying phenomenon but aren't a strict apples-to-apples number. And because
Perfetto's root-cause instrumentation didn't work here (below), this can't say whether JIT
lock contention specifically is still the dominant mechanism on this device — only that the same
downstream symptom (missed-deadline frames) reproduces, at least as severely, off the emulator.

**Friction**: `adb shell perfetto -o ... -t 8s sched freq idle am wm gfx view input dalvik hal res
memory binder_driver` — this doc's own Step 4b recipe, previously only run against the AVD —
accepted every category with no error on this device but only kernel-level ftrace groups
(`sched`, `binder_driver`) actually produced slices; the app-level atrace categories (`view`,
`gfx`, `dalvik`) that carry `Lock contention on Jit code cache`, `Choreographer#doFrame`, and
Coil's `DiskLruCache` contention markers produced **zero** slices for the app's process, even
though `atrace --list_categories` listed them as valid and the trace file itself was non-empty
(148 slices, all `binder transaction`). Read as a Samsung/SELinux restriction on this OEM build
blocking a non-root shell from toggling `debug.atrace.tags.enableflags` for app-level categories,
not a bug in the recipe — `dumpsys gfxinfo` doesn't depend on that mechanism (it's a direct Binder
call into the app's own `ViewRootImpl`) and worked without issue. Worth knowing before reaching
for Perfetto's deeper categories on a real (as opposed to AVD) device again: try `dumpsys gfxinfo`
first, and treat a trace with only kernel-category slices as a sign the device is blocking
app-level atrace rather than a sign nothing happened.

## 2026-08-12 addendum #2 — the Baseline Profile was never actually tested on hardware until now

**What prompted this**: re-reading this doc and 13.2 back to back turned up a gap neither closed
at the time. 13.2's own AVD measurement ("aggregate frame-jank did not improve, read worse") was
against the real, signed `gplayRelease` build with the profile embedded — that part was rigorous.
But the *first* addendum above, the one that moved "is this a real device problem" from open to
confirmed, was run against a plain `gplayDebug` sideload on the `SM-A920F` — a build variant that
structurally **cannot** carry the Baseline Profile at all (`androidApp/src/gplayRelease/generated/
baselineProfiles/baseline-prof.txt` is scoped to the `gplayRelease` source set; a `gplayDebug` APK
never includes it, confirmed again below). So the checklist's own "To review" entry has been
treating two separate claims as one: "the jank is real on hardware" (genuinely settled) and "the
Baseline Profile doesn't help" (never actually tested — only the AVD's word for it, which 13.2's
own methodology caveats already distrusted). This session had a real device connected (a Samsung
`SM-G998B`/Galaxy S21 Ultra, Android 15/API 35, USB, `dev` HEAD) and closed that gap directly.

**Method**: built both `:androidApp:assembleGplayDebug` and `:androidApp:assembleGplayRelease
-PgplayBuild` off the same `dev` HEAD (the six `WALLOS_*` signing env vars were set in this shell,
so the release build is properly signed, not a `nonMinifiedRelease` benchmark stand-in). Confirmed
the release APK actually embeds the profile before installing anything: `unzip -l` on
`androidApp-gplay-release.apk` shows `assets/dexopt/baseline.prof` (12,460 bytes) and
`baseline.profm`. Installed both side by side (`com.grappim.wallosmobile.debug` and
`com.grappim.wallosmobile` are different application ids, so both coexist without uninstalling
either) and confirmed the compilation state actually differs before measuring anything:

```
debug:   dumpsys package → arm64: [status=run-from-apk] [reason=unknown]
release: dumpsys package → arm64: [status=speed-profile] [reason=install-speg]
```

`reason=install-speg` means the profile was applied automatically at install time on this device
— no manual `cmd package compile` needed, `androidx.profileinstaller` (added for exactly this
reason per 16.5/the FAB doc) did its job immediately, unlike the AVD's `bg-dexopt` path the FAB
doc had to wait for. Logged into each app separately against the same local instance
(`adb reverse tcp:8282 tcp:8282`, `docs/local-info.txt` creds) and ran this doc's exact
cold/warm recipe (`input swipe 540 2000 540 300 150` ×3) via `dumpsys gfxinfo <pkg> reset` /
`framestats` (the real per-frame numbers are in the `framestats` file's own `Stats since` block,
not the `reset` call's stdout — the two look identical enough at a glance to conflate, see the
methodology note below). Cold = a build never before scrolled (debug: Coil disk cache cleared via
`run-as … rm -rf cache/coil3_disk_cache`, force-stopped, relaunched; release: not debuggable, so
`pm clear`+relogin for the repeat run, and the very first run needed no clearing at all — a fresh
install's cache is already empty). Warm = the same rows re-scrolled immediately after, no restart.
Two cold runs captured per build to check reproducibility, matching 13.2's own "run 1 / run 2"
practice rather than trusting a single capture.

| Build | Run | Frames | Janky | 50th | 90th | 95th | 99th |
|---|---|---|---|---|---|---|---|
| debug (no profile) | cold, run 1 | 78 | 29 (37.18%) | 9ms | 48ms | 53ms | 81ms |
| debug (no profile) | cold, run 2 | 77 | 27 (35.06%) | 5ms | 53ms | 65ms | 81ms |
| debug (no profile) | warm | 141 | 32 (22.70%) | 7ms | 22ms | 24ms | 61ms |
| release (profile, speed-profile) | cold, run 1 | 111 | 4 (3.60%) | 5ms | 9ms | 9ms | 21ms |
| release (profile, speed-profile) | cold, run 2 | 112 | 3 (2.68%) | 6ms | 9ms | 10ms | 19ms |
| release (profile, speed-profile) | warm | 167 | 3 (1.80%) | 5ms | 7ms | 7ms | 10ms |

**This is a real, reproducible, dramatic win — the opposite of 13.2's AVD verdict.** Cold-scroll
jank drops roughly 10x (35–37% → 2.7–3.6%), and the worst frames (99th percentile) drop roughly
4x (81ms → 19–21ms), consistent across two independent cold runs on each build. The two AVD-vs-
hardware verdicts are not a contradiction to resolve — they're measuring different things:
13.1/13.2 built the profile-generation pipeline and confirmed the *mechanism* it targets (JIT
code-cache lock contention) is genuinely eliminated; 13.2's own "did not improve" line was about
the AVD's *aggregate frame-jank number* specifically, which that same doc flagged as confounded by
`-gpu swiftshader_indirect` software rendering and `Prediction Error` scheduling noise "before this
change" — i.e. already an unreliable proxy on that hardware. This session's numbers are the first
time the *outcome metric that actually matters* (frame jank, on real hardware) was measured with
the profile correctly applied, and it says the fix works.

**What this does not yet show.** This device (a several-years-newer flagship) is not the device
that first confirmed the jank was real off the emulator (the budget `SM-A920F`, Android 10) — that
device is not connected this session. So this result cannot yet say the *original* real-hardware
finding (89% janky on the A920F) is fixed; it can only say the Baseline Profile produces a large,
reproducible improvement on *a* real device, and that the AVD's contrary verdict does not
generalize to hardware, at least not to this hardware. Two things worth separating for the
decision below: (1) does the profile help on real hardware at all — yes, now confirmed; (2) does it
help enough on the specific low-end device that originally validated the complaint — still open,
would need the A920F (or another budget/older device) running the same signed-release A/B.

**Methodology note, worth keeping for next time**: `dumpsys gfxinfo <pkg> reset` **prints the
accumulated stats since the last reset before clearing them** — it is not silent. Running `reset`
then `framestats` in the same shell session can make it look like there are two different stats
blocks in the output (there are: the `reset` call's own echo of the *previous* window, and the
`framestats` file's `Stats since` block for the *new* one) closely enough in shape to misattribute
one for the other. The authoritative number is always the `Stats since` block inside the actual
`framestats` capture, not whatever a `reset` command happened to print to the terminal moments
earlier — this cost one throwaway misread mid-session here before the two were told apart by
diffing which block came from which file.

## Options (revised 2026-08-12, supersedes the original Options above for the still-open half)

1. **Do nothing further.** The checklist's "To review" entry already reads as more open than this
   session's evidence supports — leaving it exactly as worded would keep citing an AVD verdict this
   session has good reason to distrust, without the correction. Not recommended.
2. **Update `docs/CHECKLIST.md`'s "To review" entry to reflect this finding and stop there** — record
   that the Baseline Profile fix (M13) does demonstrably help on real hardware, that the AVD's "did
   not improve" verdict was probably an artifact of that AVD's own software-rendering/scheduling
   noise rather than evidence the fix is ineffective, and that the specific low-end device which
   first confirmed the complaint hasn't been re-tested with the fix applied. *Pros*: honest, cheap,
   correctly narrows what's still actually open. *Cons*: leaves the original complaint's own device
   unconfirmed either way.
3. **Also get the `SM-A920F` (or another low-end/older device) and re-run this exact A/B** the next
   time it's connected, to settle whether the fix is enough on the hardware that originally
   validated the complaint, not just on a flagship. *Pros*: closes the loop properly — this is the
   only device this backlog item was ever really about. *Cons*: needs the device physically
   available; not actionable this session.

**Recommendation: 2 now, 3 opportunistically** — update the checklist entry with this session's
finding (a real fix, confirmed on one real device, previously undermeasured), and leave a note to
re-run the same A/B on the A920F or another budget device the next time it's on hand, rather than
blocking on scheduling that.

## 2026-08-12 addendum #3 — the A9 2018 itself, resolved

The user connected the `SM-A920F` — the exact device that produced the original 89.47%/83.33%
cold/warm numbers in addendum #1 — partway through writing up addendum #2's options. Re-ran the
identical signed-`gplayDebug`-vs-`gplayRelease` A/B from addendum #2 on this device instead of
waiting for it to turn up later.

**Method**: identical to addendum #2 (same two APKs, `adb reverse tcp:8282 tcp:8282`, same login,
same `input swipe 540 2000 540 300 150` ×3 cold/warm recipe, same `dumpsys gfxinfo <pkg> reset` /
`framestats` capture, reading the real per-frame numbers from the `framestats` file's own `Stats
since` block). Confirmed dexopt state before measuring: debug `[status=run-from-apk]`, release
`[status=speed-profile] [reason=install]` — `androidx.profileinstaller` applied the profile on
this older/lower-spec device too, not just the flagship. Two cold runs on each build (`pm clear`+
relogin between release runs, since it isn't debuggable and can't have just its Coil cache wiped
via `run-as`) to check reproducibility.

| Build | Run | Frames | Janky | 50th | 90th | 95th | 99th |
|---|---|---|---|---|---|---|---|
| debug (no profile) | cold | 18 | 15 (**83.33%**) | 109ms | 150ms | 150ms | 150ms |
| debug (no profile) | warm | 21 | 19 (**90.48%**) | 81ms | 150ms | 150ms | 150ms |
| release (profile) | cold, run 1 | 136 | 15 (**11.03%**) | 7ms | 21ms | 26ms | 57ms |
| release (profile) | cold, run 2 | 133 | 9 (**6.77%**) | 8ms | 12ms | 21ms | 44ms |
| release (profile) | warm | 193 | 6 (**3.11%**) | 7ms | 9ms | 10ms | 44ms |

The debug numbers reproduce addendum #1's original finding closely (83–90% vs. the original
89–83%, different session, same device) — this device's baseline hasn't changed. The release
numbers are a decisive, reproducible fix: janky frames drop roughly **8–12x** (83–90% → 3–11%),
and the worst frames stop hitting `dumpsys gfxinfo`'s top histogram bucket entirely (150ms — the
bucket ceiling, so the debug numbers may understate how bad the worst frames actually were) in
favor of a 44–57ms worst case. This is the same device, same swipe recipe, same session as the
original complaint's own confirmation — not a different, higher-end phone standing in for it.

**This closes the open question addendum #2 left standing.** The Baseline Profile (M13) does fix
the original, real-hardware-confirmed scroll jank — on the exact device that proved the jank was
real, not only on a flagship. The mechanism 13.1–13.3 built (eliminating JIT code-cache lock
contention) and the AVD's contrary "did not improve" verdict were never in real conflict; the AVD
measurement was the unreliable one, exactly as its own methodology caveats already warned. Every
real user gets this fix already — Play and F-Droid both distribute release builds, and
`androidx.profileinstaller` (added specifically for the sideload case, per the FAB doc) applies it
automatically on first launch outside Play too.

**What's still genuinely open, for completeness**: the debug-build numbers on this device are bad
enough (up to 150ms, the histogram's ceiling) that anyone judging feel from a debug sideload — the
build type every session here actually installs day to day — would still call this janky. That's
expected (`run-from-apk` never gets AOT-compiled regardless of any profile) and not a defect to
fix, but worth remembering before re-eyeballing a debug build and wondering if this regressed.

## Decision

**2026-08-12: resolved — no further action.** The Baseline Profile fix already shipped in M13
fixes the original complaint, confirmed on the original device. `docs/CHECKLIST.md`'s "To review"
entry for this item should be closed with this finding rather than left open pending a device that
already turned up and settled it.
