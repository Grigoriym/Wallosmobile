# Wallos HTTP API — Reference for Client Developers

Derived from the source in `api/` at **v5.4.2** (commit `3a7f965`). Upstream also publishes
docs at <https://api.wallosapp.com/>, but this document is generated from the actual PHP
handlers and notes places where behaviour differs from the in-file docblocks.

Target audience: someone writing a Kotlin (Android/KMP) client.

---

## 1. Fundamentals

### Base URL

`https://<your-wallos-host>/api/...` — the API is plain PHP files served from the web root,
so the URL path mirrors the repo layout: `api/subscriptions/get_subscriptions.php`.
There is no path rewriting; the `.php` extension is part of the URL.

### Authentication

Wallos has **two independent auth systems** that do not talk to each other:

| | Web UI (`login.php`, `endpoints/`) | JSON API (`api/`) |
|---|---|---|
| Credential | username + password → PHP session cookie | static per-user API key |
| Transport | `PHPSESSID` cookie (+ optional `wallos_login` remember-me cookie) | `api_key` request parameter |
| Responses | HTML pages / 302 redirects | JSON |

The `api/` endpoints **ignore the session entirely** — every one of them resolves the caller
with `SELECT * FROM user WHERE api_key = :apiKey`. Logging in with a password gets you a
session that the JSON API will not accept.

So: there is no username/password *API* endpoint, but there **is** a real password login on
the server, and you can bridge from one to the other. See **§9** for a working
username/password onboarding flow for mobile.

The API key itself is per-user and static. The user can copy it from the web UI
(Profile page → API Key), and regenerating it there immediately invalidates the old one.

The key is passed **as a request parameter, never as a header**:

| Endpoint kind | Where the key goes |
|---|---|
| `get_*` (GET or POST) | `api_key` (or `apiKey`) in query string **or** form body — PHP `$_REQUEST` |
| `set_*` (POST only) | `api_key` (or `apiKey`) in the **form body** — PHP `$_POST` |
| `set_budget`, `get_period_budget` | additionally accept a JSON body |

There is no `Authorization: Bearer` support. Prefer POST for every call so the key does not
end up in server access logs, browser history, or referrers.

There is **no rate limiting** and no CSRF/nonce mechanism on the API endpoints.

### Request encoding

* Read endpoints (`get_*`) accept `GET` or `POST`.
* Write endpoints (`set_*`) accept **`POST` only** and read `$_POST` — meaning
  `application/x-www-form-urlencoded` or `multipart/form-data`. **JSON bodies are ignored**
  by all write endpoints except `api/users/set_budget.php`.
* Endpoints that accept a file upload (`set_subscriptions` with `logo`,
  `set_payment_methods` with `paymenticon`) require `multipart/form-data`.

### Response envelope

`Content-Type: application/json; charset=UTF-8` for everything except the iCal feed.

```json
{ "success": true,  "title": "subscriptions", "subscriptions": [...], "notes": [] }
{ "success": false, "title": "Invalid API key" }
{ "success": false, "title": "Missing parameters", "message": "..." }
```

* `success` — boolean, the only reliable indicator of outcome.
* `title` — short machine-ish string; doubles as an error code (see §5).
* `notes` — array of human-readable warnings. **Not always present.**
  ~~`get_user.php` returns it as an empty *string* rather than an array~~ — **not true against
  this instance** (v5.4.2): a live `curl` against `get_user.php` returns `"notes": []`, a real
  JSON array, same as `get_subscriptions.php` and every other endpoint checked. Corrected
  2026-08-08 while scoping Phase 5's `feature:profile` — the source of the original claim isn't
  known (an older Wallos version, or simply a documentation error), but nothing here should model
  `notes` as `String` on any endpoint.
* `message` — present on most write-endpoint errors, absent on many read-endpoint errors.

> ### ⚠️ Everything returns HTTP 200
> No handler ever calls `http_response_code()`. Auth failures, validation failures and
> "not found" all come back as `200 OK` with `success: false`. A Kotlin client must key
> **only** off the `success` field, not off the HTTP status. A non-200 status means the web
> server or PHP itself failed (500, 404 for a wrong path, etc.), not an API-level error.

Suggested Kotlin envelope:

```kotlin
@Serializable
data class WallosResponse<T>(
    val success: Boolean,
    val title: String? = null,
    val message: String? = null,
    // `notes` is a JSON array everywhere, including get_user (corrected 2026-08-08) — ignore it
    // unless a future endpoint's warnings turn out to matter to a screen.
)
```

---

## 2. Endpoint index

| Method | Path | Purpose |
|---|---|---|
| GET/POST | `api/status/version.php` | Wallos version (still requires a valid key) |
| GET/POST | `api/users/get_user.php` | Current user profile |
| POST | `api/users/set_budget.php` | Monthly / period budget |
| GET/POST | `api/subscriptions/get_subscriptions.php` | List subscriptions (filter + sort) |
| GET/POST | `api/subscriptions/get_subscription.php` | Single subscription |
| POST | `api/subscriptions/set_subscriptions.php` | Add / edit / delete subscription |
| GET/POST | `api/subscriptions/get_monthly_cost.php` | Total cost of one month |
| GET/POST | `api/subscriptions/get_period_budget.php` | Budget-period projection |
| GET/POST | `api/subscriptions/get_ical_feed.php` | `.ics` calendar (not JSON) |
| GET/POST | `api/categories/get_categories.php` | List categories |
| POST | `api/categories/set_categories.php` | Add / edit / delete category |
| GET/POST | `api/currencies/get_currencies.php` | List currencies + main currency |
| POST | `api/currencies/set_currencies.php` | Add / edit / delete currency |
| GET/POST | `api/payment_methods/get_payment_methods.php` | List payment methods |
| POST | `api/payment_methods/set_payment_methods.php` | Add / edit / delete payment method |
| GET/POST | `api/household/get_household.php` | List household members |
| POST | `api/household/set_household.php` | Add / edit / delete member |
| GET/POST | `api/settings/get_settings.php` | User display settings |
| POST | `api/settings/set_settings.php` | Update display settings |
| GET/POST | `api/notifications/get_notification_settings.php` | Notification config (read-only) |
| GET/POST | `api/fixer/get_fixer.php` | Exchange-rate provider config |
| POST | `api/fixer/set_fixer.php` | Set Fixer/APILayer key |
| GET/POST | `api/admin/get_admin_settings.php` | Instance settings — **user id 1 only** |
| POST | `api/admin/set_admin_settings.php` | Update instance settings — admin only |
| GET/POST | `api/admin/get_oidc_settings.php` | OIDC config — admin only |
| POST | `api/admin/set_oidc_settings.php` | Update OIDC config — admin only |
| POST | `api/admin/set_disable_password_login.php` | Toggle password login — admin only |

