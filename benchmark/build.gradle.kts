import dev.detekt.gradle.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

// Not a `wallosmobile.*` convention-plugin consumer — `Quality.kt`'s `configureLinting()`
// can't be called directly from a plain subproject script (its `import` doesn't resolve
// here the way it does inside `build-logic`'s own compiled Kotlin), so the same
// detekt/ktlint settings every other module gets are inlined instead, rather than leaving
// this module's own Kotlin source ungated.
configure<DetektExtension> {
    buildUponDefaultConfig.set(true)
    parallel.set(true)
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    allRules.set(false)
    source.setFrom(layout.projectDirectory.dir("src"))
}

configure<KtlintExtension> {
    version.set("1.8.0")
    android.set(true)
    ignoreFailures.set(false)
    verbose.set(true)
    outputColorName.set("RED")
    outputToConsole.set(true)
}

android {
    namespace = "com.grappim.wallosmobile.benchmark"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Generation runs on `emulator-testing`'s AVD, already booted for on-device Verify:
    // lines throughout this project — no Gradle Managed Device needed on top of that.
    targetProjectPath = ":androidApp"

    // Mirrors `AppFlavors`/`FlavorDimensions` (`build-logic`) by name rather than reusing
    // them directly — this module isn't a `wallosmobile.*` convention-plugin consumer, and
    // pulling `com.grappim.wallosmobile.buildlogic` symbols into a plain subproject script
    // would need its own dependency wiring for two enum values. Without a matching "STORE"
    // dimension here, `:androidApp`'s `gplayNonMinifiedRelease`/`fdroidNonMinifiedRelease`
    // variants become ambiguous to this module's own unflavored one (confirmed: an
    // unflavored `:benchmark` fails `generateGplayReleaseBaselineProfile` with a Gradle
    // variant-attribute-ambiguity error between the two flavors' `RuntimeElements`).
    flavorDimensions += "STORE"
    productFlavors {
        create("gplay") { dimension = "STORE" }
        create("fdroid") { dimension = "STORE" }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)

    // The shared `config/detekt/detekt.yml`'s `Compose:` section is invalid config without
    // this plugin present, in *every* module regardless of whether it has Compose code —
    // confirmed live: `:benchmark:detekt` failed with "Property 'Compose' is misspelled or
    // does not exist" until this was added, exactly why `Quality.kt`'s `configureLinting()`
    // adds both unconditionally rather than only to Compose modules.
    ktlintRuleset(libs.composeRules.ktlint)
    detektPlugins(libs.composeRules.detekt)
}
