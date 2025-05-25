package com.sn4s.muza.di

import android.content.Context
import com.sn4s.muza.player.MusicPlayerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideMusicPlayerManager(
        @ApplicationContext context: Context
    ): MusicPlayerManager {
        return MusicPlayerManager(context)
    }
}