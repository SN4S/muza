package com.sn4s.muza.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.player.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val playbackState: StateFlow<Int> = playerManager.playbackState
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val duration: StateFlow<Long> = playerManager.duration
    val playlist: StateFlow<List<Song>> = playerManager.playlist
    val currentIndex: StateFlow<Int> = playerManager.currentIndex

    fun playSong(song: Song) {
        playerManager.playSong(song)
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        playerManager.playPlaylist(songs, startIndex)
    }

    fun play() {
        playerManager.play()
    }

    fun pause() {
        playerManager.pause()
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(position: Long) {
        playerManager.seekTo(position)
    }

    fun skipToNext() {
        playerManager.skipToNext()
    }

    fun skipToPrevious() {
        playerManager.skipToPrevious()
    }

    fun seekToIndex(index: Int) {
        playerManager.seekToIndex(index)
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}