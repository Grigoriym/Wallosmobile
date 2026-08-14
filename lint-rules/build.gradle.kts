import dev.detekt.gradle.extensions.DetektExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Plain JVM module, no AGP — same shape as `duckduckgo-Android`'s own `lint-rules` module.
// Unlike `build-logic` (a *separate* included build, own `rootDir`, so detekt's config
// auto-discovery never sees this build's `config/detekt/detekt.yml`), `lint-rules` is a normal
// subproject of *this* build — detekt's Gradle plugin auto-discovers `config/detekt/detekt.yml`
// at the shared `rootDir` regardless of whether `config.setFrom` is called here, so it fails at
// configuration time on the shared config's `Compose:` section unless `composeRules-detekt` is on
// the classpath too — confirmed by trying the build-logic shape here first and hitting exactly
// that. Explicit `config.setFrom` + the compose-rules plugins below make that real, the same shape
// `Quality.kt`'s `configureLinting()` uses for every KMP module, rather than fighting the
// auto-discovery.
plugins {
    `java-library`
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

configure<DetektExtension> {
    buildUponDefaultConfig.set(true)
    config.setFrom(File(rootDir, "config/detekt/detekt.yml"))
    source.setFrom(layout.projectDirectory.dir("src"))
}

dependencies {
    compileOnly(libs.android.tools.lint.api)
    compileOnly(libs.android.tools.lint.checks)

    testImplementation(libs.junit4)
    testImplementation(libs.android.tools.lint.api)
    testImplementation(libs.android.tools.lint.tests)

    "detektPlugins"(libs.composeRules.detekt)
    "ktlintRuleset"(libs.composeRules.ktlint)
}

tasks.test {
    useJUnit()
}
