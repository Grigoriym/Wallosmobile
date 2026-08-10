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

    implementation(libs.jetbrains.compose.ui.tooling.preview)
    debugImplementation(libs.jetbrains.compose.ui.tooling)
}
