package com.sn4s.muza.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sn4s.muza.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentlyPlayedRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val prefs = context.getSharedPreferences("recently_played", Context.MODE_PRIVATE)

    private val _recentlyPlayed = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayed: StateFlow<List<Song>> = _recentlyPlayed.asStateFlow()

    companion object {
        private const val SONGS_KEY = "songs"
        private const val MAX_RECENTLY_PLAYED = 50
    }

    init {
        loadRecentlyPlayed()
    }

    fun addSong(song: Song) {
        val current = _recentlyPlayed.value.toMutableList()

        // Remove if already exists to avoid duplicates
        current.removeAll { it.id == song.id }

        // Add to beginning
        current.add(0, song)

        // Keep only last MAX_RECENTLY_PLAYED songs
        if (current.size > MAX_RECENTLY_PLAYED) {
            current.removeAt(current.size - 1)
        }

        _recentlyPlayed.value = current
        saveRecentlyPlayed(current)
    }

    fun getRecentlyPlayedList(): List<Song> {
        return _recentlyPlayed.value
    }

    fun clearRecentlyPlayed() {
        _recentlyPlayed.value = emptyList()
        prefs.edit()
            .remove(SONGS_KEY)
            .apply()
    }

    private fun loadRecentlyPlayed() {
        val json = prefs.getString(SONGS_KEY, null)
        if (json != null) {
            try {
                val songs: List<Song> = gson.fromJson(json, object : TypeToken<List<Song>>() {}.type)
                _recentlyPlayed.value = songs
            } catch (e: Exception) {
                // If parsing fails, start with empty list
                _recentlyPlayed.value = emptyList()
                clearRecentlyPlayed()
            }
        }
    }

    private fun saveRecentlyPlayed(songs: List<Song>) {
        try {
            val json = gson.toJson(songs)
            prefs.edit()
                .putString(SONGS_KEY, json)
                .apply()
        } catch (e: Exception) {
            // Handle serialization error
        }
    }
}