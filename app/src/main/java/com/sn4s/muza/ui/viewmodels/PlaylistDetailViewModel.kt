package com.sn4s.muza.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Playlist
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.data.model.SongNested
import com.sn4s.muza.data.model.UserNested
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

    // Helper function to convert SongNested to Song
    // You might need to adjust this based on your actual models
    fun getSongFromNested(songNested: SongNested): Song? {
        return try {
            // This is a simplified conversion - you might need to fetch more data
            // or adjust based on your actual model structure
            Song(
                id = songNested.id,
                title = songNested.title,
                duration = songNested.duration,
                filePath = songNested.filePath,
                albumId = null, // SongNested doesn't have albumId
                creatorId = 0, // You'll need to get this somehow
                createdAt = songNested.createdAt,
                creator = songNested.creator,
                likeCount = songNested.likeCount
            )
        } catch (e: Exception) {
            Log.e("PlaylistDetailViewModel", "Failed to convert SongNested to Song", e)
            null
        }
    }

    fun clearError() {
        _error.value = null
    }
}