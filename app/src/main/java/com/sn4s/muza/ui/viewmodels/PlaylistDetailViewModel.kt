package com.sn4s.muza.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Playlist
import com.sn4s.muza.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _playlistDeleted = MutableStateFlow(false)
    val playlistDeleted: StateFlow<Boolean> = _playlistDeleted

    fun loadPlaylist(playlistId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val playlist = repository.getPlaylist(playlistId)
                _playlist.value = playlist
                Log.d("PlaylistDetailViewModel", "Loaded playlist: ${playlist.name} with ${playlist.songs.size} songs")
            } catch (e: Exception) {
                Log.e("PlaylistDetailViewModel", "Failed to load playlist", e)
                _error.value = e.message ?: "Failed to load playlist"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: Int, songId: Int) {
        viewModelScope.launch {
            try {
                repository.removeSongFromPlaylist(playlistId, songId)
                // Reload playlist to update UI
                loadPlaylist(playlistId)
                Log.d("PlaylistDetailViewModel", "Removed song $songId from playlist $playlistId")
            } catch (e: Exception) {
                Log.e("PlaylistDetailViewModel", "Failed to remove song from playlist", e)
                _error.value = e.message ?: "Failed to remove song"
            }
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deletePlaylist(playlistId)
                _playlistDeleted.value = true
                Log.d("PlaylistDetailViewModel", "Deleted playlist $playlistId")
            } catch (e: Exception) {
                Log.e("PlaylistDetailViewModel", "Failed to delete playlist", e)
                _error.value = e.message ?: "Failed to delete playlist"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}