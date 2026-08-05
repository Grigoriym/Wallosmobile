package com.grappim.wallosmobile

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.grappim.wallosmobile.composeapp.di.KoinApp
import com.grappim.wallosmobile.core.appinfoapi.AppInfoProvider
import com.grappim.wallosmobile.core.logger.TimberLogger
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.plugin.module.dsl.startKoin
import timber.log.Timber

class WallosApp :
    Application(),
    SingletonImageLoader.Factory {

    private val appInfoProvider: AppInfoProvider by inject()

    private val imageLoader: ImageLoader by inject()

    override fun onCreate() {
        super.onCreate()

        // `androidContext` is not optional: `core:storage`'s DataStore provider takes a `Context`
        // for the store file path (1.4).
        startKoin<KoinApp> {
            androidContext(this@WallosApp)
            printLogger()
        }

        setupLogger()
    }

    /**
     * Coil asks the `Application` for this before it ever builds a default loader, which is what
     * makes it the right place: a `setSafe` call from anywhere in the composition would race the
     * first `AsyncImage` in the same pass.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader

    /** Until this runs, every `logcat { }` in the app is a no-op — 1.1 left it unwired. */
    private fun setupLogger() {
        if (appInfoProvider.isDebug()) {
            Timber.plant(Timber.DebugTree())
        }
        TimberLogger.install()
    }
}
