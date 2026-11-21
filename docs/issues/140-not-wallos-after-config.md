# 140 — "Nothing that looks like Wallos answered at that address. Check the server URL"

**Status:** In review
**Link:** https://github.com/Grigoriym/Wallosmobile/issues/140   **Updated:** 2026-11-20

## Report

Reported in Chinese by `wonderhj` (translated):

> Why does WallosMobile on my phone show "Nothing that looks like Wallos answered at that address.
> Check the server URL" after logging in, when the same address works in the web UI? Logging in
> through the LAN address in WallosMobile doesn't help either.

Follow-up comment:

> It worked at first. It stopped working after I configured Telegram notifications, a Fixer API key
> and Google Search (SerpAPI).

- **Symptom (fact):** the `error_not_wallos` message, reached on login, and also when logging in by
  the LAN address. The web UI keeps working.
- **Environment:** unknown. No Wallos version, app version, or flavor (gplay vs fdroid), and no
  word on which login path (password or API key) was used.
- **Reporter's diagnosis (hypothesis):** configuring Telegram, Fixer and SerpAPI broke it.

**Not in the report:** the Wallos version; whether the error shows *on the login screen* or on a
screen after login ("登录后显示" can be read either way); whether it's a reverse-proxy setup; and
what the server actually returns.

## Findings

1. **The message only means `WallosError.Malformed` or `WallosError.UnsupportedEndpoint`.**
   `utils/ui/.../GetErrorMessage.kt:34`. Every screen maps errors through `getErrorMessage`, so
   the same text can come from login or from any list screen.
2. **On the login screen, the only API call that can produce it is the key validation.**
   `SetupRepositoryImpl.validate()` POSTs `api/status/version.php`
   (`feature/setup/data/.../SetupRepositoryImpl.kt`). The web-login steps (`login.php`,
   `totp.php`, `profile.php`) never throw `Malformed`. A missing key is `ApiKeyNotFound`, a refused
   login is `InvalidCredentials`, and both show different messages.
3. **`version.php` can't be affected by the reported configuration.** In upstream `v5.11.2`, it
   includes only `connect_endpoint.php` (DB, i18n, `remember_me.php`, session) and reads only the
   `user` table. It never touches `fixer`, `google_search` or `telegram_notifications`.
4. **I couldn't reproduce it on the latest Wallos.** I ran a scratch `bellamy/wallos:latest`
   container (`v5.11.2`, user language `zh_cn`) and drove the app's own sequence with curl:
   GET `login.php`, POST credentials, scrape `profile.php`, then `version.php` plus every read
   endpoint the app uses. Every call returned clean JSON. Then I wrote a Fixer key (provider 0,
   then 2), a SerpAPI key, an enabled Telegram row and `convert_currency=1` directly into the DB,
   changed the main currency to CNY and repeated the flow. The responses didn't change. **Caveat:**
   the keys were dummies written directly to the DB (the save endpoints check them against the
   real services), so a real exchange-rate refresh never ran.
5. **The app discards the evidence.** `WallosEnvelopeParser` logs the raw body only at `WARN`
   (local Logcat). Crashlytics gets a scrubbed `Envelope shape mismatch: <ExceptionClass>`, and the
   user sees nothing. If the reporter runs the gplay build, Crashlytics should have that event, and
   the exception class name narrows it down: `JsonDecodingException` means the body wasn't JSON,
   and a decode exception from the DTO means a shape mismatch.
6. **Inference, not yet tested:** `decodeEnvelope` cuts from the *first* `{` to the end of the
   body. Two server outputs would turn a working instance into `Malformed`:
   - text **after** the JSON (for example, a PHP shutdown warning such as "Failed to write session
     data", which is time-dependent and could explain "worked at first");
   - a `{` inside a PHP warning printed **before** the JSON.
   Neither depends on the configuration the reporter named, and I haven't observed either one.

### Reporter's answers (2026-11-20)

- `version.php` returns clean JSON: `{"success":true,"title":"version","version":"v5.11.2",...}`.
- Wallos v5.11.2. App 1.1.0 (6), with both the Play and F-Droid builds. Both password and API-key
  login fail the same way.
- **The error appears on the Subscriptions screen after login, not on the login screen.** Login
  succeeds, so findings 2–4 are moot.

### Second round (after the answers)

7. **The Subscriptions screen's refresh makes three calls** (`SubscriptionsRepositoryImpl`
   `refreshSubscriptions`): `api/settings/get_settings.php`,
   `api/subscriptions/get_subscriptions.php` (with `convert_currency=true` when the setting is on),
   and `api/currencies/get_currencies.php`. No other code on that screen's path throws `Malformed`.
