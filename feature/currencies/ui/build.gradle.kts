plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.compose)
    alias(libs.plugins.wallosmobile.kmp.di)
    // `CurrenciesRoute`/`CurrencyEditorRoute` are `@Serializable` `NavKey`s — the shell serializes
    // them into the back stack, so this module needs serialization like `feature:household:ui`.
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.currencies.domain)

            implementation(projects.uikit)
            implementation(projects.strings)
            // `getErrorMessage`/`ObserveAsEvents` — no longer reachable transitively through
            // `uikit`, which now depends on `grappim-kit-uikit` instead of `utils:ui`.
            implementation(projects.utils.ui)
        }

        commonTest.dependencies {
            // Test-only, as in `feature:household:ui`: production code never names a
            // `WallosError`, it hands whatever it caught to `getErrorMessage`.
            implementation(projects.core.domain)
        }
    }
}
