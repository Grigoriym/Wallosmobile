# Proposal: "Don't keep activities" is not a process-death test — `am kill` is

## What

Two different Android failures get called "process death", and the developer option only
exercises one of them:

| Check | What it rebuilds | What it catches |
|---|---|---|
| Settings → **Don't keep activities** | the Activity, inside a **live process** | `onSaveInstanceState` / `rememberSaveable` round-trips |
| `adb shell am kill <pkg>` on a **backgrounded** app | the whole **process** | the above, *plus* anything that depends on composition or init order on a cold rebuild |

Anything that resolves asynchronously during startup — a DataStore read, a repository call, a
splash gate — behaves differently between the two, because on a live-process recreate the async
value is often already warm and the tree composes in one pass.

The command sequence, which is the part worth keeping:

```bash
adb shell input keyevent KEYCODE_HOME          # must be backgrounded; am kill won't touch foreground
adb shell am kill com.example.app              # keeps the task record and its saved state
adb shell monkey -p com.example.app -c android.intent.category.LAUNCHER 1
```

`am kill` is the right verb. **`am force-stop` discards the task's saved state**, so restoring
after it proves nothing — the app legitimately starts fresh. Confirm the kill landed by watching
the pid: `adb shell pidof com.example.app` before and after.

### The bug that motivated this

In a Navigation 3 app, `rememberNavBackStack` consumes its restored state **only in the first
composition**. A startup branch that waited on a DataStore flow before composing `NavDisplay`
pushed the shell into a second pass, and the restored back stack was silently dropped — the app
came back alive, on the start destination, with no exception anywhere. "Don't keep activities"
passed the whole time. Only `am kill` reproduced it.

The fix is to make the branch resolvable in the first pass (seed it from `rememberSaveable`), but
the transferable part is the *test*: a verify line that says "survives process death" should name
`am kill`, not the developer option.

## Why shared

Not a WallosMobile fact — it is a property of the Android platform and of `rememberSaveable`, and
it applies to any project here with an async gate above its navigation host. MealieMobile has the
same shape (nav3 + a login-vs-shell startup branch), and any future Compose app will. The
checklist wording that missed it ("enable Don't keep activities → process death restores the right
screen") is wording that reads as thorough and isn't, which is exactly the kind of thing worth
fixing once in a shared place rather than per project.

## Target

Whichever shared skill covers running/verifying an Android app on an emulator — the same one that
documents the headless boot + `screencap` loop. This is an **addition**: a short "verifying state
restoration" subsection, plus a caution that `force-stop` is the wrong verb.

If no such skill exists yet, this belongs alongside the emulator commands wherever they live.

## Suggested text

> **Verifying state restoration.** "Don't keep activities" recreates the Activity inside a live
> process — it tests `onSaveInstanceState` and nothing else. To rebuild the *process*, background
> the app first and kill it:
>
> ```bash
> adb shell input keyevent KEYCODE_HOME
> adb shell am kill <pkg>        # NOT force-stop — that discards the saved state
> adb shell monkey -p <pkg> -c android.intent.category.LAUNCHER 1
> ```
>
> Check `adb shell pidof <pkg>` before and after to confirm the process actually changed. Use this
> whenever startup resolves something asynchronously (DataStore, a repository, a splash gate):
> composables that appear a pass *later* than they did at save time can silently fail to restore,
> and the developer option will not show it.

Also worth carrying, from the same session: **`adb shell input keyevent KEYCODE_TAB` moves Compose
focus between fields.** Filling a form by tapping coordinates breaks as soon as the keyboard opens
and shifts the layout, and a mis-tap silently types into the wrong field.

## Source

WallosMobile, 2026-08-01, checklist step 1.11 (wiring the Koin graph and the login-vs-shell
startup branch end to end).
