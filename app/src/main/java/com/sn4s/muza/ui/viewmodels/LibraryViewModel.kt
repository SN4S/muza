package com.sn4s.muza.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Album
import com.sn4s.muza.data.model.Playlist
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.data.model.UserProfile
import com.sn4s.muza.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    private val _likedAlbums = MutableStateFlow<List<Album>>(emptyList())
    val likedAlbums: StateFlow<List<Album>> = _likedAlbums

    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs: StateFlow<List<Song>> = _likedSongs

    // Add loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _followedArtists = MutableStateFlow<List<UserProfile>>(emptyList())
    val followedArtists: StateFlow<List<UserProfile>> = _followedArtists

    init {
        loadContent()
    }

    private fun loadContent() {
        loadPlaylists()
        loadAlbums()
        loadLikedSongs()
        loadFollowedArtists()
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            try {
                repository.getUserPlaylists()
                    .catch { e ->
                        Log.e("LibraryViewModel", "Failed to load playlists", e)
                    }
                    .collect { playlists ->
                        _playlists.value = playlists
                        Log.d("LibraryViewModel", "Loaded ${playlists.size} playlists")
                    }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error loading playlists", e)
            }
        }
    }

    private fun loadFollowedArtists() {
        viewModelScope.launch {
            try {
                repository.getMyFollowing()
                    .catch { e ->
                        Log.e("LibraryViewModel", "Failed to load followed artists", e)
                    }
                    .collect { artists ->
                        _followedArtists.value = artists
                        Log.d("LibraryViewModel", "Loaded ${artists.size} followed artists")
                    }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error loading followed artists", e)
            }
        }
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            try {
                repository.getLikedAlbums()
                    .catch { e ->
                        Log.e("LibraryViewModel", "Failed to load albums", e)
                    }
                    .collect { albums ->
                        _likedAlbums.value = albums
                        Log.d("LibraryViewModel", "Loaded ${albums.size} albums")
                    }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error loading albums", e)
            }
        }
    }

    fun loadLikedSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.getLikedSongs()
                    .catch { e ->
                        Log.e("LibraryViewModel", "Failed to load liked songs", e)
                        _error.value = e.message ?: "Failed to load liked songs"
                    }
                    .collect { songs ->
                        _likedSongs.value = songs
                        Log.d("LibraryViewModel", "Loaded ${songs.size} liked songs")
                    }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error loading liked songs", e)
                _error.value = e.message ?: "Failed to load liked songs"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshContent() {
        loadContent()
    }

    fun refreshLikedSongs() {
        loadLikedSongs()
    }

    fun clearError() {
        _error.value = null
    }
}