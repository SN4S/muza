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
class AlbumLikeViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _likedAlbums = MutableStateFlow<Set<Int>>(emptySet())
    val likedAlbums: StateFlow<Set<Int>> = _likedAlbums

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _checkedAlbums = mutableSetOf<Int>()

    init {
        loadLikedAlbums()
    }

    private fun loadLikedAlbums() {
        viewModelScope.launch {
            try {
                repository.getLikedAlbums()
                    .catch { e -> Log.e("AlbumLikeViewModel", "Failed to load liked albums", e) }
                    .collect { albums ->
                        val likedIds = albums.map { it.id }.toSet()
                        _likedAlbums.value = likedIds
                        _checkedAlbums.addAll(likedIds)
                    }
            } catch (e: Exception) {
                Log.e("AlbumLikeViewModel", "Error loading liked albums", e)
            }
        }
    }

    fun toggleLike(albumId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val isCurrentlyLiked = _likedAlbums.value.contains(albumId)

                if (isCurrentlyLiked) {
                    repository.unlikeAlbum(albumId)
                    _likedAlbums.value = _likedAlbums.value - albumId
                } else {
                    repository.likeAlbum(albumId)
                    _likedAlbums.value = _likedAlbums.value + albumId
                }

                _checkedAlbums.add(albumId)
            } catch (e: Exception) {
                Log.e("AlbumLikeViewModel", "Failed to toggle like for album: $albumId", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkIfLiked(songId: Int) {
        // Don't check if we already know the status
        if (_checkedAlbums.contains(songId)) {
            return
        }

        viewModelScope.launch {
            try {
                val isLiked = repository.isSongLiked(songId)
                if (isLiked) {
                    _likedAlbums.value = _likedAlbums.value + songId
                } else {
                    _likedAlbums.value = _likedAlbums.value - songId
                }
                _checkedAlbums.add(songId)
                Log.d("LikeViewModel", "Checked like status for song $songId: $isLiked")
            } catch (e: Exception) {
                Log.e("LikeViewModel", "Failed to check like status for song: $songId", e)
            }
        }
    }

    fun checkMultipleLikes(AlbumsIds: List<Int>) {
        val uncheckedIds = AlbumsIds.filter { !_checkedAlbums.contains(it) }
        if (uncheckedIds.isEmpty()) return

        viewModelScope.launch {
            try {
                // Call new bulk endpoint (you'll need to add this to backend)
                val likeStatuses = repository.checkMultipleLikes(uncheckedIds)
                val newLikedIds = likeStatuses.filter { it.value }.keys
                _likedAlbums.value = _likedAlbums.value + newLikedIds
                _checkedAlbums.addAll(uncheckedIds)
            } catch (e: Exception) {
                Log.e("LikeViewModel", "Failed to check multiple likes", e)
            }
        }
    }

    fun refreshLikedAlbums() {
        _checkedAlbums.clear()
        loadLikedAlbums()
    }

    fun isLiked(albumId: Int): Boolean = _likedAlbums.value.contains(albumId)
}