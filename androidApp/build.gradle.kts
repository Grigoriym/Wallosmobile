plugins {
    alias(libs.plugins.wallosmobile.android.application)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = libs.versions.app.pkg.get()

    defaultConfig {
        applicationId = libs.versions.app.pkg.get()
        testApplicationId = "${libs.versions.app.pkg.get()}.test"

        versionCode = libs.versions.version.code.get().toInt()
        versionName = libs.versions.version.name.get()
    }
}

dependencies {
    baselineProfile(project(":benchmark"))

    implementation(project(":composeApp"))
    implementation(project(":core:appinfo-api"))
    implementation(project(":core:logger"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.annotations)
    implementation(libs.timber)

    // The `Application` is where Coil looks for a `SingletonImageLoader.Factory`; what it hands
    // back is built in `:composeApp`, which is the module that can see the trust-aware engine.
    implementation(libs.coil.singleton)

    implementation(libs.androidx.activity.compose)

    // Makes the profile `:benchmark` generates (baselineProfile(...) above) self-apply on first
    // launch outside Google Play — Play compiles a Play-distributed install with it automatically
    // regardless, but this app also ships an F-Droid flavor, and a plain `adb install`/sideload
    // gets none of that without this (confirmed: `dumpsys package` read `[status=verify]
    // [reason=install]` on a profile-bundling APK installed without it — see
    // docs/issues/2026-08-10-editor-open-stall-and-unapplied-profile.md).
    implementation(libs.androidx.profileinstaller)

    implementation(libs.jetbrains.compose.ui.tooling.preview)
    debugImplementation(libs.jetbrains.compose.ui.tooling)
}