Notably **absent**: notification-settings writes, logo search, subscription clone/renew,
statistics, database backup/restore. Those exist only as session-authenticated endpoints
under `endpoints/` (see §7) and are not reachable with an API key.

"Admin" means literally `user.id === 1` — the first registered account. There is no role flag.

---

## 3. Core resources

### 3.1 Subscription

`get_subscriptions.php` / `get_subscription.php` do `SELECT *`, so the JSON object is the raw
DB row plus three resolved names. Fields (post-migration schema as of v5.4.2):

| Field | Type | Notes |
|---|---|---|
| `id` | int | |
| `name` | string | |
| `logo` | string | **Filename only.** Full URL = `{base}/images/uploads/logos/{logo}`. Empty string when none. |
| `logo_text_color` | string \| null | `"light"`/`"dark"` classification, only set when "remove background" is on |
| `logo_variant` | string \| null | Themed logo filename, same directory as `logo` |
| `price` | number | JSON number (SQLite REAL) |
| `currency_id` | int | |
| `start_date` | string \| null | `YYYY-MM-DD` |
| `next_payment` | string | `YYYY-MM-DD` |
| `cycle` | int | 1 = Days, 2 = Weeks, 3 = Months, 4 = Years, 5 = One-time |
| `frequency` | int | Multiplier for the cycle: `cycle=3, frequency=6` → every 6 months |
| `auto_renew` | int | 1/0 |
| `notes` | string | |
| `payment_method_id` | int \| null | |
| `payer_user_id` | int \| null | FK to `household`, **not** to `user` |
| `category_id` | int \| null | |
| `notify` | int | 1/0 |
| `notify_days_before` | int \| null | |
| `url` | string | |
| `inactive` | int | 1 = disabled/cancelled, 0 = active |
| `cancellation_date` | string \| null | `YYYY-MM-DD` |
| `replacement_subscription_id` | int \| null | Only meaningful when `inactive = 1` |
| `user_id` | int | Owner |
| `category_name` | string | Resolved; `"No category"` if unmatched |
| `payer_user_name` | string | Resolved; `"Unknown member"` if unmatched |
| `payment_method_name` | string | Resolved; `"Unknown payment method"` if unmatched |

> **Docblock trap:** the example responses in `get_subscriptions.php` and
> `get_subscription.php` show both `cancelation_date` (one `l`) and `cancellation_date`.
> Only `cancellation_date` exists in the schema. Don't model the misspelled one.

Three things the schema doesn't tell you, all read off the live instance (2.1):

- **`name` is HTML-escaped on the wire.** Wallos stores what `htmlspecialchars` produced, so a
  subscription called `1&1 Telekom` comes back as `1&amp;1 Telekom` and renders that way unless
  the client unescapes it. Affects `name`, `notes` and the resolved `*_name` fields.
- **Unset dates are `""`, not `null`.** `cancellation_date` is an empty string on every row that
  was never cancelled, and `start_date` on rows created before it became mandatory. Model them
  nullable *and* treat blank as absent.
- **There is no `logo_url` field** — the MCP server synthesizes one. Over HTTP you get `logo`, a
  bare filename, and build the URL yourself (§4).

Kotlin model sketch:

```kotlin
@Serializable
data class Subscription(
    val id: Int,
    val name: String,
    val logo: String = "",
    val price: Double,
    @SerialName("currency_id") val currencyId: Int,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("next_payment") val nextPayment: String,
    val cycle: Int,
    val frequency: Int,
    @SerialName("auto_renew") val autoRenew: Int = 1,
    val notes: String = "",
    @SerialName("payment_method_id") val paymentMethodId: Int? = null,
    @SerialName("payer_user_id") val payerUserId: Int? = null,
    @SerialName("category_id") val categoryId: Int? = null,
    val notify: Int = 0,
    @SerialName("notify_days_before") val notifyDaysBefore: Int? = null,
    val url: String = "",
    val inactive: Int = 0,
    @SerialName("cancellation_date") val cancellationDate: String? = null,
    @SerialName("replacement_subscription_id") val replacementSubscriptionId: Int? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("payer_user_name") val payerUserName: String? = null,
    @SerialName("payment_method_name") val paymentMethodName: String? = null,
)
```

Use `Json { ignoreUnknownKeys = true }` — older/newer instances have different column sets,
since the schema is built by incremental migrations.

### 3.2 `GET/POST api/subscriptions/get_subscriptions.php`

| Param | Type | Notes |
|---|---|---|
| `api_key` | string | required |
| `member` | string | Comma-separated household member IDs |
| `category` | string | Comma-separated category IDs |
| `payment` | string | Comma-separated payment method IDs — **the docblock calls this `payment_method`; the code reads `payment`** |
| `state` | `0`\|`1` | `0` = active, `1` = inactive. Empty string is ignored |
| `sort` | string | `name`, `id`, `next_payment` (default), `price`, `payer_user_id`, `category_id`, `payment_method_id`, `inactive`, `alphanumeric`. Unknown values silently fall back to `next_payment` |
| `disabled_to_bottom` | `"true"` | **Literal string `"true"`**, anything else = false |
| `convert_currency` | `"true"` | **Literal string `"true"`**. Converts prices into the user's main currency |
| `all-user-subscription` | `1` | Undocumented. Admin (user id 1) only; returns every user's subscriptions plus a `users` array. Non-admins get `"Denied. Not admin user"` |

Sort direction is fixed by the server: `price` and `id` descend, everything else ascends.
`alphanumeric` is an alias for `name`.

