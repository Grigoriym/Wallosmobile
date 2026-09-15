# Stored API key reads as invalid after upgrading an existing install

**Status:** Done
**Link:** none (reported directly by the user)   **Updated:** 2026-09-12

## Update — upstream fix landed in grappim-kit

Flagged to the `grappim-watcher` Claude session (owns `grappim-kit`). It confirmed the read and
pushed a fix to `grappim-kit` `main` (commit `63f5493`, not yet published to Maven Central):
`KeystoreSecretCipher` gains a second constructor param,
`legacyUnprefixedIsPlaintext: Boolean = true`. Default `true` preserves today's behavior (needed
by TaigaMobileNova, whose own pre-swap ciphertext already carried the `v1:` convention — confirmed
against its `AndroidKeystoreTokenCipher.kt`, so the prefix/passthrough design is correct for that
consumer). Passing `false` makes `decrypt()` attempt every unprefixed value as real ciphertext
instead of passing it through as plaintext — exactly Option 1 below, implemented as a kit-level
flag rather than a wallosmobile-local shim. `grappim-kit/CONSUMING.md`'s `storage` section now
documents the correction (the original swap's "harmless, no live installs yet" assessment was
wrong and is flagged as such there).

**This changes the Impact assessment below**: `3ba6e6d` (the commit that introduced the bug) never
reached `master`/a tagged release — confirmed `git merge-base --is-ancestor 3ba6e6d master` is
false, and `v1.0.3` is still the latest tag. So **no real installed user has hit this yet**; it
was only ever live on `dev`. That also means there is no already-corrupted stored value to worry
about in the field: the bug is a runtime *misread* of an otherwise-intact ciphertext, nothing ever
overwrites the stored value with garbage, so once wallosmobile wires up
`legacyUnprefixedIsPlaintext = false` a pre-swap key just decrypts correctly — no forced relogin
or migration step needed for anyone, since nobody outside `dev` testing has been exposed.

Remaining work: `grappim-kit-storage` needs a version bump + publish (Central artifacts are
immutable per version, so this is a real, deliberate release action) before wallosmobile's
`StorageModule.kt` can pass the new param — worth asking the user before doing that, same as any
other kit publish.

## Report

User-reported, verbatim: "I am right now doing a sanity check after all our changes, and what I
noticed is that when I install the current dev on top of the latest release, i see this in
responses: `{"success":false,"title":"Invalid API key","notes":["User not found or API key
invalid."]}` though it is fixed with the relogin."

- **Symptom:** installing a `dev` build over an existing install of the latest tagged release
  (`v1.0.3`) — i.e. an upgrade, not a fresh install — produces a real server response of
  `title: "Invalid API key"` on (at least) one request. Manually logging in again (Settings →
  disconnect, or re-running onboarding) makes it go away.
- **Environment:** Android, upgrade path `v1.0.3` → current `dev`, real API key stored from the
  prior install (not a fresh onboarding).
- **Reporter's diagnosis:** none given — only the symptom and the workaround.

The report doesn't say which screen was open when the response appeared, or whether an error
banner/screen was visible before the relogin — see Open questions.

## Findings

- Commit `3ba6e6d` ("0.N — swap onto grappim-kit-storage/trustmanager:0.1.4 (#81)",
  2026-09-11, landed after `v1.0.3` was tagged — confirmed
  `git merge-base --is-ancestor 3ba6e6d v1.0.3` returns false) retargeted `core:storage`'s
  `SecretCipher`/`KeystoreSecretCipher` from wallosmobile's own local implementation onto the
  published `grappim-kit-storage` module's `KeystoreSecretCipher`
  (`/home/gregory/proj/grappim/grappim-kit/storage/src/androidMain/kotlin/com/grappim/kit/storage/KeystoreSecretCipher.kt`).
- The kit's `KeystoreSecretCipher` changed the stored ciphertext format to add a `"v1:"` prefix.
  Its `decrypt()` (that file, lines 37-38):
  ```kotlin
  override fun decrypt(value: String): String? {
      if (!value.startsWith(CIPHERTEXT_PREFIX)) return value
  ```
  Any stored value with no `v1:` prefix is treated as pre-existing plaintext and returned
  **unchanged**, not decrypted. This is a deliberate, documented policy in the kit (same file,
  lines 20-22): *"A value with no [CIPHERTEXT_PREFIX] is a plaintext value written before this
  cipher existed — decrypt passes it through unchanged... a consuming app migrates it to
  ciphertext the next time it's written."* Correct in general for a consumer that really did once
  store plaintext; wrong for wallosmobile specifically (next bullet).
- wallosmobile's own pre-swap `KeystoreSecretCipher`, confirmed still present verbatim in the
  shipped `v1.0.3` tag (`git show v1.0.3:core/storage/.../KeystoreSecretCipher.kt`), used:
  - the same Keystore alias, `"wallos_api_key"`
  - the same transformation, `AES/GCM/NoPadding`
  - the same IV length (12 bytes) and GCM tag length (128 bits)
  - the same stored form, `Base64(iv || ciphertext)`, just **without** any prefix.

  So a key stored by a `v1.0.3` install is real AES-GCM ciphertext, byte-for-byte compatible with
  the new cipher's algorithm and (since Android Keystore keys persist across an app update that
  doesn't change the alias or uninstall the app) the same underlying key material — it is only
  missing the `v1:` string marker the new code uses to recognize its own format.
