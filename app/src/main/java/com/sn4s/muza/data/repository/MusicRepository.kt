package com.sn4s.muza.data.repository

import android.util.Log
import com.sn4s.muza.data.model.*
import com.sn4s.muza.data.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun login(username: String, password: String): Token {
        Log.d("MusicRepository", "Attempting login for user: $username")
        val response = apiService.login(username, password)
        Log.d("MusicRepository", "Login response: accessToken=${response.accessToken}, tokenType=${response.tokenType}")
        return response
    }

    suspend fun register(user: UserCreate): User {
        return apiService.register(user)
    }

    fun getCurrentUser(): Flow<User> = flow {
        emit(apiService.getCurrentUser())
    }

    fun getSongs(skip: Int = 0, limit: Int = 100): Flow<List<Song>> = flow {
        emit(apiService.getSongs(skip, limit))
    }

    suspend fun getSong(songId: Int): Song {
        return apiService.getSong(songId)
    }

    fun searchSongs(query: String, skip: Int = 0, limit: Int = 20): Flow<List<Song>> = flow {
        emit(apiService.searchSongs(query, skip, limit))
    }

    fun searchArtists(query: String, skip: Int = 0, limit: Int = 20): Flow<List<User>> = flow {
        emit(apiService.searchArtists(query, skip, limit))
    }

    fun searchAlbums(query: String, skip: Int = 0, limit: Int = 20): Flow<List<Album>> = flow {
        emit(apiService.searchAlbums(query, skip, limit))
    }

    fun searchPlaylists(query: String, skip: Int = 0, limit: Int = 20): Flow<List<Playlist>> = flow {
        emit(apiService.searchPlaylists(query, skip, limit))
    }

    fun getUserPlaylists(skip: Int = 0, limit: Int = 100): Flow<List<Playlist>> = flow {
        emit(apiService.getUserPlaylists(skip, limit))
    }

    fun getUserAlbums(skip: Int = 0, limit: Int = 100): Flow<List<Album>> = flow {
        emit(apiService.getUserAlbums(skip, limit))
    }

    fun getLikedSongs(skip: Int = 0, limit: Int = 100): Flow<List<Song>> = flow {
        emit(apiService.getLikedSongs(skip, limit))
    }
} 