Response: `{ success, title: "subscriptions", subscriptions: [...], notes: [] }`, plus
`users: [{id, name, email}]` when `all-user-subscription=1`.

> **Bug to avoid:** when `all-user-subscription=1` is combined with any of `member`,
> `category`, `payment` or `state`, the generated SQL becomes
> `SELECT * FROM subscriptions AND ...` (no `WHERE`), which fails to prepare. Don't send
> filters together with `all-user-subscription`.

Currency conversion only happens if exchange rates have been fetched at least once
(`last_exchange_update` row exists for the user); otherwise prices come back unconverted
with no warning in `notes`. **And when it does happen, only `price` changes — `currency_id` still
names the currency converted from.** Both halves are §5.5, with the table and the three facts a
client has to gather to tell the cases apart.

### 3.3 `GET/POST api/subscriptions/get_subscription.php`

| Param | Notes |
|---|---|
| `api_key` | required |
| `id` / `subscription_id` / `subscriptionId` | required, any of the three |
| `convert_currency` | `"true"` |

Scoped to the caller's own subscriptions; otherwise `title: "Subscription not found"`.

### 3.4 `POST api/subscriptions/set_subscriptions.php`

`action` = `add` | `edit` | `delete`.

**`add`** — required: `name`, `price`, `currency_id`, `frequency`, `cycle`, `next_payment`.
Optional: `start_date` (defaults to today), `auto_renew` (default `1`), `payment_method_id`,
`payer_user_id`, `category_id`, `notes`, `url`, `notify` (or `notifications`),
`notify_days_before`, `inactive` (default `0`), `cancellation_date`,
`replacement_subscription_id`, `logo_url` (or `logo-url`), `logo` (file upload).

**`edit`** — requires `id` (or `subscriptionId` / `subscription_id`); every other field is
optional and omitted fields keep their current value.

**`delete`** — requires only the ID.

Validation the server enforces:

* `cycle` must be **1–4**. Cycle `5` (One-time) exists in the database and in the web UI, but
  the API rejects it: `"Parameter \"cycle\" must be 1 (Days), 2 (Weeks), 3 (Months), or 4 (Years)."`
* Dates must be exactly `YYYY-MM-DD` (strict `DateTime::createFromFormat` round-trip).
* `currency_id`, `category_id`, `payer_user_id`, `payment_method_id` must exist **and belong
  to the calling user** (payment methods may also be global, `user_id = 0` or `NULL`).
* `replacement_subscription_id` is silently nulled if invalid, or if `inactive` is 0.
* Boolean-ish fields are compared as strings: `auto_renew` is 0 only for `"0"`; `notify` and
  `inactive` are 1 only for `"1"`. Send `"1"` / `"0"`, not `true` / `false`.

Logos: `logo_url` makes the server fetch the image (max 3 redirects, 5 s timeout, SSRF
guard rejecting private/CGNAT IPs). A `logo` multipart upload accepts png/jpg/jpeg/gif/webp
and is resized to fit 135×42. Both are optional and independent.

Success: `{ success: true, title: "Subscription added", subscriptionId: 55, message: "..." }`
(`subscriptionId` only on `add`).

### 3.5 `GET/POST api/subscriptions/get_monthly_cost.php`

Params: `api_key`, `month` (1–12), `year`. Returns:

```json
{ "success": true, "title": "March 2025", "monthly_cost": "120.24",
  "localized_monthly_cost": "€120.24", "currency_code": "EUR",
  "currency_symbol": "€", "notes": [] }
```

> `monthly_cost` is a **string** (`number_format`, always 2 decimals, may contain thousands
> separators for large values, e.g. `"1,234.56"`). Parse accordingly — don't map it to Double
> directly. `localized_monthly_cost` is always formatted with the `en_US` locale regardless
> of the user's language.

Only counts `inactive = 0` subscriptions, and includes every occurrence within the month
(a weekly subscription counts 4–5 times).

### 3.6 `GET/POST api/subscriptions/get_period_budget.php`

Params: `api_key`, optional `reference_date` (`YYYY-MM-DD`, defaults to today). Accepts
params via query string, form body, **or JSON body**.

Returns `period_budget`, `amount_needed_this_period`, `amount_needed_full_period`,
`amount_remaining_this_period`, `amount_over_budget` (all rounded floats), `is_over_budget`
(boolean), `budget_period_type` (`weekly`/`fortnightly`/`monthly`),
`budget_period_anchor_date`, `period_start`, `period_end`, `period_label`, `currency_code`,
`currency_symbol`, `reference_date`, `notes`.

### 3.7 `GET/POST api/subscriptions/get_ical_feed.php`

Params: `api_key`, optional `convert_currency=true`.

**Not JSON.** Returns `Content-Type: text/calendar` with
`Content-Disposition: attachment; filename="subscriptions.ics"` containing one `VEVENT` per
active subscription (all-day event on `next_payment`, with a `VALARM` derived from
`notify_days_before`). Errors before that point still come back as JSON — check the response
content type before parsing.

### 3.8 `POST api/users/set_budget.php`

Accepts a **JSON body** or form fields. Params: `api_key`, and at least one of
`monthly_budget` (alias `budget`), `period_budget`, `budget_period_type`
(`weekly`/`fortnightly`/`monthly`), `budget_period_anchor_date` (`YYYY-MM-DD`).

Negative values are clamped to 0. Note: sending `period_budget` **or** any period metadata
causes both `budget_period_type` and `budget_period_anchor_date` to be written, defaulting to
`monthly` / today's anchor if you didn't send them — so always send the period type and
anchor together with `period_budget` if you don't want them reset.

### 3.9 `GET/POST api/users/get_user.php`

Returns the whole `user` row with `password` and `api_key` replaced by `"********"`.
Includes `id`, `username`, `email`, `main_currency`, `avatar`, `language`, `budget`,
`period_budget`, `budget_period_type`, `budget_period_anchor_date`, `totp_enabled`,
`firstname`, `lastname`, `oidc_sub` (`null` off OIDC). `notes` is `[]`, same as every other
endpoint — see §1's correction; do not special-case this one.

### 3.10 Categories / Currencies / Household / Payment methods

All four follow the same shape.

