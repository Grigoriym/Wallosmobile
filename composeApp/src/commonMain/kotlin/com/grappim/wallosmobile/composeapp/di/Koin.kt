package com.grappim.wallosmobile.composeapp.di

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.grappim.wallosmobile.core.api.NetworkModule
import com.grappim.wallosmobile.core.api.createPlatformHttpClientEngine
import com.grappim.wallosmobile.core.asynckmp.KmpCoroutinesModule
import com.grappim.wallosmobile.core.storage.StorageModule
import com.grappim.wallosmobile.core.storage.cert.TrustedCertStorage
import com.grappim.wallosmobile.feature.categories.data.CategoriesDataModule
import com.grappim.wallosmobile.feature.categories.mapper.CategoriesMapperModule
import com.grappim.wallosmobile.feature.settings.ui.SettingsUiModule
import com.grappim.wallosmobile.feature.setup.data.SetupDataModule
import com.grappim.wallosmobile.feature.setup.ui.SetupUiModule
import com.grappim.wallosmobile.feature.subscriptions.data.SubscriptionsDataModule
import com.grappim.wallosmobile.feature.subscriptions.mapper.SubscriptionsMapperModule
import com.grappim.wallosmobile.feature.subscriptions.ui.SubscriptionsUiModule
import com.grappim.wallosmobile.utils.formatter.datetime.DateTimeFormatterModule
import com.grappim.wallosmobile.utils.formatter.decimal.DecimalFormatterModule
import io.ktor.client.HttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

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
        SetupUiModule::class,
        SettingsUiModule::class,
        SubscriptionsDataModule::class,
        SubscriptionsMapperModule::class,
        SubscriptionsUiModule::class,
        CategoriesDataModule::class,
        CategoriesMapperModule::class,
        DateTimeFormatterModule::class,
        DecimalFormatterModule::class
    ]
)
@Configuration
@ComponentScan("com.grappim.wallosmobile.composeapp")
class AppModule {

    /**
     * Left alone, Coil builds its own `HttpClient` through a `FetcherServiceLoaderTarget`, which
     * never sees [createPlatformHttpClientEngine]'s trust manager — so on a self-signed instance
     * the subscriptions load and none of their logos do (3.12). Naming the fetcher explicitly is
     * what puts the certificate the user accepted underneath the image loads too.
     *
     * Its own minimal client, deliberately not the `@Single HttpClient`: that one carries
     * `Logging(LogLevel.ALL)` in debug, which would dump every logo's bytes into logcat, and a
     * retry predicate written for `get_*.php`. The engine is the only part that carries the pin,
     * and it is the only part shared.
     *
     * Built here rather than in a class of its own because [NetworkModule] builds its clients the
     * same way. What is *not* a free choice is `core:storage` being an `api` dependency of this
     * module: the compiler plugin re-checks the definitions of every `@Configuration` class the
     * `startKoin` compilation can read — `AppModule` is one, its `includes` are not — so both
     * [TrustedCertStorage] and the definition that binds it have to be resolvable from
     * `:androidApp`'s classpath, whether this is a provider function or a scanned class.
     */
    @Single
    fun provideImageLoader(context: PlatformContext, trustedCertStorage: TrustedCertStorage): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(
                    KtorNetworkFetcherFactory(
                        HttpClient(createPlatformHttpClientEngine(trustedCertStorage))
                    )
                )
            }
            .build()
}

@KoinApplication
object KoinApp
