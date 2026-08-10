# WallosMobile — Emulator testing

Project-specific facts for the `emulator-testing` skill (`agentic-grappim`, symlinked at
`~/.claude/skills/emulator-testing`). Generic adb/`uiautomator` technique — coordinate
scaling, dump gotchas, `am kill` vs `force-stop`, DataStore-planting mechanics, form
filling, network toggling — lives in that skill, not here. This file is only what's
true about *this* app: its ids, its AVD, and the gotchas found by verifying its own
screens.

Backend/server-side test fixtures (the scratch currency-conversion instance, the TLS
front, the 2FA instance, the live `gregorz` account) are **not** emulator-testing
technique and stay documented in `CLAUDE.md`'s "The Wallos API will surprise you"
section and `docs/local-info.txt` — this file only covers the device side.

## Device facts

- AVD: `Medium_Phone_API_36.1`
- Package ids: `com.grappim.wallosmobile.debug` (gplay, no suffix — use this day to day),
  `com.grappim.wallosmobile.fdroid.debug` (fdroid — substitute `installFdroidDebug`)
- Activity: `com.grappim.wallosmobile.MainActivity` (fully-qualified form required once
  the id carries the fdroid suffix)
- Backend for on-device `Verify:` lines: `http://10.0.2.2:8282` (the emulator's view of
  the host's `localhost:8282`) — creds in `docs/local-info.txt`. The Bash tool's sandbox
  blocks loopback, so `curl`, `adb` and the emulator process all need
  `dangerouslyDisableSandbox`.

## App-specific gotchas

- **`screencap` is 1080×2400 while the returned image is scaled** — multiply screenshot
  coordinates by the display factor before `input tap`, or better, read a
  `uiautomator dump`'s real-pixel `bounds` instead. Mixing scaled and unscaled
  coordinates mid-session is silent: no error, the tap just lands on whatever was
  underneath.
- **A `DateField`'s own placeholder** (e.g. "Next payment", "Start date" on the
  subscription editor) **is invisible to a dump text search while empty** — unlike a
  plain `OutlinedTextField`'s placeholder, which shows up fine. The transparent
  click-overlay `Box` used for `next_payment`/`start_date` (`DatePicker`/
  `DatePickerDialog`) seems to swallow that semantics node. Compute the field's centre
  from the screenshot or from neighboring fields' spacing instead of trusting an empty
  grep.
- **`DatePickerDialog`'s calendar day is a `content-desc`** ("Friday, August 14, 2026"),
  not the visible digit's `text` — grep the digit and you'll hit the wrong node.
- **An `ExposedDropdownMenuBox` anchor's clickable region is the whole field box**, label
  to bottom border, not a narrow strip around the value.
- **Setup/login screen fields grow mid-flow**: a cleartext-HTTP warning and an SSO
  notice can appear *between* the URL field and the credentials fields once the URL is
  typed, so coordinates read off the initial screenshot are stale by the time
  credentials are entered. Re-screenshot (or use `uiautomator dump`) after typing the
  URL, before tapping further down the form.
- **Disconnect returns to a login screen that still has the last attempt in its
  fields** — login is state, not a route, so nothing is destroyed and typing again
  *appends* to what's already there (`gregorzgregorz`, a URL glued to the previous URL).
  The resulting error (`error_unreachable`) reads like a dead server, not like a stale
  field. Clear every field (`input keycombination 113 29` then `KEYCODE_DEL`) before
  retyping, or screenshot first to check.
- **A fresh boot of `Medium_Phone_API_36.1` can pop a "Try out your stylus" tutorial**
  over the first field tapped, eating every subsequent tap with no error. It has its own
  Cancel control, not a system back gesture — screenshot right after a cold boot before
  trusting typed input landed in the app.