**Read** (`get_*`) — only `api_key`; returns an array where each item carries an `in_use`
boolean indicating whether any subscription references it.

* Categories: `{ id, name, order, in_use }`
* Currencies: `{ id, name, symbol, code, rate, in_use }` plus a top-level `main_currency` int
* Household: `{ id, name, email, in_use }`
* Payment methods: `{ id, name, icon, enabled, order, in_use }` — `icon` is a path already
  relative to the web root (e.g. `images/uploads/icons/paypal.png`), unlike subscription
  logos which are bare filenames.

**Write** (`set_*`) — `action` = `add` | `edit` | `delete`, plus:

| Resource | Fields | ID param aliases |
|---|---|---|
| Categories | `name` | `categoryId` or `id` |
| Currencies | `name`, `symbol`, `code`, `rate` (default 1.0) | `currencyId` or `id` |
| Household | `name`, `email` (optional) | `memberId` or `id` |
| Payment methods | `name`, `enabled` (`1`/`0`), `icon_url` (or `icon-url`), `paymenticon` (file) | `paymentId` or `id` |

Deleting an item that is still referenced by a subscription fails with
`title: "Category in use"` (and equivalents). Returns `categoryId` / `currencyId` /
`memberId` / `paymentId` on successful `add`.

### 3.11 Settings

`get_settings.php` returns the raw `settings` row for the user (minus `user_id`), with
`custom_colors` and `custom_css` nested objects appended when present. Typical fields:
`dark_theme`, `monthly_price`, `convert_currency`, `remove_background`, `color_theme`,
`hide_disabled`, `disabled_to_bottom`, `show_original_price`, `mobile_nav`,
`show_subscription_progress`, `week_starts_sunday`, `square_icons`.

`set_settings.php` (POST) takes any subset of: `dark_theme` (`0` light / `1` dark /
`2` automatic), `color_theme` (`blue`, `green`, `red`, `yellow`, `purple`, `custom` — anything
else is rejected), `monthly_price`, `convert_currency`, `show_original_price`, `mobile_nav`,
`show_subscription_progress`, `week_starts_sunday`, `disabled_to_bottom`, `hide_disabled`,
`remove_background`, `square_icons` (all `1`/`0`), `main_color` / `accent_color` /
`hover_color` (hex, camelCase aliases accepted), `css`.

Since the settings table grows by migration, treat this object as an open map rather than a
fixed data class.

`convert_currency` here is the **user's own preference**, and it is what decides whether a client
should send `convert_currency=true` at all — overriding it means showing a user who asked for real
currencies something else. It is an `int` (`0`/`1`) on the way out of `get_settings.php`, `1`/`0`
on the way in to `set_settings.php`, and the literal string `"true"` on the subscription reads
(§3.2): three shapes for one flag. Absent on an instance predating the column — read that as off,
which is the only value that cannot mislabel a price (§5.5).

### 3.12 Notifications & Fixer

`get_notification_settings.php` returns a `notification_settings` object containing `id`,
`days`, and a sub-object per **enabled** channel (`email_notifications`,
`discord_notifications`, `gotify_notifications`, `ntfy_notifications`,
`pushover_notifications`, `telegram_notifications`, `webhook_notifications`,
`serverchan_notifications`, and others depending on version). Secrets are masked as
`"********"`. **There is no API endpoint to write notification settings.**

`get_fixer.php` returns `{ api_key: "********", provider: 0|1, provider_name }`.
`set_fixer.php` (POST) takes `fixer_api_key` (empty clears it) and `provider`
(`0` = Fixer.io, `1` = APILayer).

### 3.13 Admin endpoints

All require the API key of **user id 1**; others get `success: false`. `get_admin_settings`
masks `smtp_password`; `get_oidc_settings` does **not** mask `client_secret`. Parameters are
listed in each file's docblock and are all optional partial updates.

---

## 4. Assets

| Asset | How to build the URL |
|---|---|
| Subscription logo | `{base}/images/uploads/logos/{subscription.logo}` (skip if empty) |
| Themed logo variant | `{base}/images/uploads/logos/{subscription.logo_variant}` |
| Payment method icon | `{base}/{payment_method.icon}` — already includes the directory |
| User avatar | `{base}/{user.avatar}` — already includes the directory |

Image files are served by the web server without authentication, so a plain image loader
(Coil) works — no API key needed on those requests.

---

## 5. Error handling

### 5.1 The three failure layers

There is no framework here — each endpoint is a standalone PHP script that hand-rolls its
own checks. Failures reach you at three different layers, and only the third one is JSON.

**Layer 1 — transport / no envelope at all.** The body is not JSON and `Json.decodeFromString`
will throw. Causes:

| Cause | What you get |
|---|---|
| Endpoint doesn't exist on this Wallos version | nginx **HTTP 404** HTML page |
| Wrong base URL / reverse proxy misconfig | HTML, redirect, or a login page |
| PHP fatal error | **HTTP 500** (or 200) with an HTML error page |
| DB file unreadable | `connect_endpoint.php` emits the plain-text string `Connection to the database failed.` |
| PHP warning or notice during the handler | Warning text **prepended to otherwise-valid JSON** — see below |
| `get_ical_feed.php` success | `text/calendar`, not JSON |

**Layer 2 — corrupted JSON from PHP diagnostics.** The Dockerfile installs no `php.ini`, so
PHP's compiled-in default applies. Verified against `php:8.3-fpm-alpine`, the exact base image:

```
ini_file=(none)  display_errors='1'
```

`display_errors` is **On**, so any warning or notice is written into the response body before
the JSON. Under FPM it is HTML-wrapped:

```
<br />
<b>Warning</b>:  Trying to access array offset on null in /var/www/html/api/... on line 84<br />
{"success":true,"title":"March 2025","monthly_cost":"0.00", ...}
```

The status is still `200` and the JSON is still there — just not at offset 0. Known triggers:
a `month`/`year` that doesn't resolve to a currency in `get_monthly_cost.php`, and the
`all-user-subscription` + filter SQL bug in `get_subscriptions.php` (§3.2).

Mitigation: don't parse the raw body directly. Find the first `{`, and if the prefix is
non-empty, treat it as a server-side warning — parse the remainder, but log the prefix.

