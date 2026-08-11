# MASVS register

Profile: Android (Android-only for now, per `CLAUDE.md`) · self-hosted, user-supplied server ·
reviewed 2026-08-11, STORAGE only so far (M17, `docs/CHECKLIST.md`).

Out of scope: **MASVS-RESILIENCE** — scope decision deferred to 17.8, not made here.

## Accepted deviations

| Control | What we do instead | Bound | Why |
|---|---|---|---|
| MASVS-STORAGE-2 | `android:allowBackup="true"` on the release manifest (`androidApp/src/main/AndroidManifest.xml:9`, set since the app's first commit, `d3d01da`), no `android:dataExtractionRules`/`android:fullBackupContent` at all — confirmed by grep across every `AndroidManifest.xml` in the repo | The value that would be carried by a backup is never plaintext: `ApiKeyStorageImpl.setKey` runs every write through `SecretCipher.encrypt` (`KeystoreSecretCipher`, AES/GCM) before it reaches DataStore (`ApiKeyStorageImpl.kt:35-40`), so the on-disk bytes in `wallos_storage.preferences_pb` are ciphertext, not the key. The AES key never leaves the Android Keystore (`KeystoreSecretCipher.getOrCreateKey`, `KeystoreSecretCipher.kt:62-75`) — Keystore-resident key material is excluded from `allowBackup`/cloud backup by the platform itself and does not travel with the app's data directory, so a restored backup on any other device carries only inert ciphertext. `SecretCipher.decrypt`'s own doc comment (`SecretCipher.kt:16-19`) states this exact scenario as an anticipated, handled state — a restored backup, a reinstall, or an invalidated key all return `null`, treated as "no key," and the user re-onboards rather than the app sending a broken key or crashing. All non-sensitive prefs (`theme_mode`, `crash_reporting_enabled`, `start_destination`) and `server_url` share the *same* single DataStore file as `api_key` (`StorageModule.kt:20`, `STORAGE_FILE_NAME = "wallos_storage"`) — Android's file-level backup exclusion can't carve out just the `api_key` entry the way a dedicated auth-only DataStore file could, so excluding the file to protect an asset that's already inert would also silently drop the harmless prefs on every backup/restore, for no real security gain | The asset is encrypted before it ever reaches the file a backup would carry, and the key that would decrypt it is bound to the Keystore instance, not the app data directory — `allowBackup` leaking this file leaks nothing usable. Unlike Taiga's pre-fix state (a *plaintext* token in the backed-up file), there is no plaintext exposure here to close, and closing the file at the granularity actually available (whole-file, not per-key) costs real UX (losing theme/start-destination/server URL on every restore) for a security property the cipher already provides |
| MASVS-STORAGE-2 | `ServerUrlStorageImpl` stores a bare Wallos instance URL in the same shared plaintext DataStore file (`server_url` key, `ServerUrlStorageImpl.kt:36`) | The value is only ever what the user types on the login screen and is passed straight through (`SetupRepositoryImpl.probePasswordLogin`/`loginWithPassword`: `serverUrlStorage.saveServerUrl(serverUrl.trim())`, no parsing that would accept or strip embedded userinfo) — `ServerUrlStorage`'s own doc comment (`ServerUrlStorage.kt:4`) frames it as "the Wallos instance root," and Wallos's actual auth model (`CLAUDE.md`'s "Wallos API will surprise you": a static API key in the form body, no HTTP Basic/userinfo-in-URL auth anywhere in the API) gives the user no reason to type a URL with embedded credentials in the first place. No code path constructs a URL with userinfo | Reveals which Wallos instance the user talks to, not a credential — the same reasoning and the same MASVS-STORAGE-2 row Taiga recorded for its own `DataStoreServerStorage` |

## Open

(none)

## Needs a device or an APK

| Control | Check | Why source can't answer it |
|---|---|---|
| MASVS-STORAGE-1 / MASVS-CRYPTO-2 | Whether `KeystoreSecretCipher`'s AES key is actually hardware-backed (TEE/StrongBox) as opposed to a software-only Keystore fallback, and whether a real `adb backup`/cloud backup + restore onto a second physical device produces ciphertext that genuinely fails to decrypt (confirming the Accepted-deviation bound above holds in practice, not just in the doc comment's stated intent) | Hardware enforcement and real backup/restore behaviour aren't verifiable from source; `KeyGenParameterSpec` in `KeystoreSecretCipher.kt:66-73` doesn't request `setIsStrongBoxBacked`, so whether the key ends up hardware-backed at all depends on the device |

## Notes

- **Logs**: `grep -rnE '(logcat|Log\.|println).{0,120}(token|apiKey|api_key|password|secret|cookie|Authorization|key)'`
  across all source sets (excluding tests) returns only the three `KeystoreSecretCipher` warning
  logs (`:36`, `:40`, `:54`) — all fixed strings describing the *failure mode* ("Stored key is not
  valid base64", "…too short to hold an IV", "…could not be decrypted"), none interpolate the key
  value itself or the exception's message in a way that could carry ciphertext/plaintext. Verified
  statically; no call site logs the API key or the login password.
