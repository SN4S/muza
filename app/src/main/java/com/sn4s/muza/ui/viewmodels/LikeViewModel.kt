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

    // Keep track of checked songs to avoid duplicate API calls
    private val _checkedSongs = mutableSetOf<Int>()

    init {
        // Load initial liked songs
        loadLikedSongs()
    }

    private fun loadLikedSongs() {
        viewModelScope.launch {
            try {
                repository.getLikedSongs()
                    .catch { e ->
                        Log.e("LikeViewModel", "Failed to load liked songs", e)
                    }
                    .collect { songs ->
                        val likedIds = songs.map { it.id }.toSet()
                        _likedSongs.value = likedIds
                        _checkedSongs.addAll(likedIds)
                        Log.d("LikeViewModel", "Loaded ${likedIds.size} liked songs")
                    }
            } catch (e: Exception) {
                Log.e("LikeViewModel", "Error loading liked songs", e)
            }
        }
    }

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

                _checkedSongs.add(songId)
            } catch (e: Exception) {
                Log.e("LikeViewModel", "Failed to toggle like for song: $songId", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkIfLiked(songId: Int) {
        // Don't check if we already know the status
        if (_checkedSongs.contains(songId)) {
            return
        }

        viewModelScope.launch {
            try {
                val isLiked = repository.isSongLiked(songId)
                if (isLiked) {
                    _likedSongs.value = _likedSongs.value + songId
                } else {
                    _likedSongs.value = _likedSongs.value - songId
                }
                _checkedSongs.add(songId)
                Log.d("LikeViewModel", "Checked like status for song $songId: $isLiked")
            } catch (e: Exception) {
                Log.e("LikeViewModel", "Failed to check like status for song: $songId", e)
            }
        }
    }

    fun checkMultipleLikes(songIds: List<Int>) {
        val uncheckedIds = songIds.filter { !_checkedSongs.contains(it) }
        if (uncheckedIds.isEmpty()) return

        viewModelScope.launch {
            try {
                // Call new bulk endpoint (you'll need to add this to backend)
                val likeStatuses = repository.checkMultipleLikes(uncheckedIds)
                val newLikedIds = likeStatuses.filter { it.value }.keys
                _likedSongs.value = _likedSongs.value + newLikedIds
                _checkedSongs.addAll(uncheckedIds)
            } catch (e: Exception) {
                Log.e("LikeViewModel", "Failed to check multiple likes", e)
            }
        }
    }

    fun isLiked(songId: Int): Boolean {
        return _likedSongs.value.contains(songId)
    }

    fun refreshLikedSongs() {
        _checkedSongs.clear()
        loadLikedSongs()
    }
}