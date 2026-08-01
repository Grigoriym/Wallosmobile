plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.compose)
    // `@Serializable` routes, and the polymorphic `SerializersModule` in `NavKeySerializers.kt`.
    alias(libs.plugins.wallosmobile.kmp.serialization)
    // The DI root: `AppModule` lists every module class, so this one sees them all.
    alias(libs.plugins.wallosmobile.kmp.di)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.navigation)
            implementation(projects.uikit)
            implementation(projects.strings)

            // Named by `AppModule`'s `includes`, and `core:storage` also feeds the startup branch.
            implementation(projects.core.api)
            implementation(projects.core.storage)
            implementation(projects.core.asyncKmp)
            implementation(projects.feature.setup.data)
            implementation(projects.feature.setup.ui)
            implementation(projects.feature.subscriptions.data)
            implementation(projects.feature.subscriptions.mapper)
            implementation(projects.feature.subscriptions.ui)
            implementation(projects.utils.formatter.datetime)
            implementation(projects.utils.formatter.decimal)
        }

        commonTest.dependencies {
            implementation(libs.koin.test)

            // Test-only: `KoinGraphTest` has to name the one interface `:androidApp` supplies.
            implementation(projects.core.appinfoApi)
        }
    }
}