**Layer 3 — the normal error envelope**, always HTTP 200:

```json
{ "success": false, "title": "Unauthorized", "message": "Invalid API key." }
```

| Field | Presence |
|---|---|
| `success` | Always. The **only** reliable signal. |
| `title` | Always on errors. Short, English, not localized. |
| `message` | On **all `set_*` errors**; on **most `get_*` errors it is absent**. Exception: `get_subscription.php` includes it. |
| `notes` | Only `get_monthly_cost.php` and `get_period_budget.php` use it to carry error text (as an array). Never present alongside `message`. |

So the human-readable detail lives in a different field depending on endpoint. Read
`message ?: notes?.firstOrNull() ?: title`.

### 5.2 Check order

Every endpoint validates in a fixed order and returns on the first failure, so you only ever
see one error at a time — there is no field-level error list:

`request method` → `api_key present` → `api_key valid` → `admin?` → `action valid` →
`required params` → `format validation` → `foreign keys exist and are yours` → `DB write`

### 5.3 ⚠️ Auth failures do not share a single `title`

This is the most important gotcha for a client, because "key is bad → send the user back to
setup" is the one error you must detect reliably:

| Situation | Endpoint group | `title` | `message` |
|---|---|---|---|
| Key absent | all `get_*` | `Missing parameters` | *(absent)* |
| Key absent | all `set_*` | `Missing API key` | `API key is required.` |
| Key absent | `get_subscription.php` | `Missing parameters` | `Both API key and subscription ID are required.` |
| Key invalid | all `get_*` | `Invalid API key` | *(absent)* |
| Key invalid | `get_subscription.php` | `Invalid API key` | `Unauthorized access.` |
| Key invalid | `get_monthly_cost.php` | `Invalid API key` | `notes: ["User not found or API key invalid."]` |
| Key invalid | all `set_*` | `Unauthorized` | `Invalid API key.` |
| Key invalid | `set_disable_password_login.php` | `Unauthorized` | `Invalid API key or insufficient privileges.` |
| Valid key, not admin | admin `get_*` | `Invalid user` | *(absent)* |
| Valid key, not admin | admin `set_*` | `Forbidden` | `Only the admin user (user ID 1) can update...` |
| Valid key, not admin | `get_subscriptions.php` + `all-user-subscription=1` | `Denied. Not admin user` | *(absent)* |

Treat **`Invalid API key`, `Unauthorized`** as "re-authenticate", and
**`Invalid user`, `Forbidden`, `Denied. Not admin user`** as "not permitted" — do *not*
clear the stored key for the second group.

Note that `Missing parameters` is overloaded: `get_monthly_cost.php` returns it both for a
missing key *and* for a missing `month`/`year`, with nothing to distinguish them.

### 5.4 Full `title` catalogue

**Request-shape errors**

| `title` | Meaning |
|---|---|
| `Invalid request method` | GET used on a `set_*` endpoint (or an unsupported verb). `message: "Only POST requests are allowed."` on writes |
| `Invalid action` | `action` missing or not `add`/`edit`/`delete`. A missing `action` produces this, **not** a "missing parameter" error |

**Missing required parameters**

| `title` | Where |
|---|---|
| `Missing parameter` | Singular — one field missing (`name`, `id`, `disable`) |
| `Missing parameters` | Plural — several required, or a `get_*` key check |

**Validation** — the `message` names the offending parameter:

| `title` | Endpoint | Trigger |
|---|---|---|
| `Invalid parameter` | `set_subscriptions` (add) | `cycle` not 1–4 |
| `Invalid cycle` | `set_subscriptions` (edit) | same rule, **different title than add** |
| `Invalid name` | `set_subscriptions` (edit) | empty name |
| `Invalid date` | `set_subscriptions` | `next_payment`, `start_date` or `cancellation_date` not `YYYY-MM-DD` |
| `Invalid parameter` | `set_settings` | non-0/1 toggle, bad `dark_theme`, bad `color_theme` |
| `Invalid colors` | `set_settings` | not `#RRGGBB` |
| `Color validation failed` | `set_settings` | main and accent colour identical |
| `Invalid parameter` | `set_budget` | non-numeric budget |
| `Invalid parameter` | `get_period_budget` | bad `reference_date` (detail in `notes`) |
| `Invalid parameter` | `set_disable_password_login` | `disable` not `"0"`/`"1"` |
| `Invalid provider` | `set_fixer` | `provider` not 0/1 |
| `Invalid file type` | `set_payment_methods` | upload is not an image |
| `Validation error` | `set_admin_settings` | SMTP port range, email-verification/server-URL, registration/login combinations |

**Referential integrity**

| `title` | Meaning |
|---|---|
| `Invalid currency ID` / `Invalid category ID` / `Invalid payer ID` / `Invalid payment method ID` | Target doesn't exist **or isn't yours** — the two are deliberately indistinguishable |
| `Invalid replacement ID` | `replacement_subscription_id` equals the subscription's own id |

**Not found / ownership**

| `title` | Meaning |
|---|---|
| `Subscription not found` | Bad id, or owned by another user |
| `Unauthorized or Not Found` | Same idea for categories, currencies, household, payment methods. **Despite the name this is not an auth error** — don't clear the API key on it |

**Delete guards**