8. **The conversion branch of `get_subscriptions.php` only runs after an exchange-rate update has
   been recorded**: `$canConvertCurrency` depends on a `last_exchange_update` row (lines ~155–160).
   That is the one code path a Fixer key newly enables, which fits "broke after configuring Fixer".
9. **That path doesn't reproduce it either.** On a copy of the maintainer's real data (36
   subscriptions, one in THB), I ran upstream v5.11.2 with a real rate refresh (frankfurter
   provider, THB=37.263), `convert_currency=1`, and a `last_exchange_update` row. All three
   responses were valid JSON, and the THB price came back converted (`0.99025…`). A throwaway host
   test then fed those three bodies through the app's `WallosEnvelopeParser` with the real
   `SubscriptionsResponse` / `CurrenciesResponse` / `SettingsResponse` DTOs, and all three decoded.
   The test was deleted afterwards.
10. **Logo filenames are ruled out.** I suspected a Chinese name cut at a byte boundary, which
    would leave invalid UTF-8 and make `json_encode` return false. Upstream's `sanitizeFilename`
    strips names to `[a-zA-Z0-9\s]` (`endpoints/subscription/add.php:18`).

### Third round (2026-11-21)

11. The reporter sent `get_settings` and `get_currencies`. Both are clean and match the DTOs (rates
    as strings, `in_use` as booleans, main currency CNY, USD and CAD in use). He removed the body of
    `get_subscriptions`, which leaves it as the only suspect.
12. **Upstream `add.php` never binds `:logo` when a subscription is created without a logo**
    (`if ($logo != "") { $stmt->bindParam(':logo', …) }`, line ~454 in v5.11.2). The `INSERT` still
    names `:logo`, so SQLite stores `NULL`. The code is identical in v5.4.2, v5.8.0, v5.10.0 and
    v5.11.0, so this is not a regression on the Wallos side. `clone.php` copies the value as-is.
13. **Reproduced end to end.** On a fresh v5.11.2 container, adding a subscription through the web
    endpoint with no logo stored `logo` as `NULL`, and `get_subscriptions.php` returned
    `"logo":null`. A throwaway host test fed that exact body to `WallosEnvelopeParser` with
    `SubscriptionsResponse` and it threw `WallosError.Malformed` — the error the reporter sees. The
    test was deleted afterwards; the body is the regression fixture for the fix.
14. **Why the app breaks:** `SubscriptionDTO` declares `val logo: String = ""`. The lenient `Json`
    (`NetworkModule.provideLenientJson`: `ignoreUnknownKeys`, `isLenient`) has no
    `coerceInputValues`, so a JSON `null` for a non-null property is a decode failure, not the
    default. The maintainer's own data never hit this: none of its 36 rows has a `NULL` logo, and the
    existing fixtures use `"logo":""`.

### Field audit (2026-11-21)

Every DTO field the app declares non-null was compared against Wallos v5.11.2: the column's
nullability (`PRAGMA table_info` on a migrated instance), every `INSERT` that writes it, and every
migration that added it.

- **Real `NULL`s the app can't take:**
  - `subscriptions.logo`: `NULL` for any subscription created in the web UI without a logo
    (finding 12). The API path (`set_subscriptions.php`) always binds `''`. `clone.php` copies
    whatever is there.
  - `subscriptions.url`: added by migration `000004` (`ADD COLUMN url VARCHAR(255)`, no default),
    so subscriptions that already existed then are `NULL`. Only very old installs. Every
    current writer binds a string.
