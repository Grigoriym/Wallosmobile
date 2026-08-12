package com.grappim.wallosmobile.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

// Opt-in Compose Compiler stability audit — see docs/compose/stability-reports-plan.md.
// Off by default: -PcomposeStabilityReport to generate *-classes.txt / *-composables.txt.
fun Project.configureComposeStabilityReports() {
    if (!project.hasProperty("composeStabilityReport")) return
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        metricsDestination.set(layout.buildDirectory.dir("compose_reports"))
        reportsDestination.set(layout.buildDirectory.dir("compose_reports"))
    }
}
