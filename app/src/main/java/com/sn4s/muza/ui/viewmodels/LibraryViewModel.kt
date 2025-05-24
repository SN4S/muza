package com.sn4s.muza.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Album
import com.sn4s.muza.data.model.Playlist
import com.sn4s.muza.data.model.Song
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

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums

    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs: StateFlow<List<Song>> = _likedSongs

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            repository.getUserPlaylists()
                .catch { /* Handle error */ }
                .collect { playlists ->
                    _playlists.value = playlists
                }
        }

        viewModelScope.launch {
            repository.getUserAlbums()
                .catch { /* Handle error */ }
                .collect { albums ->
                    _albums.value = albums
                }
        }

        viewModelScope.launch {
            repository.getLikedSongs()
                .catch { /* Handle error */ }
                .collect { songs ->
                    _likedSongs.value = songs
                }
        }
    }
} 