package com.sn4s.muza.di

import android.util.Log
import com.sn4s.muza.data.model.RefreshTokenRequest
import com.sn4s.muza.data.network.ApiService
import com.sn4s.muza.data.security.TokenManager
import com.sn4s.muza.utils.ApiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    val BASE_URL = ApiConfig.BASE_URL

    private val _unauthorizedEvent = MutableSharedFlow<Unit>()
    val unauthorizedEvent: SharedFlow<Unit> = _unauthorizedEvent

    @Provides
    @Singleton
    fun provideTokenAuthenticator(tokenManager: TokenManager): Authenticator {
        return Authenticator { _, response ->
            val token = tokenManager.getToken()
            if (token?.refreshToken == null) {
                // No refresh token, no auth
                CoroutineScope(Dispatchers.Main).launch {
                    _unauthorizedEvent.emit(Unit)
                }
                return@Authenticator null
            }

            try {
                // Create a simple client for refresh call
                val refreshClient = OkHttpClient.Builder().build()
                val refreshRetrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(refreshClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val refreshService = refreshRetrofit.create(ApiService::class.java)

                val refreshRequest = RefreshTokenRequest(token.refreshToken)
                val newToken = runBlocking { refreshService.refreshToken(refreshRequest) }
                tokenManager.saveToken(newToken)

                // Return new request with fresh token
                response.request.newBuilder()
                    .header("Authorization", "${newToken.tokenType} ${newToken.accessToken}")
                    .build()
            } catch (e: Exception) {
                Log.e("NetworkModule", "Token refresh failed", e)
                tokenManager.clearToken()
                CoroutineScope(Dispatchers.Main).launch {
                    _unauthorizedEvent.emit(Unit)
                }
                null
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenManager: TokenManager,
        authenticator: Authenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val token = tokenManager.getToken()

                val request = if (token != null) {
                    original.newBuilder()
                        .header("Authorization", "${token.tokenType} ${token.accessToken}")
                        .build()
                } else original

                chain.proceed(request)
            }
            .authenticator(authenticator)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}