- **Nullable column, but never `NULL` in practice:** `subscriptions` `currency_id`,
  `next_payment`, `cycle`, `frequency`, `notes`, `notify`, `inactive`, `auto_renew`. Every writer
  (`add.php`, `set_subscriptions.php`, `clone.php`) binds a cast or defaulted value, and no
  migration added them without a default. `payment_methods.icon`: every insert binds `""` or a
  filename. `categories.order`, `household.email` and the `user` budget fields carry SQL defaults.
- **Already nullable in the DTO:** `start_date`, `cancellation_date`, `replacement_subscription_id`,
  `payment_method_id`, `payer_user_id`, `category_id`, `notify_days_before`.
- All read endpoints the app uses return the DB rows as-is (`fetchArray` → `json_encode`), so a
  column's `NULL` reaches the client unchanged.

## Root cause

A subscription created in the Wallos web UI without a logo has `logo = NULL`, and
`get_subscriptions.php` returns `"logo":null`. `SubscriptionDTO.logo` is a non-null `String`, and
the lenient `Json` doesn't coerce `null` to the default, so the whole list fails to decode as
`WallosError.Malformed`, shown as "Nothing that looks like Wallos answered…". The reporter's "worked
at first" fits adding a logo-less subscription later. Telegram, Fixer and SerpAPI are a coincidence:
none of them touches the `logo` column. (Inference: not confirmed by the reporter, who withheld the
subscriptions body, but it is the only failure found in the one remaining response, and it
reproduces exactly.)

## Impact

Anyone with at least one subscription created on the web without a logo, on any Wallos version from
at least v5.4.2. The Subscriptions list can never load, and the cache stays empty. Workaround: give
every subscription a logo in the web UI. The same pattern (a non-null `String` with a default, fed a
`NULL` column) may exist in other DTOs; not yet audited.

## Open questions

- None blocking. The field audit above settles the scope: `logo` and `url`.

## Options

- **A. `coerceInputValues = true` on the lenient `Json` (recommended),** plus the same on
  `@IgnoreUnknownKeysJson` used by `WallosCrudApi`. A JSON `null` for a non-null property with a
  default then decodes as that default, for every DTO at once, including fields nobody has hit yet.
  Cons: it is global, so a `null` in a field whose default is a poor stand-in would pass silently
  rather than fail loudly. Properties *without* a default still fail, which is the right behaviour
  for required fields. Blast radius: every decode in the app; the existing parser and DTO tests
  cover it.
- **B. Make `logo` and `url` nullable** (`String?`) and map `null` to `""` in the mapper. Exactly
  the two fields the audit found, and explicit in the DTO. Cons: a column that turns nullable in a
  future Wallos version breaks the app the same way until someone notices.
- **C. Do nothing / report upstream.** Storing `NULL` is arguably a Wallos bug, but every existing
  install already has these rows, so the app has to cope either way.

## Decision

**B**, chosen by the maintainer on 2026-11-21 after the field audit narrowed the scope to `logo` and
`url`.

## What landed

On branch `fix/140-null-logo-url` (off `dev`):

- `SubscriptionDTO.logo` and `.url` are `String? = null`; `SubscriptionMapper` maps `null` to `""`,
  so the domain `Subscription` is unchanged.
- Regression tests:
  - `SubscriptionsApiImplTest` "reads a null logo and url as null rather than failing the whole
    list", driven by `SubscriptionsJsonFixtures.SUBSCRIPTIONS_WITH_NULL_LOGO_AND_URL` (the body
    recorded off v5.11.2). It failed with `WallosError.Malformed` before the change, the same error
    the reporter sees.
  - `SubscriptionMapperTest` "reads a null logo and url as blank".
- `./gradlew allTests detekt ktlintCheck` green.

- On device (2026-11-21, API 37 AVD, `fdroidDebug`, the 8282 instance with one subscription added
  through the web form with no logo, stored as `NULL`):
  - **Before** (APK built from `master`, 1.1.0): Subscriptions showed "Nothing that looks like Wallos
    answered at that address. Check the server URL." with Try again, and the Dashboard silently
    showed €0.00 and "No upcoming payments", since the cache it reads never filled.
  - **After** (this branch, installed over the same login): the list loads, the logo-less row
    renders with its letter placeholder, its detail screen (`get_subscription.php`) opens, and the
    Dashboard shows the real totals.
