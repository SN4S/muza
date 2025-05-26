package com.sn4s.muza.data.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sn4s.muza.data.model.Token
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: Token) {
        try {
            Log.d("TokenManager", "Saving token: ${token.tokenType} ${token.accessToken}")
            sharedPreferences.edit().apply {
                putString(KEY_ACCESS_TOKEN, token.accessToken)
                putString(KEY_REFRESH_TOKEN, token.refreshToken)
                putString(KEY_TOKEN_TYPE, token.tokenType)
                commit()
            }
            Log.d("TokenManager", "Token saved successfully")
        } catch (e: Exception) {
            Log.e("TokenManager", "Error saving token", e)
        }
    }

    fun getToken(): Token? {
        try {
            val accessToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
            val refreshToken = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
            val tokenType = sharedPreferences.getString(KEY_TOKEN_TYPE, null)

            return if (accessToken != null && refreshToken != null && tokenType != null) {
                Token(accessToken, refreshToken, tokenType)
            } else {
                Log.d("TokenManager", "No token found in storage")
                null
            }
        } catch (e: Exception) {
            Log.e("TokenManager", "Error retrieving token", e)
            return null
        }
    }

    fun clearToken() {
        try {
            Log.d("TokenManager", "Clearing token")
            sharedPreferences.edit().clear().commit() // Use commit() instead of apply()
            Log.d("TokenManager", "Token cleared successfully")
        } catch (e: Exception) {
            Log.e("TokenManager", "Error clearing token", e)
        }
    }

    private companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_TYPE = "token_type"
    }
} 