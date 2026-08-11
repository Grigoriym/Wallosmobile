# MASVS register

Profile: Android (Android-only for now, per `CLAUDE.md`) · self-hosted, user-supplied server ·
reviewed 2026-08-11, all eight MASVS categories (M17, `docs/CHECKLIST.md`) — STORAGE, CRYPTOGRAPHY,
NETWORK, AUTHENTICATION, PLATFORM, CODE and PRIVACY reviewed in full; RESILIENCE excluded, reason
below.

Out of scope: **MASVS-RESILIENCE** — anti-tamper/anti-reverse-engineering protects a vendor's
assets (an API key baked into the client, business logic worth obfuscating) against the device
owner. WallosMobile is a FOSS client for a server the *same* user self-hosts and owns — there is no
vendor asset here for the device owner to be the adversary of, and the app embeds no secret of its
own for RESILIENCE to protect: confirmed by grep, no `client_secret`/`CLIENT_SECRET`/`client_id`
anywhere in source, `build-logic`, or `gradle/libs.versions.toml`, and no OAuth flow exists in the
app at all (`WebLoginApi.kt`/`ApiKeyScraper.kt` drive a plain POST/GET + regex scrape against the
user's own server, not a third-party OAuth exchange with a registered client secret) — an even
faster N/A than TaigaMobileNova's own task 7 found, since that app at least had an OAuth client id
to check against. Reproducible builds are the property that would actually matter for a FOSS
client's supply-chain trust, and MASVS does not cover them.

## Accepted deviations

| Control | What we do instead | Bound | Why |
|---|---|---|---|
| MASVS-STORAGE-2 | `android:allowBackup="true"` on the release manifest (`androidApp/src/main/AndroidManifest.xml:9`, set since the app's first commit, `d3d01da`), no `android:dataExtractionRules`/`android:fullBackupContent` at all — confirmed by grep across every `AndroidManifest.xml` in the repo | The value that would be carried by a backup is never plaintext: `ApiKeyStorageImpl.setKey` runs every write through `SecretCipher.encrypt` (`KeystoreSecretCipher`, AES/GCM) before it reaches DataStore (`ApiKeyStorageImpl.kt:35-40`), so the on-disk bytes in `wallos_storage.preferences_pb` are ciphertext, not the key. The AES key never leaves the Android Keystore (`KeystoreSecretCipher.getOrCreateKey`, `KeystoreSecretCipher.kt:62-75`) — Keystore-resident key material is excluded from `allowBackup`/cloud backup by the platform itself and does not travel with the app's data directory, so a restored backup on any other device carries only inert ciphertext. `SecretCipher.decrypt`'s own doc comment (`SecretCipher.kt:16-19`) states this exact scenario as an anticipated, handled state — a restored backup, a reinstall, or an invalidated key all return `null`, treated as "no key," and the user re-onboards rather than the app sending a broken key or crashing. All non-sensitive prefs (`theme_mode`, `crash_reporting_enabled`, `start_destination`) and `server_url` share the *same* single DataStore file as `api_key` (`StorageModule.kt:20`, `STORAGE_FILE_NAME = "wallos_storage"`) — Android's file-level backup exclusion can't carve out just the `api_key` entry the way a dedicated auth-only DataStore file could, so excluding the file to protect an asset that's already inert would also silently drop the harmless prefs on every backup/restore, for no real security gain | The asset is encrypted before it ever reaches the file a backup would carry, and the key that would decrypt it is bound to the Keystore instance, not the app data directory — `allowBackup` leaking this file leaks nothing usable. Unlike Taiga's pre-fix state (a *plaintext* token in the backed-up file), there is no plaintext exposure here to close, and closing the file at the granularity actually available (whole-file, not per-key) costs real UX (losing theme/start-destination/server URL on every restore) for a security property the cipher already provides |
| MASVS-STORAGE-2 | `ServerUrlStorageImpl` stores a bare Wallos instance URL in the same shared plaintext DataStore file (`server_url` key, `ServerUrlStorageImpl.kt:36`) | The value is only ever what the user types on the login screen and is passed straight through (`SetupRepositoryImpl.probePasswordLogin`/`loginWithPassword`: `serverUrlStorage.saveServerUrl(serverUrl.trim())`, no parsing that would accept or strip embedded userinfo) — `ServerUrlStorage`'s own doc comment (`ServerUrlStorage.kt:4`) frames it as "the Wallos instance root," and Wallos's actual auth model (`CLAUDE.md`'s "Wallos API will surprise you": a static API key in the form body, no HTTP Basic/userinfo-in-URL auth anywhere in the API) gives the user no reason to type a URL with embedded credentials in the first place. No code path constructs a URL with userinfo | Reveals which Wallos instance the user talks to, not a credential — the same reasoning and the same MASVS-STORAGE-2 row Taiga recorded for its own `DataStoreServerStorage` |
| MASVS-NETWORK-1 | Custom TOFU `X509TrustManager` on Android (`CompositeTrustManager`, `core/api/src/androidMain/kotlin/com/grappim/wallosmobile/core/api/CompositeTrustManager.kt`) | Falls through to the platform default trust manager first — TOFU is only reached from the `catch (e: CertificateException)` arm after `defaultTrustManager.checkServerTrusted` has already rejected the chain (`:73-82`). Before offering trust it requires the presented leaf's SAN (or CN fallback) to match the connecting host (`hostMatchesCertificate`, `:98-114`) — a host/cert mismatch throws the original `CertificateException` instead of offering TOFU (`:80`). The pin key is `(host, sha256Fingerprint)` (`TrustedCertStorageImpl.entry`, `core/storage/.../cert/TrustedCertStorage.kt:41`) — per-**certificate**, not per-host: a regenerated cert on an already-trusted host does not match the stored entry and re-triggers TOFU rather than being silently accepted. A pin hit still re-checks validity (`leaf.checkValidity()`, `CompositeTrustManager.kt:69`), so an accepted cert that has since expired is still rejected. All three properties are covered by dedicated host tests in `core/api/src/androidHostTest/kotlin/com/grappim/wallosmobile/core/api/CompositeTrustManagerTest.kt`: `unpinnedCertificateIsLeftToTheDeviceTrustStore` (falls through), `aPinDoesNotFollowTheCertificateToAnotherHost` + `aPinDoesNotCoverASecondCertificateFromTheSameHost` (per-cert, not per-host), `aPinnedCertificateThatHasSinceExpiredIsStillRejected` (validity still enforced). Trust is only ever granted from an explicit user action — `LoginViewModel.onCertTrustConfirm()` (`feature/setup/ui/.../LoginViewModel.kt:290-303`), wired to a modal dialog's Confirm button, is the only call site that reaches `SetupRepositoryImpl.trustCertificate` → `TrustedCertStorage.trust`; nothing calls `trust` automatically. No custom `HostnameVerifier` exists anywhere in the repo (`grep -rn 'HostnameVerifier'` empty), so platform hostname verification is untouched by this override — it replaces certificate-chain trust only, as the class's own doc comment states. One gap: the storage interface has no revoke method and there is no in-app UI to remove an accepted pin — filed as `docs/revisit.md` #1, not fixed in this review (not a live security hole, since a rotated cert on the same host fails the fingerprint match and re-triggers TOFU rather than silently keeping the old trust) | Self-hosted Wallos instances commonly run self-signed certs; this is a bounded TOFU implementation — falls through to the system store, requires a hostname match before ever offering trust, pins per-certificate, still checks expiry, and only ever activates from an explicit user confirmation — not a naive trust-everything override |
| MASVS-NETWORK-1 | `android:usesCleartextTraffic="true"` (`androidApp/src/main/AndroidManifest.xml:15`), no `android:networkSecurityConfig` scoping it (grep across every manifest in the repo found none) | Applies to every host, not restricted to LAN/dev addresses. `WallosApiClient.post`/`postMultipart` (`core/api/.../WallosApiClient.kt:34-40,46-67`) attach the API key as a form-body parameter (`params.withApiKey(apiKeyStorage.getKey())`) on every call, regardless of scheme — so the key is sent in the clear if `baseUrlProvider.getBaseUrl()` resolves to an `http://` instance, and `BaseUrlProviderImpl` does no scheme upgrade or rewriting. Two things bound it: the key travels in the request **body**, never a URL query parameter, so it never lands in server access logs or browser-style history/referrer headers even over cleartext; and `RedactingLogger` (`core/api/.../RedactingLogger.kt:21`) strips `api_key`/`apiKey`/`password` values from Ktor's own request/response logging before it reaches logcat, so a cleartext capture is limited to the actual network hop, not also duplicated into the device's own logs. `LoginUiState.isCleartextWarningVisible` (`feature/setup/ui/.../LoginUiState.kt:81-83`) warns the user once, at login time, specifically on the password path (`!isApiKeyMode`) — the reasoning documented right there (`:69-70`) is that a POSTed *password* is worse than a pasted API key, so the warning steers toward Path B rather than blocking either. It does not re-warn for the API key itself or for ongoing post-login traffic — the same shape and the same partial-coverage gap Taiga recorded for its own bearer-token case, and not worth a new finding beyond noting the parallel | Self-hosted LAN Wallos instances commonly run plain HTTP; scoping cleartext off entirely would block the only instance this project can test against (`CLAUDE.md`'s local-instance note) |
| MASVS-NETWORK-2 | No identity pinning for "endpoints under the developer's control" | N/A by construction — the server is user-supplied, not developer-operated (the TOFU mechanism above exists for a different reason: user-approved trust, not a developer-mandated pin) | Per the control's own qualifier |
| MASVS-PLATFORM-3 | No `FLAG_SECURE` anywhere in the codebase (`grep -rn 'FLAG_SECURE'` empty across every source set; `MainActivity.kt` never calls `window.setFlags`) — the recents-list thumbnail can capture whatever is on screen, including a revealed password on `LoginScreen.kt:231-263` if the app is backgrounded mid-reveal | Requires the user to actively tap the Show toggle (`LoginUiState.isPasswordVisible` defaults `false`, `LoginUiState.kt:33`, and `PasswordVisualTransformation` is the field's default state, `LoginScreen.kt:239-243`) **and** background the app at that exact moment; the recents thumbnail is local to the device only (not synced/uploaded), so exploiting it needs physical/local access to an already-unlocked device | Explicit product decision, not an oversight — confirmed with the user (2026-08-11): setting `FLAG_SECURE` on this single-`Activity` app would block screenshots/screen-recording app-wide, not just on login, which they don't want as a user of their own app. Weighed against a Low-severity, local-access-only gap on one field that's hidden by default, they chose to keep screenshot capability over closing it — the identical tradeoff and reasoning TaigaMobileNova's maintainer made for the same control |
| MASVS-AUTH-1 | No server-side login lockout or rate limiting — `LoginThrottle` (`feature/setup/data/.../LoginThrottle.kt`) is a client-side-only exponential backoff (2 free attempts, then doubling to an 8s cap — `waitAfter`, `:58-62`) | Confirmed from the PHP that Wallos itself has neither lockout nor rate limiting on `login.php` or `totp.php` (`LoginThrottle.kt:9-13`'s own doc comment). The counter lives only as long as the `@Factory`-scoped `WebLoginApi`/`LoginThrottle` instance — one login screen (`SetupRepositoryImpl.kt:41`'s doc comment) — so it resets on process death or a fresh screen, and it is trivially bypassed by anything that calls Wallos's own `login.php`/`totp.php` directly rather than through this app. It only grows on a **refused** attempt, never a transport failure (`SetupRepositoryImpl.refused`, `:113-116`), so a flaky network is never punished | It is the only mitigation available to a client for a server that enforces none of its own, not a real access control — a courtesy against this app itself being turned into a brute-force tool against the user's own instance, not a guarantee against a brute-force attempt made any other way |
| MASVS-CODE-1 | `minSdk = "24"` (`gradle/libs.versions.toml:22`, Android 7.0/2016) | `git log --follow -p` shows the value came from `TaigaMobileNova`'s own catalogue at this project's first commit (`1ae21af2`'s equivalent here — "0.2: replace the wizard's catalog with TaigaMobileNova's ... minSdk 24") and has never changed since. No README/docs line in this repo states an independent rationale — same absence Taiga's own MASVS-CODE-1 row recorded for the value it was ported from | A wide device-support floor is a reasonable product choice for a FOSS client with no attacker-relevant platform-security gap named against it; recorded so the floor is a decision on record, not silence — identical reasoning to Taiga's row for the same number |
| MASVS-CODE-2 | No forced update on either flavour. **Gplay** prompts a Play In-App Update (`AppUpdateCheckerImpl.kt`, `androidApp/src/gplay/kotlin/.../di/AppUpdateCheckerImpl.kt:50`) using `AppUpdateType.FLEXIBLE` only, never `IMMEDIATE` — a dismissible nudge. **F-Droid** has no update-check mechanism at all (confirmed: no equivalent file under `androidApp/src/fdroid/`) | Gplay: a user can dismiss the flexible prompt and keep using an outdated build indefinitely. F-Droid: update delivery is entirely up to the F-Droid client/repo, outside this app's control | Standard for both distribution channels, and already the shape M16 shipped (16.5) rather than something this review changed — F-Droid has no update-enforcement API to call, and forcing updates against a Play flexible-update dismiss would be unusual UX for a self-hosted client with no vendor asset at risk beyond the user's own account |
| MASVS-CODE-3 | `renovate.json` — Renovate handles Gradle-ecosystem dependency-version PRs and sets `"osvVulnerabilityAlerts": true`, opening PRs when a catalogue dependency has a known OSV.dev advisory, independent of GitHub's own Dependabot alerts | Confirmed the flag is actually present (not just a `renovate.json` existing, which alone wouldn't turn OSV scanning on under `config:recommended`) | The self-contained fix for "nothing checks the catalogue against an advisory feed" — same control, same fix Taiga's own MASVS-CODE-3 task added, already in place here since M14 rather than new this session |
| MASVS-CODE-4 | Server-response deserialization tolerance: `WallosEnvelopeParser`'s one `Json` instance (`core/api/.../WallosEnvelopeParser.kt:103-106`) sets `ignoreUnknownKeys = true` and `isLenient = true`, and it is the *only* JSON config in the app — `NetworkModule.kt` installs no Ktor `ContentNegotiation` plugin at all (confirmed by grep; responses are read as raw strings and passed through this one parser), so there is no second, differently-configured deserializer anywhere for a response to hit | A malformed or evolving self-hosted server response (missing field, extra field added by a newer Wallos version, PHP's `display_errors` HTML prefix) fails soft — `decodeEnvelope`'s prefix-stripping and the `catch (e: IllegalArgumentException)` around both parse steps turn any decode failure into `WallosError.Malformed` rather than a crash | Standard, safe deserialization posture for a client whose server is explicitly allowed to sit at a different migration level (`CLAUDE.md`'s "no pagination" section) |
| MASVS-CODE-4 | `LocalUriHandler.openUri()` — exactly two call sites exist in the entire repo (`grep -rn 'LocalUriHandler\|openUri' --include=*.kt .`, excluding `build/`), both in `AboutScreen.kt:81,88`, neither behind a scheme allowlist | Both URLs are build-time-fixed string resources, never user- or server-supplied text: `projectUrl` reads `RString.about_project_url` (`AboutScreen.kt:60`) and `privacyPolicyLink` is picked between `RString.privacy_policy_url`/`privacy_policy_url_gplay` by `AboutViewModel` from `crashReporter.isAvailable` — a build/flavor fact, not a field on any UI state a screen writes (`AboutUiState.kt:17`, `AboutViewModel.kt:28-32`). There is no markdown renderer in the dependency catalogue (confirmed: no `multiplatform-markdown-renderer`/similar in `gradle/libs.versions.toml`) and no other feature screen calls `openUri` at all, so there is no second call site carrying user- or server-controlled text the way Taiga's custom-field URL and markdown links did | A materially different starting shape than Taiga's own MASVS-CODE-4 finding, not the same gap re-found: nothing here ever feeds attacker- or collaborator-controlled text into `openUri`, so the `SafeUriHandler` allowlist Taiga built to close its finding has no analogous input to guard against in this app today. Worth re-checking if a future screen ever renders server-supplied text as a clickable link |

## Open

(none)

## Needs a device or an APK

| Control | Check | Why source can't answer it |
|---|---|---|
| MASVS-STORAGE-1 / MASVS-CRYPTO-2 | Whether `KeystoreSecretCipher`'s AES key is actually hardware-backed (TEE/StrongBox) as opposed to a software-only Keystore fallback, and whether a real `adb backup`/cloud backup + restore onto a second physical device produces ciphertext that genuinely fails to decrypt (confirming the Accepted-deviation bound above holds in practice, not just in the doc comment's stated intent) | Hardware enforcement and real backup/restore behaviour aren't verifiable from source; `KeyGenParameterSpec` in `KeystoreSecretCipher.kt:66-73` doesn't request `setIsStrongBoxBacked`, so whether the key ends up hardware-backed at all depends on the device |
| MASVS-CRYPTO-2 | Confirm the AES key `KeystoreSecretCipher.getOrCreateKey()` generates is actually 256-bit — `KeyGenParameterSpec.Builder` (`KeystoreSecretCipher.kt:66-73`) never calls `.setKeySize()`, so the real size comes from the Android Keystore provider's default for `KEY_ALGORITHM_AES`, not from a value in our own code | The size the provider actually enforces isn't visible from source; `KeyInfo.getKeySize()` on the real generated key, on a real device, is the only way to confirm it rather than trust the documented default |
| MASVS-NETWORK-1 | Whether `CompositeTrustManager`'s per-certificate pin actually holds at the TLS layer against a live regenerated leaf (regenerate the cert on an already-trusted host, restart, confirm the app objects and re-offers TOFU rather than connecting silently or crashing) | Source and the host-test suite confirm the pin *key* is `(host, sha256Fingerprint)` and that a fingerprint mismatch fails the storage-layer lookup, but whether OkHttp/JSSE actually re-invokes `checkServerTrusted` and surfaces `UntrustedCertificateException` correctly through to the login dialog on a live regenerated cert needs a device or a throwaway TLS front (`docs/local-info.txt` has one) |
| MASVS-CODE-3 | Whether Renovate's `osvVulnerabilityAlerts` has actually opened a PR against a real known-vulnerable dependency in the catalogue | Needs a live Renovate run against this repo (or a deliberately-pinned vulnerable test dependency) — not verifiable from a static config read. Separately confirmed via `gh api repos/Grigoriym/Wallosmobile/vulnerability-alerts` → `404` ("Vulnerability alerts are disabled"): GitHub's own native Dependabot alerts are OFF at the repo-settings level, same result Taiga's own review found for its repo. That's an optional, independent lever (repo Settings → Code security) the user could flip, not required now that OSV coverage is configured via Renovate, and not something this step changes |

## Notes

- **Logs**: `grep -rnE '(logcat|Log\.|println).{0,120}(token|apiKey|api_key|password|secret|cookie|Authorization|key)'`
  across all source sets (excluding tests) returns only the three `KeystoreSecretCipher` warning
  logs (`:36`, `:40`, `:54`) — all fixed strings describing the *failure mode* ("Stored key is not
  valid base64", "…too short to hold an IV", "…could not be decrypted"), none interpolate the key
  value itself or the exception's message in a way that could carry ciphertext/plaintext. Verified
  statically; no call site logs the API key or the login password.
- **Cryptography (MASVS-CRYPTO-1/2)**: `KeystoreSecretCipher.getOrCreateKey()`
  (`KeystoreSecretCipher.kt:62-75`) generates the AES key through
  `KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")` with
  `PURPOSE_ENCRYPT or PURPOSE_DECRYPT`, `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE` (correct — GCM
  is unpadded) and no `.setRandomizedEncryptionRequired(false)`, so the platform *enforces* — not
  merely defaults to — a provider-generated IV on every `ENCRYPT_MODE` init: a caller cannot supply
  or reuse a fixed IV for this key even if the code tried to. `encrypt()` (`:24-29`) never passes an
  `IvParameterSpec`/`GCMParameterSpec` into `init`, so `cipher.iv` is always a fresh
  Keystore-generated 96-bit nonce, stored as the first 12 bytes (`IV_LENGTH_BYTES = 12`) of the
  base64 payload; `decrypt()` (`:32-60`) reads that same nonce back into a `GCMParameterSpec(128,
  ...)` — a 128-bit tag, the maximum/recommended length, not the weaker 96-bit some implementations
  use. The key never leaves the Keystore: `getOrCreateKey()` returns the `SecretKey` handle straight
  into `Cipher.init`, and nothing in the file calls `.getEncoded()` or otherwise exports/serializes
  it. `grep -rniE '(SecretKeySpec|IvParameterSpec|byteArrayOf.*0x)'` across every source set found no
  hand-rolled key/IV material anywhere else in the repo. A separate grep for embedded credentials in
  source, build config and the version catalogue (`gradle/libs.versions.toml` has no key/secret/
  password entries; `AndroidApplicationConventionPlugin.kt:37-54` pulls release-signing passwords
  from `System.getenv("WALLOS_*")`, never a literal) turned up only test fixtures
  (`WallosApiClientTest.kt`, `LoginViewModelTest.kt`, `WallosHtmlFixtures.kt`) and one Compose
  preview default (`LoginScreen.kt:508`, `apiKey = "5c1e0b2a9f"`) — none shipped key material. No
  `.setUserAuthenticationRequired(true)` on the key is deliberate, not an oversight: it lets
  background API calls decrypt the stored key without a biometric prompt, and MASVS-AUTH-2/3 are
  already N/A for this app (no biometric anywhere, confirmed again in 17.4), so there is no step-up
  gate for this key to bind to. The one thing source can't settle — the actual generated key size,
  since `KeyGenParameterSpec` never calls `.setKeySize()` — is in the Needs-a-device table above
  rather than asserted here.
- **Authentication (MASVS-AUTH-1)**: the login bridge's credential handling, checked file:line
  against `WebLoginApi.kt`/`ApiKeyScraper.kt`/`LoginThrottle.kt`/`SetupRepositoryImpl.kt`. The
  password is held only in `LoginUiState.password`, a plain in-memory `MutableStateFlow` field
  (`LoginViewModel.kt:46-59`) — not `SavedStateHandle`-backed, since login is explicitly not a
  route (`CLAUDE.md`'s "Not everything on screen is a route") and carries none of that mechanism's
  JSON-string persistence. It reaches the network exactly once, as a Ktor `submitForm` body
  parameter (`WebLoginApiImpl.login`, `WebLoginApi.kt:76-85`), over the same trust-aware
  `HttpClient` engine the rest of the app uses (`NetworkModule.provideWebSessionHttpClient`,
  `NetworkModule.kt:68-88`) with `RedactingLogger` stripping `password=`/`api_key=`/`apiKey=` from
  Ktor's own request logging before it reaches logcat (`RedactingLogger.kt:21`) — confirmed by the
  same repo-wide credential-logging grep 17.2 already ran, which found no call site interpolating
  the password value. `password` is a local parameter throughout — `login()` never assigns it to a
  field, and nothing in `WebLoginApiImpl`/`SetupRepositoryImpl` persists it to DataStore, Room, or
  a log call. The UI state field is actively cleared once it's no longer needed: on a successful
  connect (`LoginViewModel.onConnected`, `:256-258`) and the moment a TOTP challenge supersedes it
  (`onTotpRequired`, `:242-244`, whose own comment states why — "the password has already been
  accepted ... so it goes now"); it is deliberately *not* cleared on an `InvalidCredentials` refusal,
  since the user is expected to see and correct what they typed — that's the field staying on
  screen for editing, not a persistence gap. **Scrape target is always the user's own configured
  server**: `WebLoginApiImpl`'s calls resolve through `BaseUrlProviderImpl.getBaseUrl()`
  (`BaseUrlProviderImpl.kt:15-18`), which reads `ServerUrlStorage.serverUrl` — the exact value
  `SetupRepositoryImpl.probePasswordLogin`/`loginWithPassword` persist via
  `serverUrlStorage.saveServerUrl(serverUrl.trim())` *before* issuing any web call
  (`SetupRepositoryImpl.kt:49`, `:62`), so `login.php`/`totp.php`/`profile.php` are only ever
  requested against the host the user just typed, never a fixed or third-party one. No `WebView`
  anywhere in the repo (re-confirmed empty, same grep as the M17 preamble), so this is a plain
  GET/POST + regex scrape (`ApiKeyScraper.kt:14-17`), not an embedded browser — RFC 8252 (which
  governs a third-party login rendered *inside* the app) doesn't apply to a bridge that only ever
  talks to the user's own server. **MASVS-AUTH-2/3 are N/A** — re-confirmed
  `grep -rln 'biometric\|Biometric\|BiometricPrompt'` empty across the whole repo (same result the
  M17 preamble already stated); there is no local-auth gate and no "sensitive operation" surface
  beyond the one credential this app has, checked once at connect time. No Open findings; see the
  Accepted-deviations table above for the one real design tradeoff (`LoginThrottle`'s client-only
  backoff).
- **Platform (MASVS-PLATFORM-1)**: the manifest's entire IPC surface is one component —
  `androidApp/src/main/AndroidManifest.xml:16-25` declares only `MainActivity`, no service,
  receiver, provider, or second activity anywhere in either manifest (`src/main` or `src/debug`,
  which only overrides the app label). It's `android:exported="true"` with a plain
  `MAIN`/`LAUNCHER` `intent-filter` and no `<data>` scheme/host — required for the launcher entry
  point, not an extra surface, and there's no deep link anywhere in the repo for it to be an
  IPC risk beyond that. **MASVS-PLATFORM-2** (WebView) stays N/A, same finding as 17.4's re-confirmed
  empty grep. **MASVS-PLATFORM-3**: the login password field has a working reveal toggle
  (`LoginScreen.kt:254-261`, `login_password_show`/`login_password_hide`), hidden by default — see
  the Accepted-deviations table for the `FLAG_SECURE` decision the toggle's existence bounds.
- **Code (MASVS-CODE-1 through -4)**: all four Accepted, no Open findings. `minSdk`, update
  enforcement and the dependency-advisory scan (CODE-1/2/3) are ported-in or M14/M16 decisions this
  step confirmed rather than designed — see the Accepted table for each. CODE-4 split into two
  checks: the app's one `Json` deserializer (`WallosEnvelopeParser`) is the only JSON config
  anywhere (no Ktor `ContentNegotiation` plugin exists to carry a second, stricter one) and already
  tolerates unknown keys and lenient parsing; and `LocalUriHandler.openUri()` has exactly two call
  sites in the whole repo, both in `AboutScreen.kt`, both fed fixed `RString` resources rather than
  user- or server-supplied text — a materially different shape than the Taiga finding this control
  is checked against, not the same gap re-found, since there is no markdown renderer or
  user-editable URL field anywhere in this app today for a scheme allowlist to guard.
- **Privacy (MASVS-PRIVACY-1 through -4)**: all four Accepted, no Open findings.
  **PRIVACY-1** (permission minimization): exactly two permissions declared
  (`androidApp/src/main/AndroidManifest.xml:4-5`), both used —  `INTERNET` by the Ktor OkHttp engine
  that talks to the user's Wallos server (`core/api/src/androidMain/.../PlatformHttpClientEngine.kt`)
  and, on `gplay` only, Crashlytics; `ACCESS_NETWORK_STATE` by `NetworkMonitorImpl`
  (`core/storage/src/androidMain/.../NetworkMonitorImpl.kt:22-23`, `ConnectivityManager` +
  `registerNetworkCallback`), which backs `LocalIsOffline`. No unused permission, no third
  undeclared-but-needed one. **PRIVACY-2** (no user identification): no analytics/ad-ID/fingerprinting
  dependency anywhere in the catalogue (`grep -n 'analytics\|advertising\|com.google.android.gms'
  gradle/libs.versions.toml` empty) — the only Firebase artifact pulled in is
  `firebase-crashlytics` (`gradle/libs.versions.toml:230-231`), not `firebase-analytics`, and it
  ships on `gplay` only. **PRIVACY-3** (transparency): the crash-reporting posture is a real,
  disclosed flavor split, not just a code-level `isAvailable` flag — `gplay`'s
  `CrashReporterImpl` (`androidApp/src/gplay/kotlin/.../di/CrashReporterImpl.kt`) wraps real
  `Firebase.crashlytics`, `fdroid`'s (`androidApp/src/fdroid/kotlin/.../di/CrashReporterImpl.kt`) is
  a total no-op (`isAvailable = false`, every method a `Unit` body) — and `InterfaceScreen.kt:97`
  gates the entire settings row on `uiState.isCrashReportingAvailable`, so the toggle is *absent*
  on fdroid rather than present-and-inert, matching `CrashReportingStorage`'s own doc comment
  (device-scoped consent, defaults to opt-in-`false`). Two privacy-policy docs mirror the same split
  (`PRIVACY_POLICY_GPLAY.md` names Crashlytics, its data fields and the in-app opt-out;
  `PRIVACY_POLICY.md` doesn't mention it at all) and both list exactly the same two permissions
  confirmed above. **PRIVACY-4** (user control): `ApiKeyStorageImpl.clear()`
  (`core/storage/.../ApiKeyStorageImpl.kt:46-53`) deletes all three Room tables the app has
  (`subscriptionDao`/`currencyDao`/`priceConversionDao` — confirmed against `WallosDB.kt:17-21`,
  which lists no other entity) *before* removing the DataStore `api_key` entry, so there's no
  window where a screen's cache read shows one account's rows with no credential behind them. All
  three call sites checked: `SettingsViewModel.onDisconnectClick`
  (`feature/settings/ui/.../SettingsViewModel.kt:36`, the explicit "log out"),
  `SetupRepositoryImpl.loginWithPassword` (`feature/setup/data/.../SetupRepositoryImpl.kt:66`) and
  `.connectWithApiKey` (`:128`) — both login paths clear the stale key/cache before validating a new
  one, so a second account never inherits the first's cached rows. Device-scoped prefs
  (`ThemeStorage`, `StartDestinationStorage`, `CrashReportingStorage`, `TrustedCertStorage`)
  deliberately survive `clear()` by design (each file's own doc comment states this) — none of them
  are Wallos account data, so leaving them isn't a PRIVACY-4 gap. `PRIVACY_POLICY_GPLAY.md:124-126`
  also documents the coarser fallback (uninstall / Android's own "Clear Data") for a user who wants
  everything gone, not just the account.
