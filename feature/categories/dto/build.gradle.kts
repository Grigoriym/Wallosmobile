plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `CategoryDTO` implements `CrudResource`, so a consumer that can see the DTO can see
            // the interface it satisfies too.
            api(projects.core.crud)
        }
    }
}
