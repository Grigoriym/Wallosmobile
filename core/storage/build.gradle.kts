plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.di)
    // `TrustedCertStorageImpl` JSON-encodes `PendingCertTrust` for the DataStore-backed pin list
    // (18.1) — the type itself lives in core:domain, and this module has no dependency the other
    // way, same as core:api's own core.domain + core.storage pair.
    alias(libs.plugins.wallosmobile.kmp.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    // The DAO tests are instrumented, and have to be: on the Android target `Room`'s only
    // builders take a `Context`, and `BundledSQLiteDriver`'s native library ships in the aar's
    // `jni/` — neither is reachable from `testAndroidHostTest`. See the 3.3 note in
    // docs/CHECKLIST.md. `sourceSetTreeName = null` keeps `androidDeviceTest` out of the `test`
    // source-set tree, so `commonTest` is *not* also compiled onto the device.
    android {
        withDeviceTestBuilder { sourceSetTreeName = null }
            .configure { instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.datastore.android)
            implementation(libs.androidx.room.ktx)
        }
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(libs.androidx.datastore.core)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.androidx.test.runner)
        }
    }
}

room {
    // The generated JSON is the only record of what shipped, so it is committed, not ignored.
    schemaDirectory("$projectDir/schemas")
}

// TaigaMobileNova declares four of these, one per KSP target. Android is the only target here.
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
}
