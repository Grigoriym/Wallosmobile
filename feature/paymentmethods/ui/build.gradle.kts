plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.compose)
    alias(libs.plugins.wallosmobile.kmp.di)
    // `PaymentMethodsRoute`/`PaymentMethodEditorRoute` are `@Serializable` `NavKey`s — the shell
    // serializes them into the back stack, so this module needs serialization like
    // `feature:household:ui`.
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        // The icon picker (9.5) is this module's first reason to declare `androidMain` — every
        // other call reaches the server through `core:api` alone. `feature:subscriptions:ui`'s
        // `LogoPicker.android.kt` (7.9) is the precedent this mirrors.
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }

        commonMain.dependencies {
            implementation(projects.feature.paymentmethods.domain)

            implementation(projects.uikit)
            implementation(projects.strings)

            // For `BaseUrlProvider` alone: `PaymentMethod.icon` is already relative to the
            // instance root (`WALLOS_API.md` §4), and that is the one place the root is normalized.
            implementation(projects.core.api)

            // The list row's icon render — the `ImageLoader` singleton (trust-aware engine, 3.12)
            // is wired once in `AppModule`, this module only needs Coil's compose API.
            implementation(libs.coil.compose)
        }

        commonTest.dependencies {
            // Test-only, as in `feature:household:ui`: production code never names a
            // `WallosError`, it hands whatever it caught to `getErrorMessage`.
            implementation(projects.core.domain)
        }
    }
}