- `ApiKeyStorageImpl.storedKey` (`core/storage/src/commonMain/kotlin/.../ApiKeyStorageImpl.kt:26-28`):
  ```kotlin
  private val storedKey: Flow<String?> = dataStore.data.map { prefs ->
      prefs[KEY_API_KEY]?.let(secretCipher::decrypt)
  }
  ```
  On an upgraded install, this calls `decrypt()` on the old, unprefixed base64 ciphertext. Per the
  finding above, `decrypt()` returns that raw base64 string unchanged, treating garbage bytes as
  if they were the literal API key.
- `ApiKeyStorage.isConnected` (`core/storage/src/commonMain/kotlin/.../ApiKeyStorage.kt:12-17`) is
  documented to read a key that "no longer decrypts" as **not connected**, "which sends the user
  back to onboarding rather than into a stream of `Unauthenticated` responses." That path is not
  taken here: `decrypt()` didn't return `null`, it returned a bogus non-null, non-blank string, so
  `isConnected` reports `true` and the app proceeds to the main shell rather than onboarding —
  the opposite of what that doc comment says should happen for an undecryptable key.
- Every subsequent authenticated request sends this garbage string as `api_key`
  (`FormParamsExtension.withApiKey`), the server correctly rejects it, and `WallosErrorMapper`
  (`core/api/src/commonMain/kotlin/.../WallosErrorMapper.kt:6,31`) maps the `"Invalid API key"`
  title to `WallosError.Unauthenticated` — exactly the response text the user quoted.
- **No automatic recovery exists for this state.** Grepped every call site of
  `ApiKeyStorage.clear()` (excluding tests/doc comments): `SettingsViewModel.kt:36` (manual
  "disconnect" action) and `SetupRepositoryImpl.kt:66,128` (both login paths clear the *old* key
  before validating a *new* one, per that file's own doc comment). Nothing clears the key
  automatically in response to a `WallosError.Unauthenticated` outside of those two user-initiated
  flows. So the garbage key stays stored, `isConnected` keeps reporting `true`, and every screen
  that calls the API keeps failing with the same error until the user manually goes through
  Settings → disconnect (or re-onboards), which is what "fixed with the relogin" describes — a
  manual recovery, not an automatic one.
- This was an acceptable "pre-v1, nothing to migrate" discard per `CLAUDE.md`'s pre-v1
  no-backcompat rule at the time of the swap, and `3ba6e6d`'s own commit message reasons exactly
  that way ("Pre-v1, nothing to migrate: existing installs' stored keys are discarded on this
  swap"). But `v1.0.0`–`v1.0.3` are real shipped tags (`git tag` lists them, dated back to
  2026-08-x), so that premise no longer held on 2026-09-11 when the swap landed — every real
  install that had a stored key before the swap hits this on update to a build containing it.

## Root cause

`3ba6e6d` swapped wallosmobile's local `KeystoreSecretCipher` for `grappim-kit-storage`'s version,
which introduced a `"v1:"` format prefix and treats any unprefixed stored value as plaintext
rather than ciphertext needing this new prefix. wallosmobile's actual pre-swap stored values are
real AES-GCM ciphertext under the identical key alias/algorithm/format (just unprefixed), not
plaintext — so on any install that already has a key stored from before the swap, the new
`decrypt()` returns the raw ciphertext string as if it were the API key. That garbage value passes
the app's own "is a key stored" check (`isConnected`), so the app proceeds as logged-in and sends
the garbage value to the server, which correctly responds `Invalid API key`. No code path clears
this automatically — only a manual disconnect/re-login does, which is why relogin "fixes" it.

## Impact

Hits every real user who updates from a `v1.0.x` install (Play Store or F-Droid) to any build
built from `dev` at or after `3ba6e6d` — i.e. this is a real regression for actual installed
users, not a dev-only concern, and is the release channel's very next update. Effect: after
updating, the app appears logged in but every authenticated API call fails with an "Invalid API
key" error until the user notices and manually disconnects/logs back in. No data loss (Room cache
untouched, real key just needs re-entry/re-scrape), but a confusing, unexplained-looking break on
update for anyone who doesn't already know to reconnect. No in-app messaging currently tells the
user *why* — they'd see generic `Unauthenticated`/"Invalid API key" error content
(`error_invalid_api_key` string, per `GetErrorMessage.kt:33`) wherever a screen first calls the
API, not a "please reconnect" prompt specific to this cause.

## Open questions

- Exactly which screen/error affordance the user actually saw first (a banner over cached data, a
  full error screen, or just the raw response visible in a debug/network log) — doesn't change the
  root cause, but affects how bad the on-screen UX is right now. Not blocking a decision.
