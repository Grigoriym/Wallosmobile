import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.grappim.wallosmobile.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.compose.multiplatform.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
    compileOnly(libs.kover.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.wallosmobile.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("kmpLibrary") {
            id = libs.plugins.wallosmobile.kmp.library.asProvider().get().pluginId
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpLibraryCompose") {
            id = libs.plugins.wallosmobile.kmp.library.compose.get().pluginId
            implementationClass = "KmpLibraryComposeConventionPlugin"
        }
        register("kmpLibraryStability") {
            id = libs.plugins.wallosmobile.kmp.library.stability.get().pluginId
            implementationClass = "KmpLibraryStabilityConventionPlugin"
        }
        register("kmpSerialization") {
            id = libs.plugins.wallosmobile.kmp.serialization.get().pluginId
            implementationClass = "KmpSerializationConventionPlugin"
        }
        register("kmpDi") {
            id = libs.plugins.wallosmobile.kmp.di.get().pluginId
            implementationClass = "KmpDiConventionPlugin"
        }
        register("kmpNetwork") {
            id = libs.plugins.wallosmobile.kmp.network.get().pluginId
            implementationClass = "KmpNetworkConventionPlugin"
        }
    }
}
