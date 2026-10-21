package com.reasontouch.core.audio

import android.content.Context
import android.content.res.AssetManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideSynthEngine(): SynthEngine = SynthEngine()

    @Provides
    fun provideAssetManager(@ApplicationContext context: Context): AssetManager = context.assets
}
