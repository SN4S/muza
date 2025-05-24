package com.sn4s.muza.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Album
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            repository.getSongs()
                .catch { /* Handle error */ }
                .collect { songs ->
                    _songs.value = songs
                }
        }

        viewModelScope.launch {
            repository.getUserAlbums()
                .catch { /* Handle error */ }
                .collect { albums ->
                    _albums.value = albums
                }
        }
    }
} 