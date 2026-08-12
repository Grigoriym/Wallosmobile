package com.grappim.wallosmobile.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val JDK_VERSION = 21

/**
 * The single edit point for platform targets.
 *
 * WallosMobile is Android-only for now, and the Android target itself is declared by
 * `com.android.kotlin.multiplatform.library` in `KmpLibraryConventionPlugin` — so this
 * function adds no targets at all. `jvm()`, `iosArm64()` and `iosSimulatorArm64()` go
 * here, and nowhere else, when those apps arrive.
 */
fun Project.configureKmp() {
    // Android-only, so `testAndroidHostTest` is the *only* source of coverage — unlike
    // TaigaMobileNova, which disables instrumentation for the Android unit test tasks
    // because it measures coverage on `jvmTest`.
    pluginManager.apply("org.jetbrains.kotlinx.kover")

    extensions.configure<KotlinMultiplatformExtension> {
        jvmToolchain(JDK_VERSION)
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }

        sourceSets.apply {
            commonMain.dependencies {
                implementation(libs.findLibrary("kotlinx.coroutines.core").get())
                implementation(libs.findLibrary("kotlinx.collections").get())
                implementation(libs.findLibrary("kotlinx.date.time").get())

                // Logging is available everywhere without a per-module declaration.
                // Guarded only against the module depending on itself — a mis-typed path
                // must fail the build, not be silently skipped.
                if (project.path != ":core:logger") {
                    implementation(project(":core:logger"))
                }
            }
        }
    }
}
