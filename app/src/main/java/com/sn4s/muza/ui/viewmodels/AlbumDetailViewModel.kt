package com.sn4s.muza.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Album
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _album = MutableStateFlow<Album?>(null)
    val album: StateFlow<Album?> = _album

    private val _albumSongs = MutableStateFlow<List<Song>>(emptyList())
    val albumSongs: StateFlow<List<Song>> = _albumSongs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadAlbum(albumId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Load album details
                val album = repository.getAlbum(albumId)
                _album.value = album

                // Load album songs separately for better control
                repository.getAlbumSongs(albumId)
                    .catch { e ->
                        Log.e("AlbumDetailViewModel", "Failed to load album songs", e)
                        _error.value = "Failed to load songs: ${e.message}"
                    }
                    .collect { songs ->
                        _albumSongs.value = songs
                    }

                Log.d("AlbumDetailViewModel", "Loaded album: ${album.title} with ${album.songs.size} songs")
            } catch (e: Exception) {
                Log.e("AlbumDetailViewModel", "Failed to load album", e)
                _error.value = e.message ?: "Failed to load album"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}