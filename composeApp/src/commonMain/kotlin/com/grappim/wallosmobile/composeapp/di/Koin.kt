package com.grappim.wallosmobile.composeapp.di

import com.grappim.wallosmobile.core.api.NetworkModule
import com.grappim.wallosmobile.core.asynckmp.KmpCoroutinesModule
import com.grappim.wallosmobile.core.storage.StorageModule
import com.grappim.wallosmobile.feature.setup.data.SetupDataModule
import com.grappim.wallosmobile.feature.setup.ui.SetupUiModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

/**
 * The whole graph, listed once. A module class in another Gradle module is invisible to this
 * compilation's `@Configuration` discovery, so it has to be named here — a new feature that
 * forgets this line crashes at first injection, which is what `KoinGraphTest` exists to catch.
 *
 * [StorageModule] is `androidMain`-only (it needs a `Context` for the DataStore path) and is
 * referenced from `commonMain` regardless: Android is the only target, so `commonMain` compiles
 * against the Android variant. A second target turns this into MealieMobile's
 * `expect class PlatformStorageModule`.
 *
 * The `@ComponentScan` picks up this module's own definitions — `DrawerItemsBuilder` today.
 */
@Module(
    includes = [
        KmpCoroutinesModule::class,
        StorageModule::class,
        NetworkModule::class,
        SetupDataModule::class,
        SetupUiModule::class
    ]
)
@Configuration
@ComponentScan("com.grappim.wallosmobile.composeapp")
class AppModule

@KoinApplication
object KoinApp
