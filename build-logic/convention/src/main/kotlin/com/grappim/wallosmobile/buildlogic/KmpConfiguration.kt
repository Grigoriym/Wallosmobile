package com.grappim.wallosmobile.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * The single edit point for platform targets.
 *
 * WallosMobile is Android-only for now, and the Android target itself is declared by
 * `com.android.kotlin.multiplatform.library` in `KmpLibraryConventionPlugin` — so this
 * function adds no targets at all. `jvm()`, `iosArm64()` and `iosSimulatorArm64()` go
 * here, and nowhere else, when those apps arrive.
 */
fun Project.configureKmp() {
    extensions.configure<KotlinMultiplatformExtension> {
        jvmToolchain(21)
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }

        sourceSets.apply {
            commonMain.dependencies {
                implementation(libs.findLibrary("kotlinx.coroutines.core").get())
                implementation(libs.findLibrary("kotlinx.collections").get())
                implementation(libs.findLibrary("kotlinx.date.time").get())

                // `core:logger` doesn't exist until the module skeletons are created;
                // the null check is what lets this plugin be used before then.
                val logger = findProject(":core:logger")
                if (logger != null && project.path != ":core:logger") {
                    implementation(logger)
                }
            }
        }
    }
}