| `title` |
|---|
| `Category in use` / `Member in use` / `Payment method in use` |
| `Cannot delete category` / `Cannot delete member` (the default row, id 1) |
| `Cannot delete currency` (it's the main currency) |

**Server-side**

| `title` | Meaning |
|---|---|
| `Database error` | Write failed. On `set_subscriptions` and `set_payment_methods` the `message` **appends raw `SQLite3::lastErrorMsg()`** — log it, never show it to the user |
| `Configuration error` | `set_admin_settings`: admin row missing |
| `Security Block` / `Security Error` | SSRF guard rejected an SMTP host or OIDC URL pointing at loopback/link-local |
| `Environment override` / `Managed by environment` | Setting is pinned by an env var (`OIDC_ENABLED`, `OIDC_DISABLE_PASSWORD_LOGIN`) and can't be changed via API |
| `Invalid Fixer API key` / `Validation error` | `set_fixer` verified the key against the provider and it failed |

### 5.5 Silent failures — no error is returned

These are worth guarding against in the UI because the call reports `success: true`:

* **`convert_currency=true` with no exchange rates yet.** If the instance has never fetched
  rates, prices come back unconverted with an **empty `notes` array** on
  `get_subscriptions` / `get_subscription`. Only `get_monthly_cost` warns, via `notes`.

  **A converted response is indistinguishable from an unconverted one** — this is the deeper
  problem, and it holds whether or not rates exist. Both endpoints overwrite **`price` alone**
  and leave `currency_id` naming the currency they converted *from*
  (`$subscriptionToReturn = $subscription;` then one assignment to `price`). So a client that
  renders the row's own symbol prints the wrong sign on a real number. Measured on a real
  instance, main currency EUR, one row at 31.99 USD with `rate = 1.09`:

  | request | `price` | `currency_id` | `notes` |
  |---|---|---|---|
  | no flag | `31.99` | `2` (USD) | `[]` |
  | `convert_currency=true`, no rates | `31.99` | `2` (USD) | `[]` |
  | `convert_currency=true`, rates | `29.348623853211006` | `2` (USD) | `[]` |

  An earlier version of this section said to detect it by comparing `currency_id` against
  `main_currency`. **That is wrong** and it was wrong in the direction that ships a bug: the
  comparison says conversion was *attempted*, never that it happened. What actually decides it:

  - **the user's own `convert_currency`** from `get_settings.php` (§3.11) — an **int** `0`/`1`;
  - **`main_currency`**, a top-level field of the `get_currencies.php` envelope, which is the
    only thing that says what a converted price is denominated in;
  - **whether rates exist.** The server gates on a `last_exchange_update` row that **no endpoint
    exposes**. The observable stand-in is the rate table: `createdatabase.php` seeds every rate
    at exactly `1` and only an exchange update writes both the rates and that row, so any
    `rate != 1` proves an update ran. Where the two disagree it costs nothing — `price / 1` is
    the same amount in either currency.

  `get_monthly_cost.php` reads `last_exchange_update` with **no `user_id` filter**
  (`SELECT * FROM last_exchange_update`, then the first row), so on a multi-user install its
  warning follows whichever user updated rates first. The two subscription endpoints do filter
  by user.
* **`replacement_subscription_id` pointing at something invalid** is silently set to `null`.
* **`sort` with an unrecognised value** silently falls back to `next_payment`.
* **Logo fetch failure.** If `logo_url` can't be downloaded, the subscription is still
  created/updated successfully, just without a logo. The failure reason
  (`Invalid URL format.` / `Invalid IP Address.` / `Failed to fetch image.`) is produced
  internally but **never reaches the response**. Verify by re-reading `logo` after the write.
* **`set_budget` resets period metadata.** Sending `period_budget` alone rewrites
  `budget_period_type` to `monthly` and the anchor to today (§3.8).

### 5.6 Kotlin handling

```kotlin
sealed class WallosError : Exception() {
    /** Key is bad/revoked — clear stored credentials, return to setup. */
    data class Unauthenticated(val title: String) : WallosError()
    /** Key is fine, this user just can't do it — keep the key. */
    data class Forbidden(val title: String) : WallosError()
    data class NotFound(val detail: String) : WallosError()
    data class Validation(val title: String, val detail: String) : WallosError()
    data class InUse(val detail: String) : WallosError()
    /** `Database error` and friends — log detail, show something generic. */
    data class Server(val detail: String) : WallosError()
    /** Endpoint absent: feature unsupported on this Wallos version. */
    object UnsupportedEndpoint : WallosError()
    /** Body wasn't JSON, or PHP diagnostics were mixed in. */
    data class Malformed(val body: String) : WallosError()
}

private val AUTH_TITLES = setOf("Invalid API key", "Unauthorized", "Missing API key")
private val PERMISSION_TITLES = setOf("Invalid user", "Forbidden", "Denied. Not admin user")
private val NOT_FOUND_TITLES = setOf("Subscription not found", "Unauthorized or Not Found")

fun mapError(title: String?, detail: String): WallosError = when {
    title == null                     -> WallosError.Server(detail)
    title in AUTH_TITLES              -> WallosError.Unauthenticated(title)
    title in PERMISSION_TITLES        -> WallosError.Forbidden(title)
    title in NOT_FOUND_TITLES         -> WallosError.NotFound(detail)
    title.endsWith(" in use")         -> WallosError.InUse(detail)
    title.startsWith("Cannot delete") -> WallosError.InUse(detail)
    title.startsWith("Invalid") ||
    title.startsWith("Missing")  ||
    title.startsWith("Validation") ||
    title.startsWith("Color")         -> WallosError.Validation(title, detail)
    else                              -> WallosError.Server(detail)
}
```

Parsing, tolerant of the `display_errors` prefix:

```kotlin
private val json = Json { ignoreUnknownKeys = true; isLenient = true }

suspend inline fun <reified T> parse(response: Response): T {
    if (response.code == 404) throw WallosError.UnsupportedEndpoint
    val raw = response.body?.string().orEmpty()

    // PHP warnings are emitted before the JSON when display_errors is On (the default).
    val start = raw.indexOf('{')
    if (start < 0) throw WallosError.Malformed(raw)
    if (start > 0) Log.w("Wallos", "server emitted diagnostics: ${raw.take(start)}")

    val envelope = try {
        json.parseToJsonElement(raw.substring(start)).jsonObject
    } catch (e: Exception) {
        throw WallosError.Malformed(raw)
    }

    if (envelope["success"]?.jsonPrimitive?.booleanOrNull != true) {
        val title = envelope["title"]?.jsonPrimitive?.contentOrNull
        val detail = envelope["message"]?.jsonPrimitive?.contentOrNull
            ?: envelope["notes"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
            ?: title.orEmpty()
        throw mapError(title, detail)
    }
    return json.decodeFromJsonElement(envelope)
}
```

Three rules that fall out of the above:

1. **Never branch on the HTTP status** except for `404` (feature missing) and `5xx`
   (server broken). Everything else is `200`.
2. **Never `require(response.isSuccessful)`** as your only check — it will happily let
   `success: false` through.
3. **`Unauthorized or Not Found` must not clear the API key.** It is a per-row ownership
   check on categories/currencies/household/payment methods, and the name is misleading.

---

## 6. Client design notes for Kotlin

1. **Treat `success == false` as the error path**, independent of HTTP status. A Retrofit
   `Call` returning 200 with `success: false` must still surface as a failure in your repo layer.
2. **Send everything as form-encoded POST.** One `FormBody`/`@FieldMap` shape works for the
   whole API and keeps the key out of URLs. Use `@Multipart` only for logo/icon upload.
3. **Booleans are inconsistent.** `convert_currency` and `disabled_to_bottom` want the string
   `"true"`; everything else wants `"1"` / `"0"`. Encode them explicitly rather than relying
   on `Boolean.toString()`.
4. **Numbers are inconsistent.** `price` is a JSON number; `monthly_cost` is a formatted
   string; `rate` is a string in the currencies table. Don't assume.
5. **`ignoreUnknownKeys = true` is mandatory** — responses are raw DB rows and self-hosted
   instances run different migration levels.
6. **Version-gate features** with `api/status/version.php`. `get_period_budget` /
   `set_budget`'s period fields, `logo_variant`, and `square_icons` are recent additions;
   older servers return 404 (a real HTTP 404 from nginx, not a JSON error) or omit fields.
7. **No pagination anywhere.** `get_subscriptions` returns the entire list; do filtering and
   paging client-side.
8. **No write access to notifications, stats, logo search, clone, or renew** through the API.
   If your app needs those, they only exist as session-cookie endpoints under `endpoints/`.
9. **The API key is a bearer credential in plaintext.** Store it in EncryptedSharedPreferences
   or the Keystore, require HTTPS, and consider certificate pinning is impractical for
   self-hosted instances (users have arbitrary/self-signed certs — offer a trust prompt rather
   than pinning).
10. **Self-hosted URL handling.** Accept a base URL with or without a trailing slash and with
    a subpath (many users run Wallos under `/wallos`). Validate by calling
    `api/status/version.php` during setup.

---

## 7. Session-authenticated endpoints (not part of the API)

`endpoints/**.php` are the web UI's AJAX handlers. They authenticate with the PHP session
cookie set by `login.php`, **not** with an API key, and they are not versioned or documented.
Relevant ones with no API equivalent:

* `endpoints/subscription/clone.php`, `renew.php`, `getcalendar.php`
* `endpoints/logos/search.php`, `google_search.php`, `icon_search.php`
* `endpoints/notifications/save*.php`, `test*.php`
* `endpoints/db/backup.php`, `restore.php`, `import.php`
* `endpoints/ai/*` (recommendations)
* `endpoints/user/regenerateapikey.php`

Driving these from a mobile app would mean scraping the login form and holding a session
cookie — brittle, and it breaks with TOTP or OIDC enabled. Recommend staying on the API key
surface and filing upstream requests for the gaps.

---

## 8. Quick smoke test

```bash
BASE=https://wallos.example.com
KEY=your_api_key

curl -s -X POST "$BASE/api/status/version.php"                 -d "api_key=$KEY"
curl -s -X POST "$BASE/api/users/get_user.php"                 -d "api_key=$KEY"
curl -s -X POST "$BASE/api/subscriptions/get_subscriptions.php" \
     -d "api_key=$KEY" -d "sort=next_payment" -d "convert_currency=true"
curl -s -X POST "$BASE/api/subscriptions/set_subscriptions.php" \
     -d "api_key=$KEY" -d "action=add" -d "name=Test" -d "price=9.99" \
     -d "currency_id=1" -d "cycle=3" -d "frequency=1" -d "next_payment=2026-09-01"
```

Run these against the **local instance in `docs/local-info.txt`**, not a public demo. The one at
`demo.wallosapp.com` looks tempting and is not usable: its `profile.php` dies with a PHP fatal
(`no such table: uploaded_avatars`), so there is no `id="apikey"` to scrape and the §9 login
bridge can never succeed there.

---

## 9. Username / password login on mobile

**Short answer:** there is no login endpoint in the JSON API, but the server does have a real
password login. You can implement a username/password screen on mobile — it just has to
drive the *web* login and then hand off to the API key. Treat it as **onboarding**, not as
your ongoing auth mechanism.

### 9.1 What actually exists

`login.php` accepts a plain form POST (`login.php:161`):

| Field | Notes |
|---|---|
| `username` | required |
| `password` | required |
| `remember` | optional; presence alone means "stay logged in" (checkbox semantics — send `remember=on` or omit entirely) |

Content type is `application/x-www-form-urlencoded`. Notably:

* **No CSRF token on the login POST.** The login form carries no hidden token — the only CSRF
  machinery on `login.php:132` guards the OIDC `state` parameter. A direct POST works.
* **No rate limiting and no failed-attempt lockout** anywhere in the codebase. Implement your
  own backoff client-side; without it you're shipping a brute-force tool against the user's
  own server.
* Password check is `password_verify` against the bcrypt hash (`login.php:177`).

**A GET of `login.php` is not a passive read** (3.10, straight off the PHP). Two things to know
before adding one to a flow that already has a session:

* It **clears `$_SESSION['totp_user_id']` and `$_SESSION['token']`** near the top of the file,
  unconditionally. So a GET between the login POST and the `totp.php` POST destroys the pending
  challenge, and the code that follows comes back as the §9.2 "session gone" row — a failure that
  looks like the user was slow and isn't.
* It can answer **302 rather than the form**, three ways, none of them about credentials: no user
  has registered yet (`Location: registration.php`), the session is already logged in
  (`Location: .`), or the admin set `login_disabled`, which **logs the caller straight in as user
  1** and redirects to `.`. That last one hands out a session on a GET.

### 9.2 Interpreting the response

`login.php` never returns JSON. You must **disable automatic redirect following** and read
the status + `Location` header:

| Outcome | Response |
|---|---|
| Success | **302** → `Location: .`, plus a new `PHPSESSID` cookie (and `wallos_login` if `remember` was sent) |
| TOTP required | **302** → `Location: totp.php` — session holds `totp_user_id`, not yet logged in |
| Bad credentials | **200** with the login page HTML re-rendered (`$loginFailed = true`) |
| Email not verified | **200** with the login page HTML (`$userEmailWaitingVerification = true`) |

Both failure modes are a 200 carrying HTML, and they're distinguished only by which
translated message block is rendered — there is no machine-readable marker. Practically:
**302 = success, 200 = failure**, and you can't reliably tell *why* it failed.

**TOTP second step:** POST `one-time-code` to `totp.php` on the same session
(`totp.php:48`). Only after that does `$_SESSION['loggedin']` become true (`totp.php:109`).
It answers on the same redirect-is-the-result shape, with **three** outcomes, not two:

| Outcome | Response |
|---|---|
| Verified | **302** → `Location: .`, after `session_regenerate_id(true)` — so a *new* `PHPSESSID` arrives with it |
| Rejected code | **200** with the totp page HTML re-rendered (`$invalidTotp = true`) |
| Session gone | **302** → `Location: login.php` — the guard at the top of the file, when `$_SESSION['totp_user_id']` is unset |

The third row is the one worth handling separately: no code can ever complete that attempt, so
reporting it as a bad code leaves the user typing fresh digits at a dead session. All three were
confirmed against a real instance, not read off the source alone.

Two more things the field has to allow for:

* **A backup code is accepted here too**, and it is 20 hex characters — `bin2hex(random_bytes(10))`,
  ten of them generated at enrolment (`endpoints/user/enable_totp.php:92`). A numeric-only input
  cannot type one.
* **The verification window is wide** — `$totp->verify($code, null, 15)` — so clock skew is not a
  realistic failure mode for this step.

### 9.3 The bridge: login → API key

Once you hold a valid session, the API key is readable from `profile.php`, which renders it
into an HTML input (`profile.php:272`):

```html
<input type="text" id="apikey" name="apikey" value="THE_KEY_IS_HERE" readonly>
```

So the onboarding flow is:

1. `POST /login.php` with `username` + `password`, redirects disabled.
2. If `Location: totp.php`, prompt for the 6-digit code and `POST /totp.php` with
   `one-time-code`, same cookie jar.
3. `GET /profile.php` with the session cookie.
4. Regex the value out of `id="apikey"`.
5. **Persist the API key, discard the session cookie.**
6. Use the JSON API from `api/` for everything afterwards.

The user types their username and password once, exactly as on the web, and never sees an
API key — which is the UX you're after — while your app still runs on the supported,
stable JSON surface.

```kotlin
// Step 1 — must not follow redirects, and needs its own cookie jar
val loginClient = OkHttpClient.Builder()
    .followRedirects(false)
    .cookieJar(sessionCookieJar)
    .build()

val res = loginClient.newCall(
    Request.Builder()
        .url("$base/login.php")
        .post(FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .build())
        .build()
).execute()

when {
    res.code == 302 && res.header("Location")?.contains("totp") == true -> NeedsTotp
    res.code == 302 -> LoggedIn
    else -> InvalidCredentials   // 200 + HTML, reason indeterminable
}

// Step 4 — scrape, then throw the session away
private val API_KEY = Regex("""id="apikey"[^>]*value="([^"]*)"""")
val key = API_KEY.find(profileHtml)?.groupValues?.get(1)
```

### 9.4 Do not use `regenerateapikey.php`

`endpoints/user/regenerateapikey.php` returns the key as clean JSON (`{"success", "message",
"apiKey"}`), which looks like the tidier option. Two reasons to avoid it:

* It **mints a new key and invalidates the old one**, silently breaking the user's web
  session integrations, Home Assistant widgets, iCal subscriptions, or a second device.
* Like everything under `endpoints/`, it requires a **CSRF token** on top of the session
  (`includes/validate_endpoint.php:13`) — read from `$_POST['csrf_token']` or the
  `X-CSRF-TOKEN` header. The token is generated per page render and injected as
  `window.csrfToken` into the HTML (`includes/header.php:113`), so you'd have to scrape that
  too. Scraping `profile.php` is strictly less work and non-destructive.

### 9.5 Risks to weigh before committing

* **HTML scraping is version-fragile.** The `id="apikey"` input is not an API contract; an
  upstream markup change silently breaks onboarding. Mitigate by validating the scraped key
  with `api/status/version.php` before storing it, and by keeping a manual "paste your API
  key" fallback path in the UI. The fallback is not optional — it's your recovery route.
* **Password-login may be disabled entirely.** `password_login_disabled` (admin setting or
  the `OIDC_DISABLE_PASSWORD_LOGIN` env var) removes the username/password fields from the
  form, and OIDC-only instances can't use this flow at all. Detect it by GETting `login.php`
  and checking whether the password input is present — and fall back to manual key entry.
  Three details the source adds (3.10, confirmed against a container):
  **the flag is only read when OIDC is both enabled and *configured*** — `login.php` initialises
  `$password_login_disabled = false` and only reassigns it inside `if ($oidcEnabled)`, where
  `is_configured` means all seven of `client_id`, `client_secret`, `authorization_url`,
  `token_url`, `user_info_url`, `redirect_url`, `user_identifier_field` are non-empty. So a form
  with no password input *is* an SSO instance; there is no other way to render one.
  **The whole credential block goes**, not just the password input — `<?php if
  (!$password_login_disabled) { ?>` wraps username, password, remember and the submit button, and
  the `<form action="login.php">` itself stays, carrying only the OIDC anchor.
  And **the absence only means anything on a page that is the login form**: any other 200 on that
  address — a proxy error, another app — has no password input either, so recognise the form
  before reading the absence.
* **OIDC cannot be bridged this way.** It's a browser redirect dance against a third-party
  IdP; it would need a Custom Tab and the registered `redirect_url`, which points at the
  Wallos web app, not your app.
* **You are handling the user's actual password.** Never persist it — exchange it for the
  API key and drop it. The API key alone is what goes into EncryptedSharedPreferences.
* **HTTPS is on the user.** Self-hosted instances are often plain HTTP on a LAN; a password
  POST over cleartext is materially worse than shipping a pre-existing key. Warn on
  non-HTTPS base URLs.