- Whether any other stored, encrypted value in the app (only the API key is stored via
  `SecretCipher` today — confirmed by grep, `ApiKeyStorageImpl` is the sole caller of
  `SecretCipher.encrypt`/`decrypt`) needs the same treatment. Currently: no, this is the only one.

## Options

1. **Add a wallosmobile-side compatibility read in `ApiKeyStorageImpl`.** When the raw DataStore
   value doesn't start with `v1:`, first try decrypting it as if it did (prepend the prefix before
   calling `secretCipher.decrypt`) — since alias/algorithm/IV/tag length are identical, this should
   succeed for a genuine pre-swap key. On success, treat that as the real key and re-persist it via
   `setKey()` in the new prefixed format (self-healing the stored value going forward). Only fall
   back to the kit's plaintext-passthrough behavior if that decrypt attempt fails.
   - Pros: fixes the actual regression for every real existing install with one release; no user
     action required; matches the app's own documented intent that an undecryptable key should
     read as "not connected," while a *decryptable* pre-swap key keeps working transparently, which
     is the strictly better outcome the "not connected → onboarding" fallback was never meant to
     be a substitute for.
   - Cons: this is app-specific historical knowledge (that wallosmobile's own pre-swap format is
     byte-compatible with the kit's) that has to live in wallosmobile, not in the shared kit — a
     small permanent piece of migration logic that has no equivalent anywhere else in the codebase
     yet (`CLAUDE.md`'s pre-v1 stance never needed one before). Only fixes *this* app; doesn't
     change the kit's own documented (and reasonable, for other consumers) passthrough behavior.
   - Blast radius: `core:storage`'s `ApiKeyStorageImpl` and its test only.

2. **Do nothing — treat it as an acceptable one-time break, same as any other pre-v1 stored-state
   discard, and let affected users relogin once.**
   - Pros: zero code change; consistent with the general pre-v1 policy that's applied to every
     other stored-state change so far.
   - Cons: the pre-v1 policy's own premise (no real installs) is false here — real users on
     `v1.0.0`–`v1.0.3` exist right now and would hit a real, confusing break on their very next
     update, with no explanation in the UI of why. This is a materially worse experience than a
     typical pre-v1 discard (which the app is designed to survive cleanly via "not connected →
     onboarding"); here it's "silently broken until you notice and manually fix it."
   - Blast radius: none (no change), but reputational/support cost among the small real user base.

3. **Detect the specific "connected but every call fails with Unauthenticated" state and
   auto-clear the key**, so the app at least drops the user cleanly into onboarding instead of a
   silently broken "connected" shell — without attempting to recover the original key.
   - Pros: much smaller change than option 1; turns a confusing broken state into the same clean
     "please reconnect" experience `isConnected` already promises for an undecryptable key.
   - Cons: doesn't actually fix the loss — the user still has to find and re-enter/re-scrape their
     API key, only now sooner and with a clearer prompt; conflates "genuinely revoked key"
     (working as designed today) with "this specific migration gap," which are different situations
     that happen to produce the same error title; doesn't self-heal the actual stored-format
     mismatch, only masks its symptom.
   - Blast radius: wherever `Unauthenticated` is currently surfaced as an error message would need
     new auto-clear plumbing; touches more call sites than option 1 for a worse outcome.

**Recommendation: Option 1.** It is the only option that actually preserves a real user's stored
credential across this specific update instead of forcing a re-login, it's a small and fully
contained change (`ApiKeyStorageImpl` only), and it turns the migration gap into a one-time,
invisible self-heal rather than a visible break — which matters because real users are on the
other end of this update now, not just the dev-only scenario the original swap assumed.

## Decision

**Option 1**, implemented as a `grappim-kit-storage` flag rather than a wallosmobile-local shim
(see the Update above) — chosen by the user 2026-09-12: "yes, fix it."

## What landed

- `grappim-kit-storage` bumped to `0.1.6` (published, commit `bb086c1`) in `gradle/libs.versions.toml`.
- `StorageModule.provideSecretCipher()` (`core/storage/src/androidMain/kotlin/.../StorageModule.kt`)
  now passes `legacyUnprefixedIsPlaintext = false` to `KeystoreSecretCipher`, so `decrypt()`
  attempts every unprefixed stored value as real ciphertext instead of treating it as legacy
  plaintext.
- Verified: `./gradlew :core:storage:compileAndroidMain :androidApp:compileGplayDebugKotlin` and
  `:core:storage:testAndroidHostTest detekt ktlintCheck` all pass unchanged (the existing
  `ApiKeyStorageImplTest` suite uses a fake `SecretCipher`, not the real Keystore-backed one, so it
  exercises `ApiKeyStorageImpl`'s own logic but not this specific flag — the flag's behavior is the
  kit's own responsibility and is documented/tested there, not duplicated here).
- Deliberately **not** verified on-device (an upgrade repro: install `v1.0.3`, log in, then
  install-over with this fix and confirm no relogin is needed) — skipped at the user's request,
  who will check it directly after reviewing the PR.
- No migration or forced-relogin path was needed: `3ba6e6d` (the commit that introduced the bug)
  never reached `master`/a tagged release, so no real installed user was ever exposed to it.
