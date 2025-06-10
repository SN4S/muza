package com.sn4s.muza.ui.viewmodels

import android.util.Log
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

    // Add missing loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .filter { it.length >= 3 }
                .collect { query ->
                    search(query)
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.length < 3) {
            // Clear results if query is too short
            _songs.value = emptyList()
            _artists.value = emptyList()
            _albums.value = emptyList()
            _error.value = null
        }
    }

    fun search() {
        if (_searchQuery.value.length >= 3) {
            search(_searchQuery.value)
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _songs.value = emptyList()
        _artists.value = emptyList()
        _albums.value = emptyList()
        _error.value = null
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Search songs
                repository.searchSongs(query)
                    .catch { e ->
                        Log.e("SearchViewModel", "Failed to search songs", e)
                        _error.value = "Failed to search songs: ${e.message}"
                    }
                    .collect { songs ->
                        _songs.value = songs
                        Log.d("SearchViewModel", "Found ${songs.size} songs for '$query'")
                    }

                // Search artists
                repository.searchArtists(query)
                    .catch { e ->
                        Log.e("SearchViewModel", "Failed to search artists", e)
                        _error.value = "Failed to search artists: ${e.message}"
                    }
                    .collect { artists ->
                        _artists.value = artists
                        Log.d("SearchViewModel", "Found ${artists.size} artists for '$query'")
                    }

                // Search albums
                repository.searchAlbums(query)
                    .catch { e ->
                        Log.e("SearchViewModel", "Failed to search albums", e)
                        _error.value = "Failed to search albums: ${e.message}"
                    }
                    .collect { albums ->
                        _albums.value = albums
                        Log.d("SearchViewModel", "Found ${albums.size} albums for '$query'")
                    }

            } catch (e: Exception) {
                Log.e("SearchViewModel", "Search failed", e)
                _error.value = e.message ?: "Search failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}