- **A screenshot of the subscriptions list proves nothing about the network** — those
  rows are Room's and render identically whether the last refresh succeeded, failed, or
  never ran, and they survive a change of server (the app opens on the *previous*
  server's cached rows). `adb shell pm clear com.grappim.wallosmobile.debug` and log in
  again is the fastest way onto a known-clean state when verifying against a different
  Wallos instance. The **stale banner** ("Showing saved data") over a full list is the
  tell that on-screen rows belong to a server that's no longer the configured one.
- **Proving a request happened**: `adb logcat -c`, act, then
  `grep -E "REQUEST:|RESPONSE: |failed with exception"` — never `grep Ktor` alone, since
  one `get_subscriptions.php` response body is ~35 rows of JSON that buries everything
  else. An offline attempt logs `failed with exception:
  java.net.ConnectException` **twice per endpoint** (`core:api`'s retry re-sends once
  before giving up).
- **Airplane-mode toggling** (`adb shell cmd connectivity airplane-mode enable`/
  `disable`) fires the app's `NetworkMonitor` callback **twice** on this emulator — it
  carries two networks with `NET_CAPABILITY_INTERNET`, and only the second `onLost` is
  the real offline transition.
- **`am kill` vs `force-stop`**: use `am kill` (keeps task + saved-instance-state) to
  test the nav3 back stack and `rememberSaveable` — this is the check that caught the
  nav3 first-composition bug, where "Don't keep activities" did not. Use `force-stop`
  (discards both) to test anything Room owns, since it forces a real cold read from
  disk. Filter/sort selections live in ViewModel memory, so a `force-stop` resets them
  and the list can come back in a different order — a coordinate noted before the kill
  may open a different subscription after.
- **Relaunch after `am kill` with `am start -n
  com.grappim.wallosmobile.debug/com.grappim.wallosmobile.MainActivity`, not
  `monkey … LAUNCHER`.** `MainActivity` has no `launchMode` in the manifest (`standard`),
  and `am start -n` gets Android's task-reuse treatment for that case — it prints
  `Warning: Activity not started, its current task has been brought to the front` and
  resumes the same activity record (`sz=` stays 1), which is what actually exercises
  restore. `monkey` does not get this treatment and starts a **new** activity instance on
  top on every call, including the very first one after a kill (confirmed 11.1: `pidof`
  empty after kill, then one `monkey` call took `sz=` 1→2) — the app then reopens on
  `DashboardRoute`, the start destination, which reads exactly like a back-stack-restore
  bug but is really the relaunch technique creating a fresh activity rather than
  restoring the killed one. Re-run with `am start -n` before concluding restore is
  broken.
- **Run a kill cycle once, from a clean task**, regardless of relaunch command:
  `adb shell dumpsys activity activities | grep com.grappim.wallosmobile` (`sz=`) or the
  `ActivityTaskManager` logcat line's `numActivities` says whether the task is already
  dirty. `adb shell am force-stop com.grappim.wallosmobile.debug` resets it. Tapping
  About's GitHub link leaves the same kind of dirty two-activity task behind —
  `force-stop` both the app and the browser before starting a kill cycle that follows a
  link tap. The link itself is a logcat check, not a screenshot (the browser's own
  first-run page looks the same for every URL): `adb logcat -c`, tap, then
  `grep "ActivityTaskManager.*START"` for the `dat=`/`capturedLink=` fields.
- **Theme check is a pixel, not an impression**:
  `python3 -c "from PIL import Image; print(Image.open('shot.png').convert('RGB')
  .getpixel((540, 220)))"` — `(26, 27, 31)` is `SurfaceDark`, `(253, 251, 255)` is
  `SurfaceLight`. Since dark/light can come from either the stored preference or the
  system default, verify with the two **diverged** (`cmd uimode night no` + pick Dark in
  app, then the reverse) — an agreeing pair proves nothing about which one the app reads.
- **The local instance's `get_categories`/`get_household`/`get_payment_methods` response
  time is highly variable run to run** (measured 10ms to ~700ms across otherwise-identical
  attempts against `docker exec wallos`) — don't trust a single timed screenshot attempt
  against these three to represent typical latency, and don't try to time a screenshot to
  land inside their loading window by `sleep`ing a fixed delay after the tap; cut
  connectivity first instead (skill Step 4b) so the window is deterministic.
- **Planting a DataStore key before its UI exists** (`wallos_storage.preferences_pb` is
  an unchecksummed protobuf `map<string, Value>` that merges on repeated writes):

  ```bash
  python3 -c "
  import base64
  def ld(b): return bytes([len(b)])+b
  v=b'\x2a'+ld(b'dark'); e=b'\x0a'+ld(b'theme_mode')+b'\x12'+ld(v)   # key, then Value.string
  print(base64.b64encode(b'\x0a'+ld(e)).decode())"
  adb shell "run-as com.grappim.wallosmobile.debug sh -c 'echo <b64> | base64 -d >> \
    /data/data/com.grappim.wallosmobile.debug/files/datastore/wallos_storage.preferences_pb'"
  ```

  Force-stop first, or the running app overwrites the file on its own next write.
- **`api_key` cannot be planted this way** — `ApiKeyStorageImpl` encrypts it with
  `KeystoreSecretCipher` (AES/GCM, a key that never leaves the Android Keystore), so a raw planted
  value fails to decrypt and is silently treated as "no key stored" rather than crashing (the
  cipher's catch arms are deliberately lenient). A logged-in state needs a real pass through
  `LoginScreen` against `docs/local-info.txt`'s instance — there is no shortcut for this one key.
- **Launcher icon lives in the app drawer**: `adb shell input swipe 540 1800 540 600`
  pulls it up on this AVD's resolution; the swipe silently no-ops about as often as it
  works, so screenshot after every attempt and retry rather than trusting one call.
- **Picking a gallery image for the logo-upload flow** needs the file media-scanned
  first (`adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d
  file:///sdcard/Pictures/x.jpg` after `adb push`) and, on this AVD's picker, an explicit
  **Done** tap after selecting the thumbnail — tapping the thumbnail alone only checks
  it.
