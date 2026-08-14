import org.gradle.api.artifacts.ProjectDependency

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.jetbrains.compose.compiler) apply false
    alias(libs.plugins.jetbrains.kotlin.multiplatform) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.koin.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false

    // Applied, not `apply false`: the root project both lints its own build scripts and
    // aggregates the per-module Kover reports into a single `koverXmlReport`.
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

kover {
    reports {
        filters {
            excludes {
                // Generates all 4 JVM variants for each suffix:
                // **.*Foo, **.*FooKt, **.*Foo$*, **.*FooKt$*
                fun variants(vararg names: String): Array<String> = names.flatMap { name ->
                    listOf(
                        "**.*$name",
                        "**.*${name}Kt",
                        "**.*$name\$*",
                        "**.*${name}Kt\$*"
                    )
                }.toTypedArray()

                classes(
                    *variants(
                        // Data layer
                        "Api", "ApiImpl", "DTO", "Repository",
                        // Architecture boilerplate
                        "Module", "Plugin", "TimberLogger", "Exception",
                        // App entry points & platform glue
                        "App", "Activity",
                        // UI — composables & navigation
                        "DrawerDestination", "DrawerItem", "IconSource",
                        "UI", "Widget", "Screen", "Content", "Dialog", "BottomSheet",
                        "Destination", "Route", "NavHost",
                        // Compose compiler synthetic lambdas
                        // (always appear as ComposableSingletons$FileKt)
                        "ComposableSingletons"
                    )
                )

                // Compose Multiplatform generated string resources — a large generated package
                // that inflates the denominator but has no testable logic
                packages("com.grappim.wallosmobile.strings.generated.resources")
            }
        }
        total {
            xml { }
            html { }
        }
    }
}

// Every module has to be listed here or it is silently absent from the aggregated report.
// `:testing` is not: it holds fakes and fixtures, not production code.
dependencies {
    kover(project(":composeApp"))

    kover(project(":core:api"))
    kover(project(":core:domain"))
    kover(project(":core:storage"))
    kover(project(":core:navigation"))
    kover(project(":core:async-kmp"))
    kover(project(":core:appinfo-api"))
    kover(project(":core:crashreporting-api"))
    kover(project(":core:logger"))
    kover(project(":core:crud"))

    kover(project(":utils:ui"))
    kover(project(":utils:formatter:decimal"))
    kover(project(":utils:formatter:datetime"))

    kover(project(":uikit"))
    kover(project(":strings"))

    kover(project(":feature:setup:data"))
    kover(project(":feature:setup:domain"))
    kover(project(":feature:setup:dto"))
    kover(project(":feature:setup:ui"))

    kover(project(":feature:settings:ui"))

    kover(project(":feature:subscriptions:data"))
    kover(project(":feature:subscriptions:domain"))
    kover(project(":feature:subscriptions:dto"))
    kover(project(":feature:subscriptions:mapper"))
    kover(project(":feature:subscriptions:ui"))

    kover(project(":feature:categories:data"))
    kover(project(":feature:categories:domain"))
    kover(project(":feature:categories:dto"))
    kover(project(":feature:categories:mapper"))
    kover(project(":feature:categories:ui"))

    kover(project(":feature:household:data"))
    kover(project(":feature:household:domain"))
    kover(project(":feature:household:dto"))
    kover(project(":feature:household:mapper"))
    kover(project(":feature:household:ui"))

    kover(project(":feature:paymentmethods:data"))
    kover(project(":feature:paymentmethods:domain"))
    kover(project(":feature:paymentmethods:dto"))
    kover(project(":feature:paymentmethods:mapper"))
    kover(project(":feature:paymentmethods:ui"))

    kover(project(":feature:currencies:data"))
    kover(project(":feature:currencies:domain"))
    kover(project(":feature:currencies:dto"))
    kover(project(":feature:currencies:mapper"))
    kover(project(":feature:currencies:ui"))

    kover(project(":feature:dashboard:data"))
    kover(project(":feature:dashboard:domain"))
    kover(project(":feature:dashboard:dto"))
    kover(project(":feature:dashboard:ui"))

    kover(project(":feature:profile:data"))
    kover(project(":feature:profile:domain"))
    kover(project(":feature:profile:dto"))
    kover(project(":feature:profile:ui"))
}

// Enforces, as a real configuration-time failure, three of CLAUDE.md's Architecture rules that
// otherwise hold only by convention — ported from duckduckgo-Android's own
// `subprojects { configurations.configureEach { incoming.beforeResolve { ... } } }` mechanism
// (its own checks are Dagger/Anvil module-suffix rules that don't apply to this repo's
// data/domain/dto/mapper/ui shape). Reads `ProjectDependency.path` directly rather than the
// deprecated `.dependencyProject` hop, which reaches into another project's mutable state and is
// exactly what broke `:core:storage`'s KSP wiring under Isolated Projects (see
// `docs/GRADLE_ISOLATED_PROJECTS.md`) — this stays safe for that even with the flag off.
subprojects {
    val subproject = this
    configurations.configureEach {
        val isCompileClasspath = name == "compileClasspath" || name.endsWith("CompileClasspath")
        if (isCompileClasspath) {
            incoming.beforeResolve {
                dependencies.withType<ProjectDependency>().forEach { dependency ->
                    val dependencyPath = dependency.path
                    val projectPath = subproject.path

                    // AGP/KGP's own android-host-test and lint-model machinery resolves a
                    // module's test/lint classpath against a `ProjectDependency` pointing at the
                    // module itself (confirmed via `:composeApp:compileAndroidHostTest` and
                    // `:composeApp:generateAndroidHostTestLintModel`, both real Gradle tasks, not
                    // a real cross-module edge) — a self-dependency can never violate any of these
                    // three rules, so it's excluded before the checks below rather than being
                    // read as `:composeApp -> :composeApp`.
                    if (dependencyPath == projectPath) return@forEach

                    // :benchmark is a com.android.test module and structurally must target
                    // :androidApp (`targetProjectPath`, benchmark/build.gradle.kts) — the one
                    // legitimate exception to "nothing depends on :androidApp".
                    if (dependencyPath == ":androidApp" && projectPath != ":benchmark") {
                        throw GradleException(
                            "Invalid dependency $projectPath -> $dependencyPath: " +
                                "no module may depend on :androidApp"
                        )
                    }
                    if (dependencyPath == ":composeApp" && projectPath != ":androidApp") {
                        throw GradleException(
                            "Invalid dependency $projectPath -> $dependencyPath: " +
                                "only :androidApp may depend on :composeApp"
                        )
                    }
                    if (projectPath == ":core:storage" &&
                        dependencyPath.startsWith(":feature:") &&
                        dependencyPath.endsWith(":domain")
                    ) {
                        throw GradleException(
                            "Invalid dependency $projectPath -> $dependencyPath: " +
                                ":core:storage must not depend on any feature:*:domain module"
                        )
                    }
                }
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
