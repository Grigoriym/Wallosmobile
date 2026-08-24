import dev.detekt.gradle.extensions.DetektExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Plain JVM module, no AGP — same shape as `:lint-rules` used to have (M25, subsumed and dropped
// by this module, M26). detekt's Gradle plugin auto-discovers `config/detekt/detekt.yml` at the
// shared `rootDir` regardless of whether `config.setFrom` is called here, so it fails at
// configuration time on the shared config's `Compose:` section unless `composeRules-detekt` is on
// the classpath too — the same reason the explicit `config.setFrom` + compose-rules plugins below
// are needed at all.
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
    compileOnly(libs.detekt.api)

    testImplementation(libs.junit4)
    testImplementation(libs.detekt.api)
    testImplementation(libs.detekt.test.utils)
    // `detekt-test`'s `runtimeElements` variant requests the `detekt-api-test-fixtures`
    // capability, but `detekt-api` only ever publishes a *sources* jar under that capability
    // (confirmed on both 2.0.0-alpha.5 and -alpha.6 on Maven Central) — no classes jar exists to
    // satisfy it, so a normal `testImplementation` fails resolution outright. `isTransitive =
    // false` skips that broken dependency edge; the two things this module's tests actually
    // reach into `detekt-test` for (`TestConfig`, the `Rule.lint(String)` extension) need nothing
    // from the missing test-fixtures artifact.
    testImplementation(libs.detekt.test) { isTransitive = false }
    testImplementation("org.jetbrains.kotlin:kotlin-compiler:2.4.0")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:2.4.0")

    "detektPlugins"(libs.composeRules.detekt)
    "ktlintRuleset"(libs.composeRules.ktlint)
    // The shared config's own `WallosMobile:` block (this module's own ruleset) is invalid
    // config without a provider for it on the classpath too, same reason as the two lines above
    // for `Compose:` — a self-dependency, but not a cycle: `detektPlugins` only consumes this
    // project's `jar` output, which the `detekt` task doesn't feed into.
    "detektPlugins"(project(":detekt-rules"))
}

tasks.test {
    useJUnit()
}
