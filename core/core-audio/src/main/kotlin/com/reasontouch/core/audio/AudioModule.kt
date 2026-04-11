package com.reasontouch.core.audio

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideSynthEngine(): SynthEngine = SynthEngine()

    @Provides
    @Singleton
    fun provideSf2Player(app: Application): Sf2Player =
        Sf2Player(app.assets)
}