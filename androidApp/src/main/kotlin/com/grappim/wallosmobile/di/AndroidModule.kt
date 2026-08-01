package com.grappim.wallosmobile.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

/**
 * `androidApp` sits *above* `composeApp`, so `AppModule` cannot include this one. It is picked up
 * because `@Configuration` modules in the compilation that calls `startKoin` are gathered
 * automatically — the same arrangement MealieMobile and TaigaMobileNova use.
 */
@Module
@Configuration
@ComponentScan("com.grappim.wallosmobile.di")
class AndroidModule
