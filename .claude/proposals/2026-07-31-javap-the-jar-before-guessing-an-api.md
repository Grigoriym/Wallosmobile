# Proposal: read the actual API off the jar before writing against a library signature

## What

When writing a call against a library whose exact signature you're unsure of — an
overload, whether a parameter exists, what a property is called in this major version —
don't write it from memory and let the compiler tell you. Read it off the artifact
that's already in the Gradle cache:

```bash
find ~/.gradle/caches -name "ktor-client-core-jvm-3.5.1.jar" | head -1
javap -classpath <that jar> 'io.ktor.client.plugins.HttpRequestRetryConfig' | grep -i retry
```

`javap` prints every public member with its erased signature, including the ones the
docs for the *previous* major version still describe. Two greps beat two compile cycles,
and a KMP compile cycle is 15+ seconds.

Works for any JVM dependency already resolved by the build; the `-jvm` variant of a
KMP artifact is the one to open.

## Why shared

This is not a WallosMobile fact. Every Kotlin/Gradle project here (TaigaMobileNova,
MealieMobile, WallosMobile) pins fast-moving libraries — Ktor 3.x, AGP 9.x, Compose,
Koin — where a plausible-looking signature from an older version compiles in the model's
head and not on disk. It came up here as Ktor 3.5.1's `HttpRequestRetry`: the
config method has no `retryOnTimeout` parameter, hands the predicate an
`HttpRequestBuilder` rather than an `HttpRequest`, and the `URLBuilder` it exposes has
`encodedPathSegments` but no `encodedPath`. Three details, one failed compile, all four
visible in one `javap` line.

## Target

An edit, not a new skill. It belongs wherever the shared setup already gives guidance on
working in Kotlin/Gradle projects — a "verify the API before you write it" note next to
the existing build-command guidance. If there is no such home yet, a short shared
reference skill (`jvm-api-lookup`) would carry it.

## Suggested text

> **Check a library signature against the jar, not against memory.** For any dependency
> already resolved by the build:
> `javap -classpath $(find ~/.gradle/caches -name '<artifact>-<version>.jar' | head -1) 'fully.qualified.ClassName'`
> Fast-moving libraries (Ktor, AGP, Compose, Koin) rename and drop members across minor
> versions, and a signature that looks right is the most expensive kind of wrong — it
> costs a full compile cycle to disprove. Use the `-jvm` variant of a KMP artifact.

## Source

WallosMobile, 2026-07-31, checklist step 1.3 (`core:api` HTTP clients).
