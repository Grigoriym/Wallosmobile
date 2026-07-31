# WallosMobile

An **unofficial** Kotlin Multiplatform client for [Wallos](https://github.com/ellite/Wallos), the
self-hosted subscription tracker. Not affiliated with the Wallos project.

> **Status: early development.** The foundation is in place — module structure, convention
> plugins, quality gates and CI — but there is no working UI yet.
> [`docs/CHECKLIST.md`](docs/CHECKLIST.md) is the single record of how far along it is.

## Platforms

**Android only for now.** The project is built KMP-first: all source lives in `commonMain`, and
platform targets are declared in one function in `build-logic`, so adding iOS or Desktop later is
a build-logic change rather than a refactor.

## Planned for v1

Username/password onboarding against your own Wallos instance, the subscriptions list, and a
subscription detail screen. Writes, the dashboard, the management screens and offline caching come
after — see [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md) §8 for the phase order.

Wallos has no login API, so onboarding drives the web login once and bridges to the per-user API
key the JSON API actually uses (plan §1.1).

## Tech stack

Kotlin Multiplatform · Compose Multiplatform · Navigation 3 · Koin (compiler plugin, not KSP) ·
Ktor · kotlinx.serialization · DataStore · Coil · MVVM + Clean Architecture in vertical feature
slices · detekt, ktlint, compose-rules, Kover.

## Build commands

```bash
./gradlew :androidApp:assembleDebug          # build
./gradlew allTests                           # all module tests
./gradlew :module:path:testAndroidHostTest   # one module
./gradlew detekt ktlintCheck                 # lint
./gradlew koverHtmlReport                    # coverage
```

Requires JDK 21. There is no `jvmTest` task — the project declares no `jvm()` target, so unit
tests run as the AGP KMP host test (`testAndroidHostTest`) over `commonTest`.

CI runs assemble, `allTests`, detekt and ktlint on every push and pull request to `master`.

## Layout

```
androidApp/     Android entry point — MainActivity, Koin startup
composeApp/     DI root, drawer shell, navigation host
feature/        vertical slices, each split data / domain / dto / mapper / ui
core/           api, domain, storage, navigation, logger, async-kmp, appinfo-api
uikit/          theme, top app bar, shared widgets
strings/        Compose Multiplatform string resources
utils/          formatters and UI helpers
testing/        hand-written fakes and fixtures (no mocking library)
build-logic/    convention plugins — a module's build file is little more than a plugin list
```

## Documentation

| Doc | What it is |
|---|---|
| [`docs/CHECKLIST.md`](docs/CHECKLIST.md) | The executable plan — numbered, tickable steps, and current progress |
| [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md) | Architecture and the reasoning behind it |
| [`docs/WALLOS_API.md`](docs/WALLOS_API.md) | The Wallos API contract, derived from its PHP source |
| [`CLAUDE.md`](CLAUDE.md) | Coding conventions for this repo |
