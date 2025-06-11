package com.sn4s.muza.di

import android.content.Context
import com.google.gson.Gson
import com.sn4s.muza.data.repository.RecentlyPlayedRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideRecentlyPlayedRepository(
        @ApplicationContext context: Context,
        gson: Gson
    ): RecentlyPlayedRepository {
        return RecentlyPlayedRepository(context, gson)
    }
}