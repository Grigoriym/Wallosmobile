plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.jetbrains.compose.compiler) apply false
    alias(libs.plugins.jetbrains.kotlin.multiplatform) apply false
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

dependencies {
    kover(project(":composeApp"))
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
