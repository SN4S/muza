package com.sn4s.muza.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LikeViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _likedSongs = MutableStateFlow<Set<Int>>(emptySet())
    val likedSongs: StateFlow<Set<Int>> = _likedSongs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun toggleLike(songId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val isCurrentlyLiked = _likedSongs.value.contains(songId)

                if (isCurrentlyLiked) {
                    repository.unlikeSong(songId)
                    _likedSongs.value = _likedSongs.value - songId
                    Log.d("LikeViewModel", "Unliked song: $songId")
                } else {
                    repository.likeSong(songId)
                    _likedSongs.value = _likedSongs.value + songId
                    Log.d("LikeViewModel", "Liked song: $songId")
                }
            } catch (e: Exception) {
                Log.e("LikeViewModel", "Failed to toggle like for song: $songId", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkIfLiked(songId: Int) {
        viewModelScope.launch {
            try {
                val isLiked = repository.isSongLiked(songId)
                if (isLiked) {
                    _likedSongs.value = _likedSongs.value + songId
                } else {
                    _likedSongs.value = _likedSongs.value - songId
                }
            } catch (e: Exception) {
                Log.e("LikeViewModel", "Failed to check like status for song: $songId", e)
            }
        }
    }

    fun isLiked(songId: Int): Boolean {
        return _likedSongs.value.contains(songId)
    }
}