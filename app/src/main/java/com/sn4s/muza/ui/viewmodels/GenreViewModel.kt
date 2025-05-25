package com.sn4s.muza.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Genre
import com.sn4s.muza.data.model.GenreCreate
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenreViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres

    private val _genreSongs = MutableStateFlow<List<Song>>(emptyList())
    val genreSongs: StateFlow<List<Song>> = _genreSongs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadGenres()
    }

    private fun loadGenres() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getGenres()
                    .catch { e ->
                        Log.e("GenreViewModel", "Failed to load genres", e)
                        _error.value = e.message ?: "Failed to load genres"
                    }
                    .collect { genres ->
                        _genres.value = genres
                    }
            } catch (e: Exception) {
                Log.e("GenreViewModel", "Error loading genres", e)
                _error.value = e.message ?: "Error loading genres"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadGenreSongs(genreId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getGenreSongs(genreId)
                    .catch { e ->
                        Log.e("GenreViewModel", "Failed to load genre songs", e)
                        _error.value = e.message ?: "Failed to load songs"
                    }
                    .collect { songs ->
                        _genreSongs.value = songs
                    }
            } catch (e: Exception) {
                Log.e("GenreViewModel", "Error loading genre songs", e)
                _error.value = e.message ?: "Error loading songs"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createGenre(name: String, description: String? = null) {
        if (name.isBlank()) {
            _error.value = "Genre name cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val genreCreate = GenreCreate(name = name, description = description)
                repository.createGenre(genreCreate)
                loadGenres() // Refresh genres list
                Log.d("GenreViewModel", "Genre created successfully: $name")
            } catch (e: Exception) {
                Log.e("GenreViewModel", "Failed to create genre", e)
                _error.value = e.message ?: "Failed to create genre"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateGenre(genreId: Int, name: String, description: String? = null) {
        if (name.isBlank()) {
            _error.value = "Genre name cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val genreCreate = GenreCreate(name = name, description = description)
                repository.updateGenre(genreId, genreCreate)
                loadGenres() // Refresh genres list
                Log.d("GenreViewModel", "Genre updated successfully: $name")
            } catch (e: Exception) {
                Log.e("GenreViewModel", "Failed to update genre", e)
                _error.value = e.message ?: "Failed to update genre"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteGenre(genreId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteGenre(genreId)
                loadGenres() // Refresh genres list
                Log.d("GenreViewModel", "Genre deleted successfully")
            } catch (e: Exception) {
                Log.e("GenreViewModel", "Failed to delete genre", e)
                _error.value = e.message ?: "Failed to delete genre"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun refreshGenres() {
        loadGenres()
    }
}