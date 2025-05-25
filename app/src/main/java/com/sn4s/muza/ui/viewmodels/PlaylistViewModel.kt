package com.sn4s.muza.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Playlist
import com.sn4s.muza.data.model.PlaylistCreate
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.getUserPlaylists()
                    .catch { e ->
                        Log.e("PlaylistViewModel", "Failed to load playlists", e)
                        _error.value = e.message ?: "Failed to load playlists"
                    }
                    .collect { playlists ->
                        _playlists.value = playlists
                        Log.d("PlaylistViewModel", "Loaded ${playlists.size} playlists")
                    }
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error loading playlists", e)
                _error.value = e.message ?: "Error loading playlists"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPlaylist(name: String, description: String? = null) {
        if (name.isBlank()) {
            _error.value = "Playlist name cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val playlistCreate = PlaylistCreate(name = name, description = description)
                val createdPlaylist = repository.createPlaylist(playlistCreate)

                // Add to current list immediately for better UX
                _playlists.value = _playlists.value + createdPlaylist

                Log.d("PlaylistViewModel", "Created playlist: ${createdPlaylist.name}")
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Failed to create playlist", e)
                _error.value = e.message ?: "Failed to create playlist"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSongToPlaylist(playlistId: Int, songId: Int) {
        viewModelScope.launch {
            try {
                repository.addSongToPlaylist(playlistId, songId)
                // Refresh playlists to update song counts
                loadPlaylists()
                Log.d("PlaylistViewModel", "Added song $songId to playlist $playlistId")
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Failed to add song to playlist", e)
                _error.value = e.message ?: "Failed to add song to playlist"
            }
        }
    }

    fun getUserPlaylistsForSong(): List<Playlist> {
        return _playlists.value
    }

    fun clearError() {
        _error.value = null
    }

    fun refreshPlaylists() {
        loadPlaylists()
    }
}