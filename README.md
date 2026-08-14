# WallosMobile

An **unofficial** Kotlin Multiplatform client for [Wallos](https://github.com/ellite/Wallos), the
self-hosted subscription tracker. Not affiliated with the Wallos project.

> **Status: feature-complete, pre-release.** Onboarding (password bridge, manual API key, TOTP),
> the subscriptions list/detail/add/edit flow, the dashboard, the management screens (categories,
> payment methods, currencies, household), settings, offline caching, a full MASVS security
> review and CI/release automation are all in place — but nothing has been tagged or published to
> a store yet. [`docs/CHECKLIST.md`](docs/CHECKLIST.md) is the single record of how far along it
> is.

## Platforms

**Android only for now.** The project is built KMP-first: all source lives in `commonMain`, and
platform targets are declared in one function in `build-logic`, so adding iOS or Desktop later is
a build-logic change rather than a refactor.

## What's here

Username/password onboarding against your own Wallos instance (or a manually-entered API key),
the subscriptions list with add/edit/delete, a dashboard, and management screens for categories,
payment methods, currencies and household members — all backed by an offline Room cache. Wallos
has no login API, so onboarding drives the web login once and bridges to the per-user API key the
JSON API actually uses (plan §1.1).

Phases 0 through 5 of the build (see [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md)
§8) are done. What's left is Phase 6 ("Extended": read-only notifications, iCal export, an admin
screen, a home-screen widget, and re-enabling the iOS/Desktop targets) — not yet decomposed into
checklist steps.

## Tech stack

Kotlin Multiplatform · Compose Multiplatform · Navigation 3 · Koin (compiler plugin, not KSP) ·
Ktor · kotlinx.serialization · DataStore · Coil · MVVM + Clean Architecture in vertical feature
slices · detekt, ktlint, compose-rules, Kover.

## Build commands

```bash
./gradlew :androidApp:assembleGplayDebug :androidApp:assembleFdroidDebug   # build (both store flavors)
./gradlew allTests                           # all module tests
./gradlew :module:path:testAndroidHostTest   # one module
./gradlew detekt ktlintCheck                 # lint
./gradlew koverHtmlReport                    # coverage
```

Requires JDK 21. There is no `jvmTest` task — the project declares no `jvm()` target, so unit
tests run as the AGP KMP host test (`testAndroidHostTest`) over `commonTest`. There are two store
flavors (`gplay`, `fdroid`); [`CLAUDE.md`](CLAUDE.md) has the full command reference, including
the extra property and secrets a real `gplay` or signed release build needs.

CI runs both flavor assembles, `allTests`, detekt, ktlint and a Kover coverage upload to Codecov
on every push and pull request to `dev` and `master`; a separate Guardrails workflow gates changes
to build config and quality-gate files. `master` only advances through the release-automation
workflows — see `IMPLEMENTATION_PLAN.md` §3.9.

## Layout

```
androidApp/     Android entry point — MainActivity, Koin startup, flavor-specific DI
composeApp/     DI root, drawer shell, navigation host
feature/        vertical slices, each split data / domain / dto / mapper / ui
core/           api, domain, storage, navigation, logger, async-kmp, appinfo-api,
                crashreporting-api, crud
uikit/          theme, top app bar, shared widgets
strings/        Compose Multiplatform string resources
utils/          formatters and UI helpers
testing/        hand-written fakes and fixtures (no mocking library)
benchmark/      Android Baseline Profile generation
build-logic/    convention plugins — a module's build file is little more than a plugin list
```

## Documentation

| Doc | What it is |
|---|---|
| [`docs/CHECKLIST.md`](docs/CHECKLIST.md) | The executable plan — numbered, tickable steps, and current progress |
| [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md) | Architecture and the reasoning behind it |
| [`docs/WALLOS_API.md`](docs/WALLOS_API.md) | The Wallos API contract, derived from its PHP source |
| [`docs/security/masvs.md`](docs/security/masvs.md) | The OWASP MASVS security review register |
| [`docs/EMULATOR_TESTING.md`](docs/EMULATOR_TESTING.md) | Driving a real device/emulator to verify a change |
| [`docs/PERF_PROFILING.md`](docs/PERF_PROFILING.md) | Frame-timing, tracing and Baseline Profile technique |
| [`docs/issues/`](docs/issues) | Written-up bug investigations — root cause, options, decision |
| [`CLAUDE.md`](CLAUDE.md) | Coding conventions for this repo |
