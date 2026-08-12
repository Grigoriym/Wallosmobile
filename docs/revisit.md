# Revisit list

Findings worth fixing that are bigger than "small and isolated" — deferred here instead of fixed
inline, per `docs/CHECKLIST.md`'s M17 instructions. Numbered, not dated; each entry stays until
it's actually done, then gets deleted (git has the history).

## 1. `androidLibrary { }` is deprecated in favor of `android { }`

Surfaced as a build-time warning during the Gradle 9.6.1 → 9.7.0 wrapper bump (2026-08-12):
`'fun KotlinMultiplatformExtension.androidLibrary(...)' is deprecated. Please use 'android' instead.`
in `core/storage/build.gradle.kts` and `feature/subscriptions/ui/build.gradle.kts` — the two
modules that configure a KMP Android target directly rather than through `configureKmp()`. Not
fixed inline: migrating the block risks changing Room/Compose `androidDeviceTest` source-set
wiring in ways that need their own on-device verification, out of scope for a wrapper bump.
