# Proposal — verify Koin compiler-plugin definitions by reading the bytecode

## What

With `io.insert-koin.compiler.plugin` (as opposed to classic KSP annotation processing) there is
no `build/generated/ksp/**` to open, so the usual advice is "a clean compile is the only signal
that a new `@Single` was picked up". That is not true: the plugin writes a generated module class
into the ordinary class output, one `module$lambda` per definition, and `javap` reads it back.

```bash
javap -p -c <module>/build/classes/kotlin/android/main/<pkg-path>/<GeneratedModuleName>.class \
  | grep "private static final"
```

The generated class is named after the `@Module` class with the package folded into the name —
`com.grappim.wallosmobile.core.storage.StorageModule` becomes
`ComGrappimWallosmobileCoreStorageStorageModuleModuleKt` — and sits in the module class's own
package. Each line names both the type being constructed and the types it resolves from the
`Scope`, so a missed `@ComponentScan`, a definition that silently didn't register, and a wrong
constructor dependency are all visible without starting Koin.

`build/classes/kotlin/android/main/org/koin/plugin/hints/` also gets `…DefinitionKt` marker
classes, but it is **not** a reliable index — a definition can be in the generated module and have
no hint file. Read the module class, not the hints directory.

## Why shared

This came up wiring `core:storage` in WallosMobile, where the DI graph cannot be instantiated at
all until a later checklist step, so "start the app and see if it crashes" was not available. The
same plugin, and the same lack of an inspectable artifact, applies to TaigaMobileNova and
MealieMobile. The technique is about the Koin compiler plugin's output shape, not about anything
WallosMobile-specific.

It also answers a question the `koin-expert` agent currently cannot: that agent's remedies assume
a runnable graph (`KoinGraphTest`, `checkModules`). This works before there is one.

## Target

`agents/koin-expert.md` — an edit. Add the bytecode check as a diagnostic step ahead of the
graph-assertion remedies, for the case where the graph can't be started yet.

Possibly also worth a line wherever the "no `build/generated/ksp/**` to inspect" claim is repeated,
since that claim is what stops people looking.

## Suggested text

> **Before the graph can run.** The compiler plugin has no KSP output directory, but it does emit
> a generated module class next to the compiled `@Module` class. To confirm a `@ComponentScan`
> found a definition — including `internal` classes, and classes in a different source set from
> the module class — disassemble it:
>
> ```bash
> javap -p -c <module>/build/classes/kotlin/android/main/<pkg>/<ModuleName>ModuleKt.class \
>   | grep "private static final"
> ```
>
> One line per definition, showing the constructed type and every `Scope.get` it depends on. The
> `org/koin/plugin/hints/` classes are not a complete index — a registered definition may have no
> hint file, so read the module class itself.

## Source

WallosMobile (`/home/gregory/proj/grappim/wallosmobile`), checklist step 1.4, 2026-08-01.
Koin BOM 4.2.2, koin-annotations 4.2.2, Kotlin 2.4.10.
