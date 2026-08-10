plugins {
    alias(libs.plugins.wallosmobile.android.application)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

// Applied only for the gplay flavor — F-Droid's build must not carry these plugins at all
// (they inject Firebase-related string resources regardless of flavor's dependency graph,
// which breaks both F-Droid's non-free scanner and its binary reproducibility check).
// The plugins {} block can't express this condition itself (it has no access to `project`
// or `providers`), so the plugin IDs are pulled from the catalog and applied imperatively.
if (project.hasProperty("gplayBuild")) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
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
    implementation(project(":core:crashreporting-api"))
    implementation(project(":core:logger"))

    // `core:storage` itself reaches androidApp transitively via composeApp's `api` dependency on
    // it (composeApp/build.gradle.kts) — only `core:async-kmp` (an `implementation` dependency
    // there) needs its own line here, for `ApplicationScope`.
    implementation(project(":core:async-kmp"))

    // Neither reaches androidApp transitively: composeApp depends on both only via
    // `implementation` (composeApp/build.gradle.kts), so a direct consumer here — MainActivity
    // resolving the update-snackbar strings and constructing the shared `SnackbarHostController`
    // (16.5 follow-up) — needs its own line for each, same rule as `core:async-kmp` above.
    implementation(project(":strings"))
    implementation(project(":uikit"))

    // `SnackbarHostController.show()` (`uikit`) takes/returns `SnackbarDuration`/`SnackbarResult`
    // — `uikit` only depends on material3 via `implementation` (`configureKmpCompose()`), so those
    // types don't reach a direct caller here either without their own line.
    implementation(libs.jetbrains.compose.material3)

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

    // Crashlytics ships in the gplay flavor only — the fdroid flavor never pulls in
    // this proprietary dependency, only a no-op CrashReporter implementation.
    gplayImplementation(platform(libs.firebase.bom))
    gplayImplementation(libs.firebase.crashlytics)

    // Play In-App Updates ship in the gplay flavor only — the fdroid flavor never pulls in
    // this proprietary dependency, only a no-op AppUpdateChecker implementation.
    gplayImplementation(libs.google.inapp.update.ktx)
}
