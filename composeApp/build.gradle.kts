plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.compose)
    // The polymorphic `SerializersModule` in `NavKeySerializers.kt`. The routes it lists are
    // `@Serializable` in their own feature modules — none is declared here any more (2.6).
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

            // `api`, uniquely, because of the Koin compiler plugin (4.5): `AppModule` is the one
            // module class the `startKoin` compilation in `:androidApp` can read, so it re-checks
            // that module's own definitions there — and `provideImageLoader`'s `TrustedCertStorage`
            // has to be resolvable from *that* classpath, definition included.
            api(projects.core.storage)
            implementation(projects.core.asyncKmp)
            implementation(projects.feature.setup.data)
            implementation(projects.feature.setup.ui)
            implementation(projects.feature.settings.ui)
            implementation(projects.feature.subscriptions.data)
            implementation(projects.feature.subscriptions.mapper)
            implementation(projects.feature.subscriptions.ui)
            implementation(projects.feature.categories.data)
            implementation(projects.feature.categories.mapper)
            implementation(projects.feature.household.data)
            implementation(projects.feature.household.mapper)
            implementation(projects.feature.paymentmethods.data)
            implementation(projects.feature.paymentmethods.mapper)
            implementation(projects.feature.dashboard.data)
            implementation(projects.feature.dashboard.domain)
            implementation(projects.utils.formatter.datetime)
            implementation(projects.utils.formatter.decimal)

            // The logo loader's own components: `ImageLoader` plus the Ktor fetcher that
            // replaces Coil's autodiscovered one, so the accepted certificate reaches images too.
            implementation(libs.coil.core)
            implementation(libs.coil.ktor)
        }

        commonTest.dependencies {
            implementation(libs.koin.test)

            // Test-only: `KoinGraphTest` has to name the one interface `:androidApp` supplies.
            implementation(projects.core.appinfoApi)
        }
    }
}
