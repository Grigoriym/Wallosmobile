# CLAUDE.md

WallosMobile — an unofficial Kotlin Multiplatform client for [Wallos](https://github.com/ellite/Wallos),
a self-hosted subscription tracker. **Android only for now**, built KMP-first.

## Status

`docs/CHECKLIST.md`'s `Progress` / `Current step` header is the **only** record of how far the
build has got. Don't duplicate it here — it would go stale every session.

## How work happens here

**One checklist step per session, with context cleared in between.**

1. `docs/CHECKLIST.md` — the executable plan. Numbered, tickable steps, each self-contained.
   The ticked ones live in `docs/CHECKLIST-DONE.md`; read that only for precedent on a step that
   is cited by number, never to find work.
2. `docs/IMPLEMENTATION_PLAN.md` — the reference. Architecture and rationale.
3. `docs/WALLOS_API.md` — the API contract, derived from Wallos PHP source.

When asked to "do step N": read `docs/CHECKLIST.md`, do **exactly** that step, run its `Verify:`
line, then tick the box and update the `Progress` / `Current step` header. Put anything that
affects later steps in the Deviations log at the bottom. Don't start a step whose dependencies
aren't ticked, and don't expand scope beyond the step.

**Then close the step out — every time, without being asked:**

1. Run the **`/finalize` skill**. A step almost always teaches something the plan didn't know;
   this is where it gets written down instead of dying with the context.
2. Fold anything structural out of the step's `Note:` and into `IMPLEMENTATION_PLAN.md`, then
   annotate the Deviations row (*now in plan §X*) so the log and the plan can't contradict
   each other.
3. **Check the docs for claims the step just made false** — `IMPLEMENTATION_PLAN.md` and this
   file both accumulate stale present-tense statements (a build command that no longer exists,
   a "the repo is still…" line). Grep for what changed rather than trusting a read-through.
4. Commit and push. One commit per step, straight to `master` — subject `0.N — short title`,
   body listing the deltas that aren't obvious from the diff.

### Changing a gate means saying so

The gates constrain the code a session writes; nothing constrains a session from widening a gate
so its own step passes. `.github/workflows/guardrails.yml` doesn't prevent that — it makes it
impossible to do quietly. A commit trips it by touching `.github/`, `build-logic/`,
`config/detekt/`, `.editorconfig` or `gradle/libs.versions.toml`; by adding an `@Ignore` or a
`@Suppress`; or by **reducing** the number of Non-negotiables below or of steps in
`docs/CHECKLIST.md` **plus `docs/CHECKLIST-DONE.md`** — the two are summed, so moving a step
between them is free and dropping one from either is not. Any of those needs a line in the commit
message:

```
Gate-change: what was widened, and why
```

That is an opt-in, not a veto — widening a gate is often right. Run it before committing:
`.github/scripts/check-guardrails.sh HEAD~1..HEAD`. A `@Suppress` **carried over from a reference
project** is worth deleting and recompiling first: 3.7's ported `X509Certificate` fake came with
`@Suppress("OVERRIDE_DEPRECATION")` that this toolchain doesn't need, and it would have bought a
`Gate-change:` line for a warning that never fires. Editing the two rule documents is otherwise
free, so ticking a box, adding a `Note:` and reflowing a bullet all pass; only *dropping* a rule
or a step counts.

## Build commands

```bash
./gradlew :androidApp:assembleDebug          # build
./gradlew allTests                           # all KMP module tests
./gradlew :module:path:testAndroidHostTest   # one module
./gradlew detekt ktlintCheck                 # must pass before ticking a step
./gradlew :module:path:ktlintFormat          # fix style — don't hand-format
./gradlew koverHtmlReport                    # coverage

# The Room DAOs are the one instrumented suite (3.3) — an emulator must be up, and neither
# `allTests` nor CI runs it. Nothing else in the repo has a device-test source set.
./gradlew :core:storage:connectedAndroidDeviceTest

# ktlint's `standard:function-signature` rule collapses any signature that fits in 120 chars back
# onto one line, so hand-wrapping a parameter list "for readability" fails `ktlintCheck`. Write it
# either way and let `ktlintFormat` decide.

# There is no `jvmTest` and no `testDebugUnitTest`. WallosMobile declares no `jvm()` target, so
# the only unit test task is the AGP KMP host test — `testAndroidHostTest`, source set
# `commonTest`, enabled by `withHostTestBuilder` in `KmpLibraryConventionPlugin`.

# Force the Koin compiler plugin to re-run after DI changes ("no definition found" crashes).
# Koin here is a Kotlin compiler plugin, NOT classic KSP — there is no build/generated/ksp/**
# to inspect; a clean compile is the only signal that new @Single/@KoinViewModel were picked up.
./gradlew :androidApp:compileDebugKotlin --rerun-tasks

# To actually see what a module's @ComponentScan found — the graph can't be started before the
# app is wired — read the generated module out of the bytecode. One `module$lambda` per definition:
javap -p -c core/storage/build/classes/kotlin/android/main/com/grappim/wallosmobile/core/storage/\
ComGrappimWallosmobileCoreStorageStorageModuleModuleKt.class | grep "private static final"
```

A step whose `Verify:` line is about the running app is verified **on the emulator**, not by
assembling. Headless, no snapshot, no interaction needed beyond `input tap`:

```bash
~/Android/Sdk/emulator/emulator -avd Medium_Phone_API_36.1 -no-snapshot-save -no-boot-anim \
  -gpu swiftshader_indirect &
adb wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
./gradlew :androidApp:installDebug && adb shell am start -n com.grappim.wallosmobile/.MainActivity
adb exec-out screencap -p > shot.png   # then read shot.png; tap with `adb shell input tap X Y`
adb emu kill                           # don't leave it running
```

`screencap` is 1080×2400 while the image comes back scaled — multiply the coordinates read off
the screenshot by the stated factor before feeding them to `input tap`.

**"Is it dark?" is a pixel, not an impression.** `python3 -c "from PIL import Image; print(
Image.open('shot.png').convert('RGB').getpixel((540, 220)))"` answers it in one line —
`(26, 27, 31)` is `SurfaceDark`, `(253, 251, 255)` is `SurfaceLight` — and it is the only way to
compare two screenshots that differ by one role. It is also what makes a *crop* worth taking:
the dark-on-dark status bar of 4.2 is invisible at full-page scale and obvious at 2× on the top
90 rows.

**A stored preference can be planted before its UI exists** (4.2), which is what lets a storage
step verify on device instead of waiting for the screen that sets it. DataStore Preferences is a
plain protobuf `map<string, Value>` with **no checksum**, and protobuf merges repeated fields — so
appending one encoded entry to `files/datastore/wallos_storage.preferences_pb` sets a key without
disturbing the stored URL, key or cert pins; appending the same key twice makes the last win; and
`truncate -s <original size>` undoes all of it. Force-stop first, or the running app overwrites it.

```bash
python3 -c "
import base64
def ld(b): return bytes([len(b)])+b
v=b'\x2a'+ld(b'dark'); e=b'\x0a'+ld(b'theme_mode')+b'\x12'+ld(v)   # key, then Value.string
print(base64.b64encode(b'\x0a'+ld(e)).decode())"
adb shell "run-as com.grappim.wallosmobile sh -c 'echo <b64> | base64 -d >> \
  /data/data/com.grappim.wallosmobile/files/datastore/wallos_storage.preferences_pb'"
```

**Note the quoting**: `adb` flattens its arguments into one string that the *device's* shell
re-parses, so inner single quotes are stripped and a `$VAR` expands on the device — hence the outer
double quotes, no shell variables, and an absolute path (`run-as` does not leave you in the app's
data directory).

Filling a form: **tap the first field once, then `input keyevent KEYCODE_TAB` between fields.**
Compose honours TAB for focus, and re-tapping by coordinate goes wrong the moment the keyboard
opens and shifts the layout — a mis-tap lands on whatever moved into that spot (typing an API key
into the URL field, say). **The keyboard is not the only thing that moves the fields**: this screen
grows a cleartext warning and an SSO notice *between* the URL and the credentials, so coordinates
read off a screenshot are stale as soon as the URL is typed. 3.10 put a username into the URL field
exactly that way.

**`input text` needs a `sleep 1` after each field**, or a long value arrives **truncated** — and
a truncated password fails as `InvalidCredentials`, which reads exactly like a wrong credential
and sends you looking at the server. Count the dots in the password field against a screenshot of
a known-good attempt before believing the error. Clearing a field to retry:
`input keycombination 113 29` (Ctrl+A) then `input keyevent KEYCODE_DEL`.
On the *last* field, `input keyevent KEYCODE_ENTER` submits — the login screen's `ImeAction.Done`
calls `onConnectClick`, so there is no need to hunt for the button's coordinates under a keyboard.
**Disconnect returns to a login screen that still has the last attempt in its fields** (4.1): login
is state, not a route, so nothing is destroyed and the ViewModel keeps its values within the same
process. Typing then *appends* — `gregorzgregorz`, a URL glued to the previous URL — and the error
is `error_unreachable`, which reads like a dead server. Clear every field before retyping, or
screenshot first.

**Flipping night mode does not disturb the screen**, which is what makes a two-mode verify cheap:
`adb shell cmd uimode night yes` / `no` recreates the activity but keeps the back stack, the
ViewModel and any transient surface — an open `AlertDialog` and an open `ModalBottomSheet` both
survived it in 4.1. So capture a state once, flip, and capture the other mode, instead of driving
the app back to it twice.

**Since 4.3 anything about the theme has to be verified with the two *diverged*.** A dark screenshot
on a night-mode device is consistent with the stored preference working, with it being ignored, and
with the window theme alone — the three are indistinguishable. Set `cmd uimode night no` and pick
Dark (then the reverse) and each one has a single explanation. It is the same argument as 3.4's
"a screenshot of the list proves nothing about the network", and it is what the status bar needs
most: its icon tint has *two* possible sources, the resource configuration and the app, and only a
divergence says which one won.

**Since 3.4 a screenshot of the list proves nothing about the network** — those rows are Room's,
and they render identically whether the refresh succeeded, failed or never ran. **They also survive
a change of server**, which is what a step verifying against a scratch instance has to get past:
the app comes up on the *last* instance's cached rows, and a screenshot of them is a screenshot of
the wrong instance. `adb shell pm clear com.grappim.wallosmobile` and log in again is the shortest
way in (Disconnect works too — `ApiKeyStorage.clear()` evicts the cache — but it is several taps
deeper), and the stale banner ("Showing saved data") over a full list is the tell that the rows on
screen belong to a server that is no longer the one configured. To prove a request
actually happened, `adb logcat -c`, act, then read the `Ktor` lines (`REQUEST` / `RESPONSE: 200`)
— the debug build logs every call. That is the check worth running whenever a change sits under
*every* request (3.7's engine swap) even though the step's own `Verify:` line is a test task.

**Grep the request/response *lines*, never `grep Ktor`**: the logger prints whole bodies, and one
`get_subscriptions.php` response is 35 rows of JSON that buries everything else in the buffer.
`grep -E "REQUEST:|RESPONSE: |failed with exception"` is the readable form. The failure line is the
one that proves an *offline* run — `REQUEST … failed with exception: java.net.ConnectException` —
and there are **two per endpoint**, because `core:api`'s retry re-sends before giving up (3.12).

Toggling the network for an offline check:
`adb shell cmd connectivity airplane-mode enable` / `disable` — give it a few seconds either way.
`NetworkMonitor` (3.2) reacts to the flip, but **the emulator has two networks with
`NET_CAPABILITY_INTERNET`**, so `onLost` fires twice and only the second one is offline — a probe
that reads the first callback and stops will conclude the monitor is broken. Nothing *else*
re-requests on its own: a screen showing stale data keeps showing it until the next fetch.

**"Don't keep activities" is not a process-death test.** It recreates the activity inside a live
process, so anything that only breaks when the *process* is rebuilt passes it. The real check is
to background the app and kill it:

```bash
adb shell input keyevent KEYCODE_HOME && adb shell am kill com.grappim.wallosmobile
adb shell monkey -p com.grappim.wallosmobile -c android.intent.category.LAUNCHER 1
```

`am kill` keeps the task and its saved state; `force-stop` discards them, so it tests nothing.
This is the check that caught the nav3 first-composition bug — the developer option did not.

**Which of the two you want depends on where the state lives.** `force-stop` tests nothing *about
saved state* — but the Room cache is on disk, so `force-stop` + relaunch is exactly the right check
for it, and the stronger one: nothing restored, everything re-read (3.5's offline cold start). Use
`am kill` for the back stack and `rememberSaveable`, `force-stop` for anything the database owns.

**Run the cycle once, from a clean task.** Every `monkey … LAUNCHER` after an `am kill` *adds* a
`MainActivity` to the task, and once there is more than one the relaunch starts a fresh activity
instead of restoring the killed one — so the second and third cycles restore nothing and read as a
regression that isn't there (2.4 chased exactly this). `adb shell am force-stop` first to reset the
task, relaunch, get to the screen under test, and only then background + `am kill`. `numActivities`
in the `ActivityTaskManager` logcat line tells you which situation you are in — or `sz=` in
`dumpsys activity activities`, which is the same count and one grep away.

**An outbound link puts the browser in the app's own task**, which is the same dirty state (4.4):
tapping About's GitHub button leaves the task at `numActivities=2` with Chrome's launcher on top,
so a kill cycle run straight after it is testing the wrong thing. `force-stop` both, then start the
cycle. **And the link itself is a logcat check, not a screenshot** — the browser draws its
first-run page, which is identical for every URL. `adb logcat -c`, tap, then
`grep "ActivityTaskManager.*START"`: the `dat=` and `capturedLink=` fields carry the URL the app
actually asked for, which is the only thing `LocalUriHandler.openUri` is responsible for.

**Crossing a process boundary invalidates coordinates as surely as the keyboard does** (3.12). The
filter and sort live in ViewModel `MutableStateFlow`s, so a `force-stop` resets them and the list
comes back in a *different order* — tap the row you noted from the pre-kill screenshot and you open
a different subscription. Re-screenshot after every relaunch; the layout didn't move, the data did.

Rotating, which nothing before 3.12 needed:

```bash
adb shell settings put system accelerometer_rotation 0   # or the AVD ignores the next line
adb shell settings put system user_rotation 1            # 1 = landscape, 0 = portrait
adb shell settings put system accelerometer_rotation 1   # put it back when done
```

`localhost` from the emulator is the emulator, so the host is **`http://10.0.2.2:8282`**. The
Bash tool's sandbox also blocks loopback, so `curl` to `127.0.0.1`, `adb` and the emulator all
need `dangerouslyDisableSandbox`.

CI (`.github/workflows/ci.yml`, plan §3.5) runs assemble + `allTests` + `detekt ktlintCheck` on
push and PR to `master`, but `paths-ignore` skips `**.md` and `docs/**` — a docs-only commit
produces **no CI run**, which is not a failure. Kover is local-only, and so are the instrumented
Room DAO tests — `allTests` doesn't fan out to device tests and the CI job has no emulator. The second workflow,
`guardrails.yml` (plan §3.6), has no `paths-ignore` and runs no Gradle, so a docs-only commit
does produce *that* run — see "Changing a gate means saying so" above.

## Architecture

`androidApp/` → `composeApp/` (DI root, drawer shell, nav) → `feature/` → `core/` → `utils/`.
Feature modules split `data` / `domain` / `dto` / `mapper` / `ui`. MVVM + Clean Architecture,
vertical slices, **all source in `commonMain`**.

- **Shell**: `ModalNavigationDrawer` + a `TopBarController` provided through `LocalTopBarConfig`;
  each screen declares its own `TopBarConfig`. Feature `ui` modules depend on `uikit`, never on
  `composeApp`.
- **Use cases only when a screen needs multiple calls.** Single repo calls go straight from the
  ViewModel.
- **A feature grows the modules its screen actually needs**, not the `data/domain/dto/ui` set
  plan §2 lists for it. `feature:settings` is `ui` alone: Disconnect is one `ApiKeyStorage.clear()`,
  so there is no repository to hide behind a `domain` interface and the ViewModel takes the `core`
  seam directly — as `feature:subscriptions:ui` takes `BaseUrlProvider`. Add a layer when a real
  repository or a second caller turns up; a third `ui` → `core` reach is the point to ask whether
  the seam is in the right place instead.
  **3.1 is that question, answered "no".** The login screen needs the stored server URL, and
  `feature:setup` has a full `data`/`domain` layer — so the read went on `SetupRepository`
  (`getStoredServerUrl(): Result<String>`) instead of pulling `core:storage` into the `ui` module.
  The exception is for a feature with **no** layer to route through, not a licence to skip one that
  exists.
  **4.3 is the third reach, and it did not change the answer.** `InterfaceViewModel` takes
  `ThemeStorage` the same way `SettingsViewModel` takes `ApiKeyStorage`: still one call on one
  seam, still nothing for a `domain` interface to hide. **4.4's `AboutViewModel` makes three inside
  `feature:settings`** (`AppInfoProvider`) and the answer is still no — so stop counting reaches.
  What flips it is a *repository*: something with its own failure modes, callers or caching, which
  a one-call seam read straight into UI state has none of.
- **Mappers are classes, not extension functions** — one per file, for testability. Same for
  formatters: pure logic gets **no interface**. An interface here is a seam over a platform or
  over IO (`SecretCipher`, `ApiKeyStorage`, `WebLoginApi`) — something a host test can't reach.
  Faking a pure class only lets the consumer's test assert output the app never produces.
  **Such a seam returns facts, not rendered text** (4.4): `AppInfoProvider` exposes `versionName()`
  and `versionCode()` rather than Mealie's `getAppInfo(): String`, because its impl lives in
  `androidApp` — the one class no host test can construct and the one place a `:strings` resource
  can't be resolved. Pre-rendering there puts presentation where neither a test nor a translation
  can reach it.
- **Storage** is DataStore-backed (KMP), interface + impl, keys in a `private companion object`.
  The **Room cache** (`core/storage/.../db/`, 3.3) is the exception to that shape: entities and
  DAOs, no interface over them, and **every column a SQLite primitive** — no `TypeConverter`, and
  no dependency on any `feature:*:domain`, so `cycleCode` is an `Int?` and dates are ISO strings.
  Converting to domain types is a mapper's job in the feature that owns the model
  (`SubscriptionEntityMapper`, 3.4).
- **The cache belongs to the stored key, and `ApiKeyStorage.clear()` drops both** (3.4). Not a
  cleaner someone calls next to it: `clear()` has three callers — disconnect and *both* login
  paths, which clear the stale key before validating a new one — so anywhere else covers one of
  three and lets a second account see the first one's rows. A repository that caches per-account
  data adds its eviction there, in `ApiKeyStorageImpl`.
- **A repository over the cache is `observe*` + `refresh*`, never `get*`** (`SubscriptionsRepository`,
  3.4). The DAO `Flow` is the only thing a screen reads and it cannot fail; the network only writes
  to the database. So a failed refresh changes nothing but the error, and the loading spinner
  belongs to the *empty* cache alone. Such a repository also runs into detekt's
  **`allowedConstructorParameters: 6`** — two DAOs and two entity mappers on top of the API and the
  wire mappers is eight. Split the DAO half into its own class (`SubscriptionsCache`) rather than
  widening the rule; the line the limit draws is a real seam — and it keeps paying: 3.11's third DAO
  and mapper went behind it and cost the repository nothing, leaving `SubscriptionsCache` at exactly
  6. The other way past it, when the
  seventh thing has **no dependencies of its own**, is to stop injecting it: 3.10's `LoginThrottle`
  is a `private val` constructed by `SetupRepositoryImpl`, because DI was buying it nothing but a
  constructor slot — the lifetime it wanted was already its owner's.
- **A `SavedStateHandle` holds a JSON string here, not an encoded `SavedState`** (5.2). On Android
  `androidx.savedstate`'s `SavedState` **is** `Bundle`, so `encodeToSavedState`, the `saved { }`
  property delegate and `savedState { }` all need an Android runtime — unreachable from a host test,
  and Robolectric is out. `SavedStateHandle()` itself, `get`, `set` and `getMutableStateFlow` are
  pure Kotlin, so a `String` value is testable in both directions while anything Bundle-shaped is
  testable in neither. `SubscriptionFilter`'s `ImmutableSet`s have no kotlinx serializer either, so
  what gets written is a small `@Serializable` companion type (`SavedCriteria`) with plain `Set`s —
  the same DTO-beside-the-model instinct the wire layer uses. Decoding it catches
  `IllegalArgumentException`: pre-v1 stored state is disposable, but *discarding* it has to be a
  default screen rather than a crash in the ViewModel's constructor.
- **A new module must be added to the root `build.gradle.kts` `kover { }` block** — coverage is
  aggregated by an explicit per-module list, so a module left out of it is silently at 0% and
  nothing fails. `:testing` is deliberately absent (fakes, not production code).
- **Every module of a layer gets the same plugin set** — see plan §3.3 for the table and the
  standard dependency blocks. `ui` = `kmp.library` + `library.compose` + `di` + `serialization`;
  `data` = `library` + `di` + `network`; `dto` = `library` + `serialization`;
  `mapper` = `library` + `di`. Coroutines, datetime, immutable collections, `core:logger` and the
  test deps come from the convention plugins — never declare them per module. Same for the Compose
  set, including **material icons** (`Icons.Filled.*`), which material3 does *not* pull in
  transitively and which therefore lives in `configureKmpCompose()`, not in any module.
- **`material-icons-core` is ~50 icons, and the obvious one is usually missing** — no
  `Subscriptions`, no `Payment`, no `Visibility`/`VisibilityOff`, no `FilterList` (3.6, which plan
  §5.4's own top-bar sketch used), not even `Add`; `ArrowBack`,
  `List` and `Send` live under `Icons.AutoMirrored.Filled.*`. Reaching for anything else means
  adding `material-icons-extended` to `configureKmpCompose()`, so pick from the set, use a
  `TextButton` with a word in it (the login password toggle is Show/Hide text), or say you're
  growing it. To list the set without a compile: `unzip` the `material-icons-core-*.jar` from
  `~/.gradle/caches` and `ls` its `androidx/compose/material/icons/filled/` directory.
- **Material 3's own source is in the Gradle cache, and it is the authority on which colour role a
  component reads** — `unzip` `material3-desktop-*-sources.jar` from
  `~/.gradle/caches/modules-2/files-2.1/org.jetbrains.compose.material3/` and read
  `commonMain/androidx/compose/material3/tokens/*Tokens.kt` (`FilledCardTokens.ContainerColor` and
  friends) or `ColorScheme.kt` for the full parameter list of `lightColorScheme`. 4.1's step text
  said `Card` takes `surfaceContainerLow`; it takes `surfaceContainerHighest`, and this is a
  ten-second read rather than a guess.

## Non-negotiables

- Package root `com.grappim.wallosmobile`. Module namespace follows the Gradle path.
- **Pre-v1, there is nothing to be backward compatible with.** No installs exist, so don't write
  a DataStore migration, a deprecated overload, or a compatibility shim — change the thing and
  say in the commit that stored state is discarded. The serialized nav back stack and the
  DataStore contents are both disposable; a moved route class or a renamed key is free. This
  expires the day the app ships: the stored API key and the back stack are then the two things
  that need real care.
- **No `androidMain` in feature modules** — use `expect`/`actual`. Platform targets are declared
  **only** in `configureKmp()` in `build-logic`.
- **`commonMain` is not enforced platform-neutral here.** Android being the only target,
  `commonMain` compiles against the JVM variants: `java.io.File` and `kotlinx.coroutines.runBlocking`
  both resolve there and *nothing* fails. Keeping common code common is a discipline, not a
  compiler guarantee — the day a second target is declared, whatever leaked in stops compiling.
- **DI: Koin with `io.insert-koin.compiler.plugin`. Never KSP for DI.** One
  `@Module @Configuration @ComponentScan` class per module. (KSP is still used for Room.)
  **A `@Factory` reached through a `@Single` is a `@Single`** — the singleton resolves it once and
  holds that instance forever. When a `@Factory` exists for a *lifetime* reason (the
  `@WebSessionHttpClient` cookie jar, plan §1.1), every class between it and the call site has to
  be a `@Factory` too, or the reason is silently undone. Nothing fails; the object just lives too
  long.
  **`startKoin<KoinApp>` expands at the *call site***, into
  `startKoinWith(listOf(AndroidModule().module(), AppModule().module()))` — check it with `javap -c`
  on `WallosApp.class`. It gathers `@Configuration` modules only from the compilation that calls it,
  so `androidApp`'s own `AndroidModule` needs no wiring while **every module from another Gradle
  module must be in `AppModule`'s `includes`**. A forgotten line there compiles and crashes at
  first injection; `KoinGraphTest` is what catches it.
  **That call site also re-checks the definitions of every `@Configuration` class it can read** —
  `AndroidModule` and `AppModule`, not the `includes`, which reach `:androidApp` only through
  `composeApp`'s `implementation` dependencies. So a definition **declared in `AppModule` itself**
  needs its parameter types *and the definitions binding them* on `:androidApp`'s classpath, or the
  build fails with `[KOIN-D001] Missing dependency` — which is why `composeApp` declares
  `api(projects.core.storage)` for `provideImageLoader`'s `TrustedCertStorage` (4.5), and why
  `NetworkModule` takes the same type with no such ceremony. Moving the definition from a scanned
  `@Factory` class to a module function does **not** dodge it; only visibility does. Prefer putting
  a new definition in the module that already owns its dependencies.
  **The graph test uses `koin-test`'s `verify()`, never `checkModules()`** — `checkModules`
  instantiates definitions, which here means a DataStore file and an HTTP engine. `verify()` reads
  a definition through its **bound type's** constructor, so for a `@Single fun provideX(): T` it
  inspects `T`'s constructor rather than the function's parameters; that is why
  `HttpClientEngine` sits in `extraTypes` as a known false positive, next to the types
  `:androidApp` really does supply.
  **A route parameter reaching a ViewModel through `parametersOf` needs `@InjectedParam` on the
  constructor property**, or the compiler plugin looks for a definition of that type in the graph
  and the screen crashes at first injection. `KoinGraphTest` will **not** catch a missing one:
  `verify()` whitelists `String`, `Int`, `Long` and `Double` unconditionally
  (`org.koin.test.verify.Verify.primitiveTypes`), so a primitive constructor parameter passes
  whether or not it is annotated. MealieMobile takes route params without the annotation and has no
  graph test to disagree — don't read that as precedent.
  **A `SavedStateHandle` needs the same annotation, and there the graph test *is* the gate** (5.2).
  Koin builds one from the `CreationExtras` in `AndroidParametersHolder`, never from the graph, so
  an unannotated one asks for a definition that doesn't exist — and because `SavedStateHandle` is
  not one of `verify()`'s whitelisted primitives, `KoinGraphTest` fails on it instead of waving it
  through. Nothing else is needed: no factory, no `parametersOf` at the call site.
- **Navigation 3, not nav2.** `NavDisplay` + `entryProvider`; no `NavController`/`NavHost`.
  `org.jetbrains.androidx.navigation3:*` in `commonMain` — never `androidx.navigation3:*`.
  Every new route must also be registered in the polymorphic `SerializersModule` in
  `NavKeySerializers.kt`, or back-stack restore breaks silently on process death. The
  `SavedStateConfiguration` carrying it is *not* optional: `rememberNavBackStack` `require`s a
  non-default `serializersModule` and throws on the **first composition** if given
  `SavedStateConfiguration.DEFAULT` — only a *missing route* is the silent, process-death-only
  failure. **`NavKeySerializersTest` only covers the drawer destinations**: it walks
  `DrawerDestination.entries`, so a detail or editor route registered nowhere passes every gate.
  The `am kill` cycle below is the only check on those.
  **`rememberNavBackStack` consumes its restored state only in the *first* composition.** Anything
  that gates the shell on an async value — a DataStore flow, a suspend read — composes
  `NavDisplay` a pass later and the restored back stack is dropped with no error: the app comes
  back alive, on the start destination. `WallosAppContent`'s startup branch is seeded from
  `rememberSaveable` for exactly this reason. Don't put a loading state above the shell.
  **There are now two DataStore flows up there** — the startup branch and `ThemeStorage` (4.2) —
  and the second one shows the cheaper way to obey this rule: a value with a sensible default
  (`collectAsState(initial = ThemeMode.default())`) never gates anything, so it needs no
  `rememberSaveable` at all and the stored value simply arrives a frame later. Reach for the
  seeded-`rememberSaveable` shape only when there is no default that can render.
  **`MainNavHost` passes no `entryDecorators`, and that tells you nothing about ViewModel lifetime**
  (5.2). `NavDisplay`'s default is `rememberSaveableStateHolderNavEntryDecorator()` alone — no
  ViewModel-store decorator — which reads as "every ViewModel is the activity's, so opening a second
  subscription reuses the first one's ViewModel". It doesn't: on device the list ViewModel survives
  a detail round trip untouched while each detail route builds its own, and the proof is one grep —
  `adb logcat | grep "Refreshing subscription"` names the id (`4`, then `26`). Measure a lifetime
  question here; the wiring does not answer it.
- **Not everything on screen is a route.** Login isn't — the startup branch renders it *instead of*
  the shell, so it has no `NavDisplay`, no back-stack entry, and nothing to register. The test is
  whether anything can navigate *back* to it; a screen the app is either on or not is state.
- **Tests use hand-written fakes in `:testing`. No mocking library — no MockK, no Mockito,
  anywhere.** `kotlin.test` + Turbine. **This extends to the platform: no Robolectric.** Its
  shadows are mocks of Android, and the same objection applies — anything that needs a real
  Android runtime is **instrumented**. Don't propose Robolectric as the cheaper
  option; it was weighed and declined, on dependency count as much as on principle.
  **Instrumentation is no longer hypothetical**: `core/storage/src/androidDeviceTest/` holds the
  Room DAO tests (3.3), because on the Android target `Room`'s builders all take a `Context` and
  `BundledSQLiteDriver`'s native library lives in the aar's `jni/` — a host test can reach
  neither. Copy that module's `withDeviceTestBuilder { sourceSetTreeName = null }` if a second
  one is needed, and keep the `null`: the default drags `commonTest` onto the device too. There
  is **no `androidDeviceTest` accessor** in `kotlin { sourceSets { } }` the way there is for
  `commonMain`/`androidMain` — it's `getByName("androidDeviceTest").dependencies { }`, and the
  suite declares its own `kotlin("test")` because `configureTests()` only wires `commonTest`. It is
  the *last* resort, not a second option — it needs a booted emulator, `allTests` skips it and CI
  has no emulator, so an instrumented test is not a gate anyone else's commit will feel.
  **Before reaching for it, check whether `src/androidHostTest/` is enough** (3.7): `commonTest`
  can't see an `androidMain` class, but that source set can, the same `testAndroidHostTest` task
  runs it, it inherits `kotlin.test`/Turbine/`:testing` through the test tree, and `javax.*` is
  the JDK's. Its one cost is that detekt's per-rule `excludes` name `commonTest` and *not*
  `androidHostTest`, so `FunctionNaming` applies — **camelCase test names**, as in the device
  suite. Widening the exclude list would be a `config/detekt/` tripwire; a name is cheaper.
  Fake/fixture shape: plan §6.1. `:testing` is for doubles
  **other** modules need; a double used by exactly one test file stays private in that file.
  Ktor's **`MockEngine` is not a mocking library** and is fine — it's the only way to get an
  `HttpClient` in a host test, since the real engine is `androidMain`-only (autodiscovered before
  3.7, built by `createPlatformHttpClientEngine` since). It reaches every `commonTest` as `api(libs.ktor.client.mock)` in `:testing`,
  alongside `kotlinx-coroutines-test`; never declare either per module.
  **`:testing` is excluded from linting** (`lintingExclusions` in `build-logic/.../Quality.kt`,
  plus `.editorconfig`), so there is no `:testing:ktlintFormat`/`:testing:detekt` task at all —
  asking for one fails with "task not found", which is the config working, not a broken build.
  **A test fixture factory grows by `.copy()`, never by parameters.** detekt's
  `allowedFunctionParameters: 5` applies to `commonTest` too, and `ignoreDataClasses` exempts the
  data class, not the `private fun subscription(...)` that builds one — so a sixth parameter fails
  the build and the reflex fix, `@Suppress`, is a guardrail tripwire costing a `Gate-change:` line
  for a test helper. Build one canonical fixture and `.copy()` off it.
  **A fake's settable field must not be named after the method it feeds.** `var baseUrl` beside
  `override fun getBaseUrl()` is a "platform declaration clash" — the property's getter compiles to
  `getBaseUrl()` too. Name the field for what it holds (`var url`), not for the method.
- **A ViewModel test must set the main dispatcher.** `viewModelScope` dispatches on
  `Dispatchers.Main`, which a host test doesn't have, so the first `launch` throws. Use
  `MainDispatcherRule` from `:testing` — not a JUnit `@Rule` (this is `kotlin.test`), so call
  its `setup()`/`tearDown()` from `@BeforeTest`/`@AfterTest`. The exception is a ViewModel that
  never touches `viewModelScope` at all: `AboutViewModel` reads three build facts in its
  constructor, so `AboutViewModelTest` sets no dispatcher and needs no `runTest`.
  **A repository test injects `UnconfinedTestDispatcher()`, not `StandardTestDispatcher()`.**
  Every repository here takes an `@IoDispatcher` and does its work in `withContext(dispatcher)`;
  a `StandardTestDispatcher()` built outside `runTest` carries its *own* scheduler, so that
  `withContext` dies with "Detected use of different schedulers" — which surfaces as an
  `IllegalStateException` where the test expected a `WallosError`, not as anything mentioning
  dispatchers.
  **The moment the code under test `delay`s, that dispatcher must share `runTest`'s scheduler**
  (3.10): `UnconfinedTestDispatcher()` gets away with its own only because nothing was suspending
  on virtual time. Make the factory a `TestScope` extension — `private fun TestScope.repository()`
  — and pass `UnconfinedTestDispatcher(testScheduler)`; every existing `repository()` call inside
  a `runTest { }` keeps compiling, since that lambda's receiver *is* the `TestScope`. `currentTime`
  is then the assertion (`import kotlinx.coroutines.test.currentTime` — it is an extension, so it
  does not come with `runTest`).
  **`MainDispatcherRule` is unconfined, so a state that only exists *while* a call is in flight is
  invisible** — the ViewModel's `launch` runs to completion before the constructor returns, and
  `uiState.value` is already the final one. To assert a loading state, give the fake a
  `CompletableDeferred` the suspend function awaits, read the state, then `complete()` it (3.4's
  `an empty cache keeps the spinner up until the refresh answers`).
- **A `@Dao` is faked by hand like anything else** — it is an interface, so a `commonTest` fake
  needs no Room runtime, and `replaceAll` is a `@Transaction` method *with a body*, so only the
  abstract members have to be implemented. Back the fake with a `MutableStateFlow` if the code
  under test observes it: the real `@Query` `Flow` re-emits on every write, and a fake returning
  `flowOf(rows)` will pass a test that the app then fails.
- **A `commonTest` fixture is a Kotlin constant, not a file.** There is no portable way to read a
  resource or a path from `commonTest`, so recorded HTML/JSON lives in a `*Fixtures.kt` and
  anything filesystem-backed needs an in-memory fake (`FakePreferencesDataStore`, 1.4).

## UI state and events

- **State classes carry data *and* the callbacks that call into their own ViewModel**, with no-op
  defaults so previews are trivial. The ViewModel wires its own methods in at construction;
  `Content` composables call `uiState.onSomething()` and never hold a ViewModel reference.
- **Exception: pure navigation callbacks** (`onBackClick`, `onItemClick` that only call
  `navigator.navigate(...)`) stay as plain parameters on the outer `Screen` composable — they're
  wired by the `EntryProviderScope`, not by this screen's ViewModel.
- **One-off events** (navigation, snackbars, success signals) use `Channel` + `receiveAsFlow()`,
  observed with `ObserveAsEvents`. **Never** in UI state.
- Screen/Content split — `Screen` takes the ViewModel, private `Content` takes `uiState`:

```kotlin
@Composable
fun FeatureScreen(viewModel: FeatureViewModel = koinViewModel(), onBackClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    FeatureContent(uiState = uiState, onBackClick = onBackClick)
}

@Composable
private fun FeatureContent(uiState: FeatureUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) { … }

@PreviewWallosDarkLight
@Composable
private fun FeatureContentPreview() = WallosMobilePreviewTheme {
    FeatureContent(uiState = FeatureUiState(), onBackClick = {})
}
```

Naming follows MealieMobile: `FeatureUiState` / `uiState` (not Taiga's `FeatureState` / `state`).

## Compose rules

- **No early returns in Composables** — they break composition (remembered state, effects). Use
  conditional content.
- **Lambda params are present tense**: `onClick` not `onClicked`, `onItemAdd` not `onItemAdded`.
  Enforced by the `compose:parameter-naming` lint rule — **not auto-correctable, fails the build.**
- **`compose:parameter-order` exempts exactly one trailing function**, so a `Screen` composable can
  keep `viewModel = koinViewModel()` first only while it has a *single* callback. Adding a second
  fails detekt (4.4's About row on `SettingsScreen`), and the fix is the order the subscriptions
  screens already use: callbacks first, `viewModel` last with its default.
- **`ImmutableList` / `persistentListOf()`** over `List` in state classes and Composable params,
  for stable recomposition.
- **Always write previews** for screens and reusable widgets, using `@PreviewWallosDarkLight` +
  `WallosMobilePreviewTheme` (both from `uikit`). Since 4.1 the `Surface` belongs to
  `WallosMobileTheme` and the preview theme adds only composition locals, so a preview draws the
  same background and the same `LocalContentColor` as the app — a preview is now evidence about
  colour, which before 4.1 it was not. Don't add a `Surface` back into a preview.
- **A colour role left out of `lightColorScheme`/`darkColorScheme` is not derived from `surface`** —
  it falls back to Material's baseline lavender, on screen, silently. 4.1 filled in the
  surface-container ladder, `outlineVariant` and the inverse roles in `Color.kt`/`Theme.kt`; the
  `*Fixed` family is still on the baseline because nothing draws it. Adding a component means
  checking which token it reads (see the sources-jar note under Architecture) and, if that role is
  still unset, setting it rather than passing a colour at the call site.
- Fixed-item screens (settings) use `Column`, not `LazyColumn`.
- **A new `CompositionLocal` fails `ktlintCheck`** (`compose:compositionlocal-allowlist`) until it
  is named in `.editorconfig`'s `compose_allowed_composition_locals` — which is a tripwire path, so
  adding one costs a `Gate-change:` line. There are two, both provided by the shell and by
  `WallosMobilePreviewTheme`: `LocalTopBarConfig` and `LocalIsOffline`.
- Offline → **disable** write actions (`enabled = !LocalIsOffline.current`), no error dialog.
  Wallos has no permission model, so there is no "hide the action" case. The local says only that
  the *device* has a network — a LAN-only Wallos instance is "online" here and still unreachable,
  so a failed request is still the thing that reports a dead server.
  Its **second** job (3.5) is picking copy where the error would misattribute the failure: an
  offline refresh comes back as `error_unreachable` — "check the URL and your connection" — and
  `StaleBanner` overrides that reason line rather than blaming a server the request never reached.
  A preview of such a branch provides the local itself: `WallosMobilePreviewTheme` supplies
  `false`, so an offline preview wraps its content in `CompositionLocalProvider(LocalIsOffline
  provides true)` inside the theme.
- **An error over cached data is a banner, not a screen** (3.5). Two derived properties on the UI
  state, never a field the ViewModel sets: `isStale` = error *with* data (banner above rows that
  stay put), `isFailed` = error with *no* data (owns the screen, keeps the Try again button). A
  stored boolean would be a second copy of what `error` and the data field already say, free to
  drift from them — 3.5 changed no ViewModel at all.
- **The moment a screen narrows what it draws, those states must ask the *cache*, not the list**
  (3.6). `items` became the filtered view, so `items.isEmpty()` stopped meaning "nothing is
  cached" — hence `hasCachedRows`, which is a stored field and *not* a violation of the rule above,
  because the filtered list genuinely cannot express it. Skip it and a filter matching nothing
  reads as an empty instance; offline it flips the stale banner into a full-screen error over rows
  that are on the device. `isNoMatch` (rows exist, none survive the filter) is the fourth derived
  state and owes the user a Clear button, since unlike the other three it is undoable.
  The filter and sort selections themselves live in **`MutableStateFlow`s beside the UI state**,
  `combine`d with the DAO flow — one render path for "the criteria changed" and "a refresh
  arrived", instead of a second copy in the state that the next refresh could overwrite.

## Error handling

- **Never use bare `try/catch (Exception)` in coroutines** — it swallows `CancellationException`
  and breaks structured concurrency. Use `resultOf` from `core.domain`.
- **Never swallow an exception silently.** Every `catch` at minimum logs.
- `logcat { }` from `core:logger` has two overloads, and **inside a class body it always resolves
  to the `Any.logcat` extension**, which tags the line with the receiver's `simpleName`. The
  receiverless overload (tag stays `null`) is only reachable from top-level code — passing
  `tag = "…"` is the only way to override it from a class.
- Repository/use-case calls go through `resultOf`; errors reach UI state as `NativeText` via
  `getErrorMessage()`.
- **Everything that leaves `core:api` is a `WallosError`.** A decode failure becomes
  `Malformed`, not a leaked `SerializationException`, so repositories only ever match one type.
- To catch a kotlinx.serialization failure without a bare `catch (Exception)`:
  **`SerializationException` extends `IllegalArgumentException`**, as does the failure of the
  `.jsonObject` accessor — one `catch (e: IllegalArgumentException)` covers both and cannot
  swallow a `CancellationException`.
- **An untrusted certificate is not a type anyone catches** (3.7). A platform callback that
  constrains the exception *type* — JSSE's trust manager may throw only `CertificateException` —
  does not constrain its **cause**, so the portable payload rides down there and
  `Throwable.findPendingCertTrust()` (`core:domain`) walks the chain for it. That is what replaced
  TaigaMobileNova's `expect`/`actual` platform-exception mapper; reach for the same shape before
  building a second one.
  **So any "everything else" arm has to *ask*** (5.1). `getErrorMessage`'s non-`WallosError` branch
  consults `findPendingCertTrust()` ahead of `error_unreachable`, because a rotated certificate is
  the one transport failure the user can fix and "check the URL and your connection" argues against
  fixing it. `LoginViewModel.onFailure` asks the same question first and raises the trust dialog
  instead — the copy is for screens with no trust surface, so the branch is correctly invisible
  there and a test on the login path would prove nothing about it.

## Strings and resources

- Type aliases: `RString` and `RPlurals` from `:strings`, `RDrawable` from `:uikit`.
- **A quantity string is a `<plurals>` resolved in the composable**, never a string built in a
  ViewModel: `pluralStringResource(RPlurals.x, count, count)` takes the count **twice** — once to
  pick the form, once as the `%1$d` argument. So UI state carries the *number* (and whatever enum
  picks the resource), not a rendered phrase.
- A string produced outside a Composable is a **`NativeText`** (`utils:ui`), resolved with
  `asString()` at the call site. `uikit` depends on it as `api`, so any module that has `uikit`
  already has `NativeText` and must not re-declare `utils:ui`.
- Arguments use printf style in XML — `%1$d`, `%1$s`, `%2$s` — passed to `stringResource(...)`.
- **Each string is imported by name** (`import …strings.generated.resources.login_connect`), so a
  new one fails as `Unresolved reference 'x'` **at the `RString.x` usage**, not at an import line.
  That reads exactly like stale codegen — it isn't; add the import. Don't reach for
  `--rerun-tasks` (which is the right move for Koin, and the wrong one here).
- **Do not escape apostrophes.** CMP resources are not Android XML; `\'` renders literally.
  Write `isn't`.

## The Wallos API will surprise you

Read `docs/WALLOS_API.md` before touching anything network-related — and treat it as **derived,
not complete**. It was written from the PHP, so where a step's `Ref:` points at it, that is a
starting point rather than a contract: 3.9's `totp.php` section listed the request and the success
case and simply had no row for the branch that mattered (a lost session), which would have shipped
as an infinite retry loop had it been implemented from the doc alone. `docker exec wallos cat
/var/www/html/<file>.php` is a read against the real thing and takes seconds; do it for any
endpoint whose *failure* modes the code has to branch on. 3.10 is the second time it paid, and it
widens the rule: read it before adding a **request** to a flow that already holds a session, not
only before branching on a response. A GET of `login.php` looks inert and silently clears
`$_SESSION['totp_user_id']`, so an unguarded probe would have killed live 2FA logins — nothing in
the doc, nothing in the response, and no test would have caught it.

**3.11 is the third time, and it widens the rule furthest: the doc can be actively wrong, and a
`success: true` is the dangerous case.** §5.5 said to detect an unconverted price by comparing
`currency_id` against `main_currency`. The PHP shows conversion overwrites `price` **alone** and
leaves `currency_id` naming the source currency, so the two responses are identical and the
documented check reports "converted" for a row that wasn't — a wrong answer implemented confidently.
So: **read the PHP for any response whose *meaning* the code has to infer, not only its failure
modes**, and treat a `Ref:` line into `WALLOS_API.md` as the previous session's reading rather than
as the server. Where a step's premise turns out inverted, correcting the doc is part of the step.

- **Everything returns HTTP 200**, including auth failures. Only the JSON `success` field is
  reliable. Never branch on HTTP status except 404 (endpoint missing on this version) and 5xx.
- **Auth is a static API key in the form body**, not a header. No login endpoint, no token, no
  refresh. Username/password onboarding works by driving the *web* login and scraping the key
  (plan §1.1).
- **PHP `display_errors` prepends HTML to valid JSON.** Never call `.body<T>()` — always go
  through `WallosEnvelopeParser`.
- **No pagination anywhere.** Filtering and sorting are client-side.

- **Text fields come back HTML-escaped** — a subscription named `1&1 Telekom` is
  `1&amp;1 Telekom` on the wire, and it renders that way unless the mapper unescapes it. Same for
  `notes`, the resolved `category_name` / `payer_user_name` / `payment_method_name`, and currency
  `name` / `symbol`. `HtmlUnescaper` (`feature:subscriptions:mapper`) is the one place that
  reverses it; use it from any new mapper rather than writing a second one. It decodes `&amp;`
  **last** on purpose — decode it first and `&amp;lt;`, which is the literal text `&lt;`, becomes
  a `<` the user never typed.
- **Unset dates are `""`, not `null`** (`cancellation_date`, `start_date`), so a nullable field is
  not enough — blank has to read as absent.

Two that bite later: `cycle=5` (One-time) is readable but **rejected on write**, and
`Unauthorized or Not Found` is a per-row ownership error that must **not** clear the stored key.

### There is a live instance, and it is the only one to test against

`docs/local-info.txt` holds the URL and credentials for the user's own Wallos container
(`http://localhost:8282`, i.e. `http://10.0.2.2:8282` from the emulator). It is committed on
purpose — the instance is LAN-only, the user has said so explicitly, so don't re-raise it as a
leak. **Every on-device `Verify:` line means this instance.**

Its data has holes worth knowing before planning a verify: **`notes` and `url` are `""` on every
subscription**, and `start_date` is `""` on a good few, so anything rendering those fields can only
be proven by unit test and preview. Pick the row deliberately — `Fiton` (id 4) has a start date and
a `&` in its category name.

**It is also single-currency, with conversion off and rates never fetched** (3.11): all 35 rows are
`currency_id = 1` (EUR, the main currency), `settings.convert_currency = 0`, `last_exchange_update`
is empty and all 32 rates are exactly `1`. So nothing about currency conversion can be seen here at
all — not the working case, not the failing one. That needs a scratch container, which is one
`cp -a` of the database plus two `UPDATE`s.

**That container exists, stopped, as `wallos-scratch` on port 8284** (5.3) — a `bellamy/wallos` over
a copy of the live database with 8 of the 35 rows moved to `currency_id = 2` and the copied key
rotated to `5c3aa1de…7b8c`. `docker start wallos-scratch`, then `http://10.0.2.2:8284`. Rates are
still all `1`, so it shows conversion *off* over mixed currencies and not conversion working; that
needs an `UPDATE currencies SET rate = …` on top. Copying the database in is three commands, since
the files are uid 82 and the container's are not a volume:

```bash
docker run --rm -v /home/gregory/data/wallos/db:/src:ro alpine cat /src/wallos.db > wallos.db
docker cp wallos.db wallos-scratch:/var/www/html/db/wallos.db
docker exec wallos-scratch chown www-data:www-data /var/www/html/db/wallos.db
```

**It speaks plain HTTP, so certificate work needs a TLS front for it** (3.8, and 3.12 again).
Don't touch the Wallos container — put a throwaway nginx beside it. The whole recipe:

```bash
openssl req -x509 -newkey rsa:4096 -sha256 -days 3650 -nodes \
  -keyout ca-key.pem -out ca-cert.pem -subj "/CN=Home Lab Test CA/O=Wallos QA"
openssl req -newkey rsa:2048 -nodes -keyout server-key.pem -out server.csr \
  -subj "/CN=10.0.2.2/O=Wallos QA"          # CN *and* SAN are the address the app is given
echo "subjectAltName=IP:10.0.2.2" > san.cnf # no SAN → the cert doesn't cover the host → no prompt
openssl x509 -req -in server.csr -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial \
  -out server-cert.pem -days 825 -sha256 -extfile san.cnf
# nginx.conf: listen 8443 ssl; ssl_certificate /certs/server-cert.pem; … proxy_pass http://wallos:80;
docker run -d --name wallos-tls --network wallos_default -p 8443:8443 \
  -v $(pwd)/certs:/certs:ro -v $(pwd)/nginx.conf:/etc/nginx/conf.d/default.conf:ro nginx:alpine
```

Then the app's server URL is `https://10.0.2.2:8443`, and **never install the CA on the emulator**
— an untrusted chain is the whole point. `docker rm -f wallos-tls` when done. Regenerating the
leaf and restarting the container is what proves a pin is per-*certificate* and not per-host; it
is marked optional in Taiga's recipe and it is the check that found 3.8's real gap.
Conscrypt preserves the cause chain, confirmed on device — the failure arrives as
`SSLHandshakeException: …UntrustedCertificateException`, which is what makes
`findPendingCertTrust()` work at all.

**The front's own access log is the proof for anything the Ktor logger can't see** (4.5). Image
loads go through Coil, which has no `Logging` plugin, so `adb logcat` says nothing about them —
`docker logs wallos-tls | grep images/uploads/logos` does, and the `"ktor-client"` user-agent on a
`200` is what says the load used the app's pinned engine rather than a stack that would have failed
the handshake. **Coil caches on disk**, so a second run proves nothing until
`adb shell run-as com.grappim.wallosmobile rm -rf /data/data/com.grappim.wallosmobile/cache/coil3_disk_cache`
— that path, and `pm clear` for everything at once. A failed load is also **sticky per request**:
Coil does not retry a state that is already `Error`, so after the server comes back the rows keep
their placeholders until they recompose (scroll them off and on).

**A step that wants a server-side *setting* changed gets a throwaway instance, not the user's**
(3.9). Enabling 2FA, disabling password login, seeding a second account — all of them mutate live
data and some have a lockout tail.

**For 2FA that instance already exists and is set up**: `wallos-totp` on port 8283, a copy of the
real database with `gregorz` 2FA-enabled, the same password, the same 35 subscriptions and its own
API key. It is **stopped by default** — `docker compose -f /home/gregory/data/wallos-totp/compose.yaml
up -d`, then `http://10.0.2.2:8283` from the emulator and `python3 scripts/totp-code.py` for a
code. Full details, backup codes and the re-seed command: `docs/local-info.txt`. Stop it when done;
don't `docker rm` it, the point is that the setup survives.

**For any other server-side setting**, build a scratch one the same way:

```bash
docker run -d --name wallos-scratch -p 8284:80 bellamy/wallos:latest   # ~10s to boot
curl -s -o /dev/null -d "username=u&firstname=T&lastname=U&email=u@e.com&password=p&\
confirm_password=p&main_currency=USD&language=en" http://localhost:8284/registration.php
```

**OIDC needs no identity provider to test against** (3.10): `login.php` only reads
`password_login_disabled` when OIDC is enabled *and* `is_configured`, and `is_configured` is a
non-empty check on seven strings that are never dereferenced unless somebody clicks the button. So
env vars alone produce a real SSO-only instance — `-e OIDC_ENABLED=1 -e
OIDC_DISABLE_PASSWORD_LOGIN=1` plus `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET`, `OIDC_AUTH_URL`,
`OIDC_TOKEN_URL`, `OIDC_USERINFO_URL`, `OIDC_REDIRECT_URL` set to anything at all.

`registration.php` needs **all** of those fields — a short POST silently re-renders the form and
`login.php` keeps redirecting to `registration.php`, which reads as a broken container. Copying the
real data instead of registering is one `cp -a` of `/home/gregory/data/wallos/db` through a root
container (the files are uid 82), and it is worth it for anything that renders a list. From there
`docker exec <name> php -r '…new SQLite3("/var/www/html/db/wallos.db")…'` writes whatever the step
needs directly, skipping the enrolment endpoints' own CSRF + code dances. PHP sessions are plain
files — `rm -f /tmp/sess_*` inside the container is how you make a live session expire mid-flow.
**Rotate any copied API key**, or a disposable container is holding a working credential for the
real instance.

**Reading the PHP out of the container is the fastest authority on any endpoint** —
`docker exec wallos cat /var/www/html/totp.php` settled a response table `WALLOS_API.md` had left
half-written, and it is a read, so the live container is fine for that.

**Do not reach for `demo.wallosapp.com`.** Its `profile.php` dies with a PHP fatal
(`no such table: uploaded_avatars`), so there is no `id="apikey"` to scrape and the Path A login
bridge can never succeed there. References to it were removed from the docs in 1.11.

### The same instance is behind the `wallos` MCP

`mcp__wallos__*` reaches a **real Wallos v5.4.2**, and it is the **user's own personal instance** —
`gregorz`, user id 1, real subscriptions. Read tools (`wallos_get_version`, `wallos_get_user`,
`wallos_list_*`, `wallos_get_subscription`, `wallos_get_monthly_cost`, `wallos_get_period_budget`)
are free to call and are the fastest way to settle a question `docs/WALLOS_API.md` leaves open —
it was derived from PHP source, so the MCP is the check on it. **`wallos_add_subscription`,
`wallos_update_subscription`, `wallos_delete_subscription` and `wallos_set_budget` mutate the
user's live data — ask first, every time.**

Three limits worth knowing before leaning on it:

- **It returns the payload unwrapped** — `wallos_get_user` gives `{"user": {…}}` with no `success`
  or `title`. So it confirms **real values**, and says nothing about the envelope behaviour
  `core:api` is built around (HTTP 200 on failure, PHP prefixes, the `title` catalogue). Those
  still need `curl` against `api/*.php` directly, per §8's smoke test.
- **It adds fields the API does not have.** `wallos_list_subscriptions` returns a `logo_url` that
  exists nowhere in the PHP — the server sends the bare `logo` filename. So it is *not* an
  authority on field shapes either: **model DTOs against `curl`, and use the MCP for values.**
- **It is one instance at one version.** A field present here may be absent on the older installs
  the app has to tolerate — `ignoreUnknownKeys` and nullable DTO fields are still the rule.

Getting a key for that `curl`, since the app is the only other thing that has one:

```bash
curl -s -c /tmp/w.txt -o /dev/null \
  -d "username=gregorz&password=$(grep -A1 '^gregorz$' docs/local-info.txt | tail -1)" \
  http://localhost:8282/login.php
curl -s -b /tmp/w.txt http://localhost:8282/profile.php | grep -o 'id="apikey"[^>]*'
```

## Reference projects

Read these rather than guessing — the conventions here are ported from them.

- `/home/gregory/proj/grappim/TaigaMobileNova` — module structure, `build-logic` convention
  plugins, networking, `:testing` fakes.
- `/home/gregory/proj/grappim/MealieMobile` — nav3 (`docs/kmp-nav3.md` plus the real code in
  `composeApp/.../nav/`), drawer shell, `uikit/.../widgets/topappbar/`, `core/api/NetworkModule.kt`,
  and the Screen/ViewModel/Route/DI templates in its `CLAUDE.md`.

Two known drifts in those repos: `MealieMobile/docs/kmp-nav3.md` disagrees with its own code
(plan §5.5), and Mealie's `CLAUDE.md` Tech Stack line says Koin uses KSP — its `build-logic` shows
the compiler plugin.

## Coding guidelines

**Tradeoff:** these bias toward caution over speed. For trivial tasks, use judgment.

### Think before coding
Don't assume. Don't hide confusion. Surface tradeoffs. State assumptions explicitly; if multiple
interpretations exist, present them rather than picking silently. If a simpler approach exists,
say so. If something is unclear, stop and name what's confusing.

### Simplicity first
Minimum code that solves the problem, nothing speculative. No features beyond what was asked, no
abstractions for single-use code, no unrequested "flexibility", no error handling for impossible
scenarios. If you write 200 lines and it could be 50, rewrite it.

### Surgical changes
Touch only what you must. Don't "improve" adjacent code, comments, or formatting. Don't refactor
what isn't broken. Match existing style even if you'd do it differently. Don't add UI or
navigation that wasn't asked for. Remove orphans *your* change created; mention pre-existing dead
code rather than deleting it. **Every changed line should trace directly to the request.**

### Goal-driven execution
Turn tasks into verifiable goals — "fix the bug" → "write a failing test, then make it pass". For
multi-step work, state the plan as steps with a verify check each, then loop until they pass.
