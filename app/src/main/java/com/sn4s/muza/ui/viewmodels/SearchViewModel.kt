package com.sn4s.muza.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Album
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.data.model.User
import com.sn4s.muza.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _artists = MutableStateFlow<List<User>>(emptyList())
    val artists: StateFlow<List<User>> = _artists

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .filter { it.length >= 2 }
                .collect { query ->
                    search(query)
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun search(query: String) {
        viewModelScope.launch {
            repository.searchSongs(query)
                .catch { /* Handle error */ }
                .collect { songs ->
                    _songs.value = songs
                }
        }

        viewModelScope.launch {
            repository.searchArtists(query)
                .catch { /* Handle error */ }
                .collect { artists ->
                    _artists.value = artists
                }
        }

        viewModelScope.launch {
            repository.searchAlbums(query)
                .catch { /* Handle error */ }
                .collect { albums ->
                    _albums.value = albums
                }
        }
    }
} 