package com.sn4s.muza.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Album
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.data.model.UserNested
import com.sn4s.muza.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistProfileViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _artist = MutableStateFlow<UserNested?>(null)
    val artist: StateFlow<UserNested?> = _artist

    private val _artistSongs = MutableStateFlow<List<Song>>(emptyList())
    val artistSongs: StateFlow<List<Song>> = _artistSongs

    private val _artistAlbums = MutableStateFlow<List<Album>>(emptyList())
    val artistAlbums: StateFlow<List<Album>> = _artistAlbums

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadArtist(artistId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Load artist profile
                val artist = repository.getUser(artistId)
                _artist.value = artist

                if (artist.isArtist) {
                    // Load artist's songs
                    repository.getPublicUserSongs(artistId)
                        .catch { e ->
                            Log.e("ArtistProfileViewModel", "Failed to load artist songs", e)
                        }
                        .collect { songs ->
                            _artistSongs.value = songs
                        }

                    // Load artist's albums
                    repository.getPublicUserAlbums(artistId)
                        .catch { e ->
                            Log.e("ArtistProfileViewModel", "Failed to load artist albums", e)
                        }
                        .collect { albums ->
                            _artistAlbums.value = albums
                        }
                }

                Log.d("ArtistProfileViewModel", "Loaded artist: ${artist.username}")
            } catch (e: Exception) {
                Log.e("ArtistProfileViewModel", "Failed to load artist", e)
                _error.value = e.message ?: "Failed to load artist profile"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}